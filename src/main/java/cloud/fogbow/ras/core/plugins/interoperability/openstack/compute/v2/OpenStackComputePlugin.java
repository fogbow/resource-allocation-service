package cloud.fogbow.ras.core.plugins.interoperability.openstack.compute.v2;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpToFogbowExceptionMapper;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.HardwareRequirements;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.core.plugins.interoperability.ComputePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.network.v2.OpenStackNetworkPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.util.DefaultLaunchCommandGenerator;
import cloud.fogbow.ras.core.plugins.interoperability.util.LaunchCommandGenerator;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;

import java.util.*;

public class OpenStackComputePlugin implements ComputePlugin<OpenStackV3User> {
    private static final Logger LOGGER = Logger.getLogger(OpenStackComputePlugin.class);

    public static final String DEFAULT_NETWORK_ID_KEY = "default_network_id";
    public static final String COMPUTE_NOVAV2_URL_KEY = "openstack_nova_v2_url";
    public static final String COMPUTE_V2_API_ENDPOINT = "/v2/";
    public static final String SERVERS = "/servers";
    public static final String ACTION = "action";
    protected static final String SUFFIX_ENDPOINT_KEYPAIRS = "/os-keypairs";
    protected static final String SUFFIX_ENDPOINT_FLAVORS = "/flavors";
    protected static final String SUFFIX_FLAVOR_EXTRA_SPECS = "/os-extra_specs";

    private TreeSet<HardwareRequirements> hardwareRequirementsList;
    private Properties properties;
    private OpenStackHttpClient client;
    private LaunchCommandGenerator launchCommandGenerator;

    public OpenStackComputePlugin(String confFilePath) throws FatalErrorException {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        this.launchCommandGenerator = new DefaultLaunchCommandGenerator();
        instantiateOtherAttributes();
    }

    /**
     * Constructor used for testing only
     */
    protected OpenStackComputePlugin(Properties properties, LaunchCommandGenerator launchCommandGenerator,
                                     OpenStackHttpClient client) {
        this.properties = properties;
        this.launchCommandGenerator = launchCommandGenerator;
        this.client = client;
        this.hardwareRequirementsList = new TreeSet<HardwareRequirements>();
    }

    @Override
    public boolean isReady(String cloudState) {
        return OpenStackStateMapper.map(ResourceType.COMPUTE, cloudState).equals(InstanceState.READY);
    }

    @Override
    public boolean hasFailed(String cloudState) {
        return OpenStackStateMapper.map(ResourceType.COMPUTE, cloudState).equals(InstanceState.FAILED);
    }

    @Override
    public String requestInstance(ComputeOrder computeOrder, OpenStackV3User cloudUser) throws FogbowException {
        HardwareRequirements hardwareRequirements = findSmallestFlavor(computeOrder, cloudUser);
        String flavorId = hardwareRequirements.getFlavorId();
        String projectId = getProjectId(cloudUser);
        // Even if one or more private networks are informed in networkIds, we still need to include the default
        // network, since this is the only network that has external set to true, and a floating IP can only
        // be attached to such type of network.
        // We add the default network before any other network, because the order is very important to Openstack
        // request. Openstack will configure the routes to the external network using the first network found on
        // the request body.
        String defaultNetworkId = this.properties.getProperty(DEFAULT_NETWORK_ID_KEY);
        List<String> networkIds = new ArrayList<>();
        networkIds.add(defaultNetworkId);
        List<String> userDefinedNetworks = computeOrder.getNetworkIds();
        if (!userDefinedNetworks.isEmpty()) {
            networkIds.addAll(userDefinedNetworks);
        }
        String imageId = computeOrder.getImageId();
        String userData = this.launchCommandGenerator.createLaunchCommand(computeOrder);
        String keyName = getKeyName(projectId, cloudUser, computeOrder.getPublicKey());
        String endpoint = getComputeEndpoint(projectId, SERVERS);
        String instanceId = null;
        String instanceName = computeOrder.getName();

        try {
            instanceId = doRequestInstance(cloudUser, flavorId, networkIds, imageId, instanceName, userData,
                    keyName, endpoint);

            synchronized (computeOrder) {
                ComputeAllocation actualAllocation = new ComputeAllocation(
                        hardwareRequirements.getCpu(),
                        hardwareRequirements.getMemory(),
                        1);
                // When the ComputeOrder is remote, this field must be copied into its local counterpart
                // that is updated when the requestingProvider receives the reply from the providingProvider
                // (see RemoteFacade.java)
                computeOrder.setActualAllocation(actualAllocation);
            }
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        } finally {
            if (keyName != null) {
                deleteKeyName(projectId, cloudUser, keyName);
            }
        }
        return instanceId;
    }

