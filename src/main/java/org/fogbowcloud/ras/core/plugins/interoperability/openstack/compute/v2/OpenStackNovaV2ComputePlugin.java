package org.fogbowcloud.ras.core.plugins.interoperability.openstack.compute.v2;

import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.*;
import org.fogbowcloud.ras.core.models.HardwareRequirements;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.instances.ComputeInstance;
import org.fogbowcloud.ras.core.models.instances.InstanceState;
import org.fogbowcloud.ras.core.models.orders.ComputeOrder;
import org.fogbowcloud.ras.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.ras.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.ras.core.plugins.interoperability.ComputePlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenStackHttpToFogbowRasExceptionMapper;
import org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenStackStateMapper;
import org.fogbowcloud.ras.core.plugins.interoperability.openstack.network.v2.OpenStackV2NetworkPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.util.DefaultLaunchCommandGenerator;
import org.fogbowcloud.ras.core.plugins.interoperability.util.LaunchCommandGenerator;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestUtil;

import java.util.*;

public class OpenStackNovaV2ComputePlugin implements ComputePlugin<OpenStackV3Token> {
    protected static final Logger LOGGER = Logger.getLogger(OpenStackNovaV2ComputePlugin.class);

    protected static final String COMPUTE_NOVAV2_URL_KEY = "openstack_nova_v2_url";
    public static final String DEFAULT_NETWORK_ID_KEY = "default_network_id";
    protected static final String ID_JSON_FIELD = "id";
    protected static final String NAME_JSON_FIELD = "name";
    protected static final String SERVER_JSON_FIELD = "server";
    protected static final String FLAVOR_REF_JSON_FIELD = "flavorRef";
    protected static final String FLAVOR_JSON_FIELD = "flavor";
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
    protected static final String FOGBOW_INSTANCE_NAME = "fogbow-compute-instance-";
    protected static final String PROJECT_ID = "projectId";
    protected static final String SERVERS = "/servers";
    protected static final String SUFFIX_ENDPOINT_KEYPAIRS = "/os-keypairs";
    protected static final String SUFFIX_ENDPOINT_FLAVORS = "/flavors";
    protected static final String COMPUTE_V2_API_ENDPOINT = "/v2/";
    protected static final String ADDRESS_FIELD = "addresses";
    protected static final String PROVIDER_NETWORK_FIELD = "provider";
    protected static final String ADDR_FIELD = "addr";
    private TreeSet<HardwareRequirements> hardwareRequirementsList;
    private Properties properties;
    private HttpRequestClientUtil client;
    private LaunchCommandGenerator launchCommandGenerator;

    public OpenStackNovaV2ComputePlugin() throws FatalErrorException {
        this.properties = PropertiesUtil.readProperties(HomeDir.getPath() +
                DefaultConfigurationConstants.OPENSTACK_CONF_FILE_NAME);
        this.launchCommandGenerator = new DefaultLaunchCommandGenerator();
        instantiateOtherAttributes();
    }

    /**
     * Constructor used for testing only
     */
    protected OpenStackNovaV2ComputePlugin(Properties properties, LaunchCommandGenerator launchCommandGenerator,
                                           HttpRequestClientUtil client) {
        this.properties = properties;
        this.launchCommandGenerator = launchCommandGenerator;
        this.client = client;
        this.hardwareRequirementsList = new TreeSet<HardwareRequirements>();
    }

