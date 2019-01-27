package cloud.fogbow.ras.core.plugins.interoperability.openstack.compute.v2;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.core.constants.ConfigurationConstants;
import cloud.fogbow.ras.core.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.network.v2.OpenStackNetworkPlugin;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.HardwareRequirements;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.instances.ComputeInstance;
import cloud.fogbow.ras.core.models.instances.InstanceState;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.core.plugins.interoperability.ComputePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackHttpToFogbowExceptionMapper;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackV3Token;
import cloud.fogbow.ras.core.plugins.interoperability.util.DefaultLaunchCommandGenerator;
import cloud.fogbow.ras.core.plugins.interoperability.util.LaunchCommandGenerator;
import cloud.fogbow.ras.util.connectivity.AuditableHttpRequestClient;

import java.util.*;

public class OpenStackComputePlugin implements ComputePlugin {
    private static final Logger LOGGER = Logger.getLogger(OpenStackComputePlugin.class);

    public static final String DEFAULT_NETWORK_ID_KEY = "default_network_id";
    public static final String COMPUTE_NOVAV2_URL_KEY = "openstack_nova_v2_url";
    public static final String COMPUTE_V2_API_ENDPOINT = "/v2/";
    public static final String SERVERS = "/servers";
    public static final String ACTION = "action";