    private String doRequestInstance(OpenStackV3User cloudUser, String flavorId, List<String> networkIds,
                                     String imageId, String instanceName, String userData, String keyName, String endpoint)
            throws FogbowException, HttpResponseException {
        CreateComputeRequest createBody = getRequestBody(instanceName, imageId, flavorId, userData, keyName, networkIds);

        String body = createBody.toJson();
        String response = this.client.doPostRequest(endpoint, body, cloudUser);
        CreateComputeResponse createComputeResponse = CreateComputeResponse.fromJson(response);

        return createComputeResponse.getId();
    }

    @Override
    public ComputeInstance getInstance(ComputeOrder computeOrder, OpenStackV3User cloudUser) throws FogbowException {
        String projectId = getProjectId(cloudUser);
        String requestEndpoint = getComputeEndpoint(projectId, SERVERS + "/" + computeOrder.getInstanceId());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(requestEndpoint, cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }

        ComputeInstance computeInstance = getInstanceFromJson(jsonResponse, cloudUser);
        // The default network is always included in the order by the OpenStack plugin, thus it should be added
        // in the map of networks in the ComputeInstance by the plugin. The remaining networks passed by the user
        // are appended by the LocalCloudConnector.
        String defaultNetworkId = this.properties.getProperty(DEFAULT_NETWORK_ID_KEY);
        Map<String, String> computeNetworks = new HashMap<>();
        computeNetworks.put(defaultNetworkId, SystemConstants.DEFAULT_NETWORK_NAME);
        computeInstance.setNetworks(computeNetworks);
        return computeInstance;
    }