    public String requestInstance(ComputeOrder computeOrder, OpenStackV3Token openStackV3Token)
            throws FogbowRasException, UnexpectedException {
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
                        hardwareRequirements.getRam(),
                        1);
                // When the ComputeOrder is remote, this field must be copied into its local counterpart
                // that is updated when the requestingMember receives the reply from the providingMember
                // (see RemoteFacade.java)
                computeOrder.setActualAllocation(actualAllocation);
            }
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowRasExceptionMapper.map(e);
        } finally {
            if (keyName != null) {
                deleteKeyName(projectId, openStackV3Token, keyName);
            }
        }
        return instanceId;
    }

    private String doRequestInstance(OpenStackV3Token openStackV3Token, String flavorId, List<String> networksId,
                                     String imageId, String instanceName, String userData, String keyName, String endpoint)
            throws UnavailableProviderException, HttpResponseException {
        CreateComputeRequest createBody = getRequestBody(instanceName, imageId, flavorId, userData, keyName, networksId);

        String body = createBody.toJson();
        String response = this.client.doPostRequest(endpoint, openStackV3Token, body);
        CreateComputeResponse createComputeResponse = CreateComputeResponse.fromJson(response);

        return createComputeResponse.getId();
    }

    @Override
    public ComputeInstance getInstance(String instanceId, OpenStackV3Token openStackV3Token)
            throws FogbowRasException, UnexpectedException {
        LOGGER.info("Getting instance " + instanceId + " with tokens " + openStackV3Token);

        String projectId = getProjectId(openStackV3Token);
        String requestEndpoint = getComputeEndpoint(projectId, SERVERS + "/" + instanceId);

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(requestEndpoint, openStackV3Token);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowRasExceptionMapper.map(e);
        }
        ComputeInstance computeInstance = getInstanceFromJson(jsonResponse, openStackV3Token);
        return computeInstance;
    }

    @Override
    public void deleteInstance(String instanceId, OpenStackV3Token openStackV3Token)
            throws FogbowRasException, UnexpectedException {
        LOGGER.info("Deleting instance " + instanceId + " with tokens " + openStackV3Token);
        String endpoint = getComputeEndpoint(getProjectId(openStackV3Token), SERVERS + "/" + instanceId);
        try {
            this.client.doDeleteRequest(endpoint, openStackV3Token);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowRasExceptionMapper.map(e);
        }
    }

    private void instantiateOtherAttributes() {
        this.hardwareRequirementsList = new TreeSet<HardwareRequirements>();
        this.initClient();
    }

    private void initClient() {
        HttpRequestUtil.init();
        this.client = new HttpRequestClientUtil();
    }

    private String getProjectId(OpenStackV3Token openStackV3Token) throws InvalidParameterException {
        String projectId = openStackV3Token.getProjectId();
        if (projectId == null) {
            throw new InvalidParameterException("No projectId in local token.");
        }
        return projectId;
    }

    private List<String> resolveNetworksId(ComputeOrder computeOrder) {
        List<String> requestedNetworksId = new ArrayList<>();
        String defaultNetworkId = this.properties.getProperty(DEFAULT_NETWORK_ID_KEY);

        //We add the default network before any other network, because the order is very important to Openstack
        //request. Openstack will configure the routes to the external network by the first network found on request body.
        requestedNetworksId.add(defaultNetworkId);
        requestedNetworksId.addAll(computeOrder.getNetworksId());
        computeOrder.setNetworksId(requestedNetworksId);
        return requestedNetworksId;
    }

    private String getKeyName(String projectId, OpenStackV3Token openStackV3Token, String publicKey)
            throws FogbowRasException, UnexpectedException {
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
                OpenStackHttpToFogbowRasExceptionMapper.map(e);
            }
        }

        return keyName;
    }

    private String getComputeEndpoint(String projectId, String suffix) {
        return this.properties.getProperty(COMPUTE_NOVAV2_URL_KEY) + COMPUTE_V2_API_ENDPOINT + projectId + suffix;
    }

    private void deleteKeyName(String projectId, OpenStackV3Token openStackV3Token, String keyName)
            throws FogbowRasException, UnexpectedException {
        String suffixEndpoint = SUFFIX_ENDPOINT_KEYPAIRS + "/" + keyName;
        String keyNameEndpoint = getComputeEndpoint(projectId, suffixEndpoint);

        try {
            this.client.doDeleteRequest(keyNameEndpoint, openStackV3Token);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowRasExceptionMapper.map(e);
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
                String prefix = OpenStackV2NetworkPlugin.SECURITY_GROUP_PREFIX;
                String securityGroupName = prefix + "-" + networkId;
                securityGroups.add(new CreateComputeRequest.SecurityGroup(securityGroupName));
            }
        }

        // do not specify security groups if no additional network was given
        securityGroups = securityGroups.size() == 0 ? null : securityGroups;

        String name = instanceName == null ?  FOGBOW_INSTANCE_NAME + getRandomUUID() : instanceName;
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

    private HardwareRequirements findSmallestFlavor(ComputeOrder computeOrder, OpenStackV3Token openStackV3Token)
            throws UnexpectedException, FogbowRasException {
        HardwareRequirements bestFlavor = getBestFlavor(computeOrder, openStackV3Token);
        if (bestFlavor == null) {
            throw new NoAvailableResourcesException();
        }
        return bestFlavor;
    }

    private HardwareRequirements getBestFlavor(ComputeOrder computeOrder, OpenStackV3Token openStackV3Token)
            throws FogbowRasException, UnexpectedException {
        updateFlavors(openStackV3Token);
        TreeSet<HardwareRequirements> hardwareRequirementsList = getHardwareRequirementsList();
        for (HardwareRequirements hardwareRequirements : hardwareRequirementsList) {
            if (hardwareRequirements.getCpu() >= computeOrder.getvCPU()
                    && hardwareRequirements.getRam() >= computeOrder.getMemory()
                    && hardwareRequirements.getDisk() >= computeOrder.getDisk()) {
                return hardwareRequirements;
            }
        }
        return null;
    }

    private void updateFlavors(OpenStackV3Token openStackV3Token) throws FogbowRasException, UnexpectedException {
        String projectId = getProjectId(openStackV3Token);
        String flavorsEndpoint = getComputeEndpoint(projectId, SUFFIX_ENDPOINT_FLAVORS);

        try {
            String jsonResponse = this.client.doGetRequest(flavorsEndpoint, openStackV3Token);
            GetAllFlavorsResponse getAllFlavorsResponse = GetAllFlavorsResponse.fromJson(jsonResponse);

            List<String> flavorsIds = new ArrayList<>();
            for (GetAllFlavorsResponse.Flavor flavor : getAllFlavorsResponse.getFlavors()) {
                flavorsIds.add(flavor.getId());
            }

            TreeSet<HardwareRequirements> newHardwareRequirements =
                    detailFlavors(flavorsEndpoint, openStackV3Token, flavorsIds);
            setHardwareRequirementsList(newHardwareRequirements);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowRasExceptionMapper.map(e);
        }
    }

    private TreeSet<HardwareRequirements> detailFlavors(String endpoint, OpenStackV3Token openStackV3Token,
                                                        List<String> flavorsIds)
            throws FogbowRasException, UnexpectedException {
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
                    OpenStackHttpToFogbowRasExceptionMapper.map(e);
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

    private ComputeInstance getInstanceFromJson(String getRawResponse, OpenStackV3Token openStackV3Token)
            throws FogbowRasException, UnexpectedException {
        GetComputeResponse getComputeResponse = GetComputeResponse.fromJson(getRawResponse);

        String flavorId = getComputeResponse.getFlavor().getId();
        HardwareRequirements hardwareRequirements = getFlavorById(flavorId, openStackV3Token);

        if (hardwareRequirements == null) {
            throw new NoAvailableResourcesException("No matching flavor");
        }

        int vcpusCount = hardwareRequirements.getCpu();
        int memory = hardwareRequirements.getRam();
        int disk = hardwareRequirements.getDisk();

        String openStackState = getComputeResponse.getStatus();
        InstanceState fogbowState = OpenStackStateMapper.map(ResourceType.COMPUTE, openStackState);

        String instanceId = getComputeResponse.getId();
        String hostName = getComputeResponse.getName();

        GetComputeResponse.Addresses addressesContainer = getComputeResponse.getAddresses();

        String address = "";
        if (addressesContainer != null) {
            GetComputeResponse.Address[] addresses = addressesContainer.getProviderAddresses();
            boolean firstAddressEmpty = addresses == null || addresses.length == 0 || addresses[0].getAddress() == null;
            address = firstAddressEmpty ? "" : addresses[0].getAddress();
        }

        ComputeInstance computeInstance = new ComputeInstance(instanceId,
                fogbowState, hostName, vcpusCount, memory, disk, address);
        return computeInstance;
    }

    private HardwareRequirements getFlavorById(String id, OpenStackV3Token openStackV3Token) throws FogbowRasException, UnexpectedException {
        updateFlavors(openStackV3Token);
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