    protected static final String ID_JSON_FIELD = "id";
    protected static final String NAME_JSON_FIELD = "name";
    protected static final String SERVER_JSON_FIELD = "server";
    protected static final String FLAVOR_REF_JSON_FIELD = "flavorRef";
    protected static final String FLAVOR_JSON_FIELD = "flavor";
    protected static final String FLAVOR_EXTRA_SPECS_JSON_FIELD = "extra_specs";
    protected static final String FLAVOR_ID_JSON_FIELD = "id";
    protected static final String IMAGE_JSON_FIELD = "imageRef";
    protected static final String USER_DATA_JSON_FIELD = "user_data";
    protected static final String NETWORK_JSON_FIELD = "networks";
    protected static final String STATUS_JSON_FIELD = "status";
    protected static final String DISK_JSON_FIELD = "disk";
    protected static final String VCPU_JSON_FIELD = "vcpus";
    protected static final String MEMORY_JSON_FIELD = "ram";
    protected static final String SECURITY_JSON_FIELD = "security_groups";
    protected static final String FLAVOR_JSON_OBJECT = "flavor";
    protected static final String FLAVOR_JSON_KEY = "flavors";
    protected static final String KEY_JSON_FIELD = "key_name";
    protected static final String PUBLIC_KEY_JSON_FIELD = "public_key";
    protected static final String KEYPAIR_JSON_FIELD = "keypair";
    protected static final String UUID_JSON_FIELD = "uuid";
    protected static final String FOGBOW_INSTANCE_NAME = "ras-compute-";
    protected static final String PROJECT_ID = "projectId";
    protected static final String SUFFIX_ENDPOINT_KEYPAIRS = "/os-keypairs";
    protected static final String SUFFIX_ENDPOINT_FLAVORS = "/flavors";
    protected static final String SUFFIX_FLAVOR_EXTRA_SPECS = "/os-extra_specs";
    protected static final String ADDRESS_FIELD = "addresses";
    protected static final String PROVIDER_NETWORK_FIELD = "default";
    protected static final String ADDR_FIELD = "addr";
    private TreeSet<HardwareRequirements> hardwareRequirementsList;
    private Properties properties;
    private AuditableHttpRequestClient client;
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
                                     AuditableHttpRequestClient client) {
        this.properties = properties;
        this.launchCommandGenerator = launchCommandGenerator;
        this.client = client;
        this.hardwareRequirementsList = new TreeSet<HardwareRequirements>();
    }

    public String requestInstance(ComputeOrder computeOrder, CloudToken openStackV3Token) throws FogbowException {
        HardwareRequirements hardwareRequirements = findSmallestFlavor(computeOrder, openStackV3Token);
        String flavorId = hardwareRequirements.getFlavorId();
        String projectId = getProjectId(openStackV3Token);
        List<String> networksId = resolveNetworksId(computeOrder);
        String imageId = computeOrder.getImageId();
        String userData = this.launchCommandGenerator.createLaunchCommand(computeOrder);
        String keyName = getKeyName(projectId, openStackV3Token, computeOrder.getPublicKey());
        String endpoint = getComputeEndpoint(projectId, SERVERS);
        String instanceId = null;
        String instanceName = computeOrder.getName();

        try {
            instanceId = doRequestInstance(openStackV3Token, flavorId, networksId, imageId, instanceName, userData,
                    keyName, endpoint);

            synchronized (computeOrder) {
                ComputeAllocation actualAllocation = new ComputeAllocation(
                        hardwareRequirements.getCpu(),
                        hardwareRequirements.getMemory(),
                        1);
                // When the ComputeOrder is remote, this field must be copied into its local counterpart
                // that is updated when the requestingMember receives the reply from the providingMember
                // (see RemoteFacade.java)
                computeOrder.setActualAllocation(actualAllocation);
            }
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        } finally {
            if (keyName != null) {
                deleteKeyName(projectId, openStackV3Token, keyName);
            }
        }
        return instanceId;
    }

    private String doRequestInstance(CloudToken openStackV3Token, String flavorId, List<String> networksId,
                                     String imageId, String instanceName, String userData, String keyName, String endpoint)
            throws UnavailableProviderException, HttpResponseException {
        CreateComputeRequest createBody = getRequestBody(instanceName, imageId, flavorId, userData, keyName, networksId);

        String body = createBody.toJson();
        String response = this.client.doPostRequest(endpoint, openStackV3Token, body);
        CreateComputeResponse createComputeResponse = CreateComputeResponse.fromJson(response);

        return createComputeResponse.getId();
    }

    @Override
    public ComputeInstance getInstance(String instanceId, CloudToken openStackV3Token) throws FogbowException {
        LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE, instanceId, openStackV3Token));

        String projectId = getProjectId(openStackV3Token);
        String requestEndpoint = getComputeEndpoint(projectId, SERVERS + "/" + instanceId);

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(requestEndpoint, openStackV3Token);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }

        ComputeInstance computeInstance = getInstanceFromJson(jsonResponse, openStackV3Token);

        // Case the user has specified no private networks, than the compute instance was attached to
        // the default network. When inserting the data that come from the order in the instance, if
        // networks were set, then the default network was not inserted, and this information will be
        // overwritten.
        String defaultNetworkId = this.properties.getProperty(DEFAULT_NETWORK_ID_KEY);
        Map<String, String> computeNetworks = new HashMap<>();
        computeNetworks.put(defaultNetworkId, PROVIDER_NETWORK_FIELD);
        computeInstance.setNetworks(computeNetworks);

        return computeInstance;
    }

    @Override
    public void deleteInstance(String instanceId, CloudToken openStackV3Token) throws FogbowException {
        LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE, instanceId, openStackV3Token));
        String endpoint = getComputeEndpoint(getProjectId(openStackV3Token), SERVERS + "/" + instanceId);
        try {
            this.client.doDeleteRequest(endpoint, openStackV3Token);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
    }

    private void instantiateOtherAttributes() {
        this.hardwareRequirementsList = new TreeSet<HardwareRequirements>();
        this.initClient();
    }

    private void initClient() {
        this.client = new AuditableHttpRequestClient(new Integer(PropertiesHolder.getInstance().getProperty(ConfigurationConstants.HTTP_REQUEST_TIMEOUT_KEY)));
    }

    private String getProjectId(CloudToken token) throws InvalidParameterException {
        OpenStackV3Token openStackV3Token = (OpenStackV3Token) token;
        String projectId = openStackV3Token.getProjectId();
        if (projectId == null) {
            throw new InvalidParameterException(Messages.Exception.NO_PROJECT_ID);
        }
        return projectId;
    }

    private List<String> resolveNetworksId(ComputeOrder computeOrder) {
        String defaultNetworkId = this.properties.getProperty(DEFAULT_NETWORK_ID_KEY);

        // Even if one or more private networks are informed in networkIds, we still need to include the default
        // network, since this is the only network that has external set to true, and a floating IP can only
        // be attached to such type of network.
        // We add the default network before any other network, because the order is very important to Openstack
        // request. Openstack will configure the routes to the external network by the first network found on request
        // body.
        List<String> requestedNetworkIds = new ArrayList<>();
        requestedNetworkIds.add(defaultNetworkId);
        requestedNetworkIds.addAll(computeOrder.getNetworkIds());

        computeOrder.setNetworkIds(requestedNetworkIds);
        return requestedNetworkIds;
    }

    private String getKeyName(String projectId, CloudToken openStackV3Token, String publicKey) throws FogbowException {
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
                this.client.doPostRequest(osKeypairEndpoint, openStackV3Token, body);
            } catch (HttpResponseException e) {
                OpenStackHttpToFogbowExceptionMapper.map(e);
            }
        }

        return keyName;
    }

    private String getComputeEndpoint(String projectId, String suffix) {
        return this.properties.getProperty(COMPUTE_NOVAV2_URL_KEY) + COMPUTE_V2_API_ENDPOINT + projectId + suffix;
    }

    private void deleteKeyName(String projectId, CloudToken openStackV3Token, String keyName) throws FogbowException {
        String suffixEndpoint = SUFFIX_ENDPOINT_KEYPAIRS + "/" + keyName;
        String keyNameEndpoint = getComputeEndpoint(projectId, suffixEndpoint);

        try {
            this.client.doDeleteRequest(keyNameEndpoint, openStackV3Token);
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

        String name = instanceName == null ? FOGBOW_INSTANCE_NAME + getRandomUUID() : instanceName;
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

    private HardwareRequirements findSmallestFlavor(ComputeOrder computeOrder, CloudToken openStackV3Token)
            throws FogbowException {
        HardwareRequirements bestFlavor = getBestFlavor(computeOrder, openStackV3Token);
        if (bestFlavor == null) {
            throw new NoAvailableResourcesException();
        }
        return bestFlavor;
    }

    private HardwareRequirements getBestFlavor(ComputeOrder computeOrder, CloudToken openStackV3Token)
            throws FogbowException {
        updateFlavors(openStackV3Token, computeOrder);
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

    private void updateFlavors(CloudToken openStackV3Token, ComputeOrder computeOrder) throws FogbowException {
        String projectId = getProjectId(openStackV3Token);
        String flavorsEndpoint = getComputeEndpoint(projectId, SUFFIX_ENDPOINT_FLAVORS);

        try {
            String jsonResponse = this.client.doGetRequest(flavorsEndpoint, openStackV3Token);
            GetAllFlavorsResponse getAllFlavorsResponse = GetAllFlavorsResponse.fromJson(jsonResponse);

            List<String> flavorsIds = new ArrayList<>();
            for (GetAllFlavorsResponse.Flavor flavor : getAllFlavorsResponse.getFlavors()) {
                if (computeOrder != null && computeOrder.getRequirements() != null && computeOrder.getRequirements().size() > 0) {
                    if (!flavorHasRequirements(openStackV3Token, computeOrder.getRequirements(), flavor.getId())) {
                        continue;
                    }
                }
                flavorsIds.add(flavor.getId());
            }

            TreeSet<HardwareRequirements> newHardwareRequirements =
                    detailFlavors(flavorsEndpoint, openStackV3Token, flavorsIds);
            setHardwareRequirementsList(newHardwareRequirements);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
    }

    private boolean flavorHasRequirements(CloudToken openStackV3Token, Map<String, String> requirements,
                                          String flavorId) throws FogbowException {
        String projectId = getProjectId(openStackV3Token);
        String specsEndpoint = getComputeEndpoint(projectId, SUFFIX_ENDPOINT_FLAVORS)  + "/" + flavorId + SUFFIX_FLAVOR_EXTRA_SPECS;

        try {
            String jsonResponse = this.client.doGetRequest(specsEndpoint, openStackV3Token);
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

    private TreeSet<HardwareRequirements> detailFlavors(String endpoint, CloudToken openStackV3Token,
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
                    getJsonResponse = this.client.doGetRequest(newEndpoint, openStackV3Token);
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

    private ComputeInstance getInstanceFromJson(String getRawResponse, CloudToken openStackV3Token)
            throws FogbowException, UnexpectedException {
        GetComputeResponse getComputeResponse = GetComputeResponse.fromJson(getRawResponse);

        String flavorId = getComputeResponse.getFlavor().getId();
        HardwareRequirements hardwareRequirements = getFlavorById(flavorId, openStackV3Token);

        if (hardwareRequirements == null) {
            throw new NoAvailableResourcesException(Messages.Exception.NO_MATCHING_FLAVOR);
        }

        int vcpusCount = hardwareRequirements.getCpu();
        int memory = hardwareRequirements.getMemory();
        int disk = hardwareRequirements.getDisk();

        String openStackState = getComputeResponse.getStatus();
        InstanceState fogbowState = OpenStackStateMapper.map(ResourceType.COMPUTE, openStackState);

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
                fogbowState, hostName, vcpusCount, memory, disk, ipAddresses);

        return computeInstance;
    }

    private HardwareRequirements getFlavorById(String id, CloudToken openStackV3Token) throws FogbowException {
        updateFlavors(openStackV3Token, null);
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