    @Override
    public void deleteInstance(ComputeOrder computeOrder, OpenStackV3User cloudUser) throws FogbowException {
        String endpoint = getComputeEndpoint(getProjectId(cloudUser), SERVERS + "/" + computeOrder.getInstanceId());
        try {
            this.client.doDeleteRequest(endpoint, cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
    }

    private void instantiateOtherAttributes() {
        this.hardwareRequirementsList = new TreeSet<HardwareRequirements>();
        this.initClient();
    }

    private void initClient() {
        this.client = new OpenStackHttpClient();
    }

    private String getProjectId(OpenStackV3User cloudUser) throws InvalidParameterException {
        String projectId = cloudUser.getProjectId();
        if (projectId == null) {
            throw new InvalidParameterException(Messages.Exception.NO_PROJECT_ID);
        }
        return projectId;
    }

    private String getKeyName(String projectId, OpenStackV3User cloudUser, String publicKey) throws FogbowException {
        String keyName = null;

        if (publicKey != null && !publicKey.isEmpty()) {
            String osKeypairEndpoint = getComputeEndpoint(projectId, SUFFIX_ENDPOINT_KEYPAIRS);

            keyName = getRandomUUID();
            CreateOsKeypairRequest request = new CreateOsKeypairRequest.Builder()
                    .name(keyName)
                    .publicKey(publicKey)
                    .build();

            String body = request.toJson();
            try {
                this.client.doPostRequest(osKeypairEndpoint, body, cloudUser);
            } catch (HttpResponseException e) {
                OpenStackHttpToFogbowExceptionMapper.map(e);
            }
        }

        return keyName;
    }

    private String getComputeEndpoint(String projectId, String suffix) {
        return this.properties.getProperty(COMPUTE_NOVAV2_URL_KEY) + COMPUTE_V2_API_ENDPOINT + projectId + suffix;
    }

    private void deleteKeyName(String projectId, OpenStackV3User cloudUser, String keyName) throws FogbowException {
        String suffixEndpoint = SUFFIX_ENDPOINT_KEYPAIRS + "/" + keyName;
        String keyNameEndpoint = getComputeEndpoint(projectId, suffixEndpoint);

        try {
            this.client.doDeleteRequest(keyNameEndpoint, cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
    }

    private CreateComputeRequest getRequestBody(String instanceName, String imageRef, String flavorRef, String userdata,
                                                String keyName, List<String> networksIds) {
        List<CreateComputeRequest.Network> networks = new ArrayList<>();
        List<CreateComputeRequest.SecurityGroup> securityGroups = new ArrayList<>();
        for (String networkId : networksIds) {
            networks.add(new CreateComputeRequest.Network(networkId));

            String defaultNetworkId = this.properties.getProperty(DEFAULT_NETWORK_ID_KEY);
            if (!networkId.equals(defaultNetworkId)) {
                String securityGroupName = OpenStackNetworkPlugin.getSGNameForPrivateNetwork(networkId);
                securityGroups.add(new CreateComputeRequest.SecurityGroup(securityGroupName));
            }
        }

        // do not specify security groups if no additional network was given
        securityGroups = securityGroups.size() == 0 ? null : securityGroups;

        String name = instanceName == null ? SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + getRandomUUID() : instanceName;
        CreateComputeRequest createComputeRequest = new CreateComputeRequest.Builder()
                .name(name)
                .imageReference(imageRef)
                .flavorReference(flavorRef)
                .userData(userdata)
                .keyName(keyName)
                .networks(networks)
                .securityGroups(securityGroups)
                .build();

        return createComputeRequest;
    }

    private HardwareRequirements findSmallestFlavor(ComputeOrder computeOrder, OpenStackV3User cloudUser)
            throws FogbowException {
        HardwareRequirements bestFlavor = getBestFlavor(computeOrder, cloudUser);
        if (bestFlavor == null) {
            throw new NoAvailableResourcesException();
        }
        return bestFlavor;
    }

    private HardwareRequirements getBestFlavor(ComputeOrder computeOrder, OpenStackV3User cloudUser)
            throws FogbowException {
        updateFlavors(cloudUser, computeOrder);
        TreeSet<HardwareRequirements> hardwareRequirementsList = getHardwareRequirementsList();
        for (HardwareRequirements hardwareRequirements : hardwareRequirementsList) {
            if (hardwareRequirements.getCpu() >= computeOrder.getvCPU()
                    && hardwareRequirements.getMemory() >= computeOrder.getMemory()
                    && hardwareRequirements.getDisk() >= computeOrder.getDisk()) {
                return hardwareRequirements;
            }
        }
        return null;
    }

    private void updateFlavors(OpenStackV3User cloudUser, ComputeOrder computeOrder) throws FogbowException {
        String projectId = getProjectId(cloudUser);
        String flavorsEndpoint = getComputeEndpoint(projectId, SUFFIX_ENDPOINT_FLAVORS);

        try {
            String jsonResponse = this.client.doGetRequest(flavorsEndpoint, cloudUser);
            GetAllFlavorsResponse getAllFlavorsResponse = GetAllFlavorsResponse.fromJson(jsonResponse);

            List<String> flavorsIds = new ArrayList<>();
            for (GetAllFlavorsResponse.Flavor flavor : getAllFlavorsResponse.getFlavors()) {
                if (computeOrder != null && computeOrder.getRequirements() != null && computeOrder.getRequirements().size() > 0) {
                    if (!flavorHasRequirements(cloudUser, computeOrder.getRequirements(), flavor.getId())) {
                        continue;
                    }
                }
                flavorsIds.add(flavor.getId());
            }

            TreeSet<HardwareRequirements> newHardwareRequirements =
                    detailFlavors(flavorsEndpoint, cloudUser, flavorsIds);
            setHardwareRequirementsList(newHardwareRequirements);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
    }

    private boolean flavorHasRequirements(OpenStackV3User cloudUser, Map<String, String> requirements,
                                          String flavorId) throws FogbowException {
        String projectId = getProjectId(cloudUser);
        String specsEndpoint = getComputeEndpoint(projectId, SUFFIX_ENDPOINT_FLAVORS)  + "/" + flavorId + SUFFIX_FLAVOR_EXTRA_SPECS;

        try {
            String jsonResponse = this.client.doGetRequest(specsEndpoint, cloudUser);
            GetFlavorExtraSpecsResponse getFlavorExtraSpecsResponse = GetFlavorExtraSpecsResponse.fromJson(jsonResponse);
            Map<String, String> flavorExtraSpecs = getFlavorExtraSpecsResponse.getFlavorExtraSpecs();

            for(String tag : requirements.keySet()) {
                if (!flavorExtraSpecs.containsKey(tag) || !flavorExtraSpecs.get(tag).equals(requirements.get(tag))) {
                    return false;
                }
            }
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }

        return true;
    }

    private TreeSet<HardwareRequirements> detailFlavors(String endpoint, OpenStackV3User cloudUser,
                                                        List<String> flavorsIds)
            throws FogbowException, UnexpectedException {
        TreeSet<HardwareRequirements> newHardwareRequirements = new TreeSet<>();
        TreeSet<HardwareRequirements> flavorsCopy = new TreeSet<>(getHardwareRequirementsList());

        for (String flavorId : flavorsIds) {
            boolean containsFlavorForCaching = false;

            for (HardwareRequirements flavor : flavorsCopy) {
                if (flavor.getFlavorId().equals(flavorId)) {
                    containsFlavorForCaching = true;
                    newHardwareRequirements.add(flavor);
                    break;
                }
            }

            if (!containsFlavorForCaching) {
                String newEndpoint = endpoint + "/" + flavorId;

                String getJsonResponse = null;

                try {
                    getJsonResponse = this.client.doGetRequest(newEndpoint, cloudUser);
                } catch (HttpResponseException e) {
                    OpenStackHttpToFogbowExceptionMapper.map(e);
                }

                GetFlavorResponse getFlavorResponse = GetFlavorResponse.fromJson(getJsonResponse);

                String id = getFlavorResponse.getId();
                String name = getFlavorResponse.getName();
                int disk = getFlavorResponse.getDisk();
                int memory = getFlavorResponse.getMemory();
                int vcpusCount = getFlavorResponse.getVcpusCount();

                newHardwareRequirements.add(new HardwareRequirements(name, id, vcpusCount, memory, disk));
            }
        }
        return newHardwareRequirements;
    }

    private ComputeInstance getInstanceFromJson(String getRawResponse, OpenStackV3User cloudUser)
            throws FogbowException {
        GetComputeResponse getComputeResponse = GetComputeResponse.fromJson(getRawResponse);

        String flavorId = getComputeResponse.getFlavor().getId();
        HardwareRequirements hardwareRequirements = getFlavorById(flavorId, cloudUser);

        if (hardwareRequirements == null) {
            throw new NoAvailableResourcesException(Messages.Exception.NO_MATCHING_FLAVOR);
        }

        int vcpusCount = hardwareRequirements.getCpu();
        int memory = hardwareRequirements.getMemory();
        int disk = hardwareRequirements.getDisk();

        String openStackState = getComputeResponse.getStatus();
        String instanceId = getComputeResponse.getId();
        String hostName = getComputeResponse.getName();

        Map<String, GetComputeResponse.Address[]> addressesContainer = getComputeResponse.getAddresses();
        List<String> ipAddresses = new ArrayList<>();

        if (addressesContainer != null) {
            for (GetComputeResponse.Address[] addresses : addressesContainer.values()) {
                for (GetComputeResponse.Address address : addresses) {
                    ipAddresses.add(address.getAddress());
                }
            }
        }

        ComputeInstance computeInstance = new ComputeInstance(instanceId,
                openStackState, hostName, vcpusCount, memory, disk, ipAddresses);

        return computeInstance;
    }

    private HardwareRequirements getFlavorById(String id, OpenStackV3User cloudUser) throws FogbowException {
        updateFlavors(cloudUser, null);
        TreeSet<HardwareRequirements> flavorsCopy = new TreeSet<>(getHardwareRequirementsList());
        for (HardwareRequirements hardwareRequirements : flavorsCopy) {
            if (hardwareRequirements.getFlavorId().equals(id)) {
                return hardwareRequirements;
            }
        }
        return null;
    }

    protected String getRandomUUID() {
        return UUID.randomUUID().toString();
    }

    private TreeSet<HardwareRequirements> getHardwareRequirementsList() {
        synchronized (this.hardwareRequirementsList) {
            return new TreeSet<HardwareRequirements>(this.hardwareRequirementsList);
        }
    }

    private void setHardwareRequirementsList(TreeSet<HardwareRequirements> hardwareRequirementsList) {
        synchronized (this.hardwareRequirementsList) {
            this.hardwareRequirementsList = hardwareRequirementsList;
        }
    }
}
