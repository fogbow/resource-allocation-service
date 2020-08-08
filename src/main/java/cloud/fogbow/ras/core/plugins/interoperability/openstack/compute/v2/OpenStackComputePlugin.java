package cloud.fogbow.ras.core.plugins.interoperability.openstack.compute.v2;

import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.ras.api.http.response.NetworkSummary;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.HardwareRequirements;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.core.plugins.interoperability.ComputePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.OpenStackPluginUtils;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.OpenStackStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.requests.CreateComputeRequest;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.requests.CreateOsKeypairRequest;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.responses.*;
import cloud.fogbow.ras.core.plugins.interoperability.util.DefaultLaunchCommandGenerator;
import cloud.fogbow.ras.core.plugins.interoperability.util.LaunchCommandGenerator;
import com.google.common.annotations.VisibleForTesting;
import org.apache.log4j.Logger;

import java.util.*;

public class OpenStackComputePlugin implements ComputePlugin<OpenStackV3User> {
    private static final Logger LOGGER = Logger.getLogger(OpenStackComputePlugin.class);

    private TreeSet<HardwareRequirements> hardwareRequirementsList;
    private Properties properties;
    private OpenStackHttpClient client;
    private LaunchCommandGenerator launchCommandGenerator;

    public OpenStackComputePlugin(String confFilePath) throws FatalErrorException {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        this.launchCommandGenerator = new DefaultLaunchCommandGenerator();
        instantiateOtherAttributes();
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
        LOGGER.info(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER);
        HardwareRequirements hardwareRequirements = findSmallestFlavor(computeOrder, cloudUser);
        String flavorId = hardwareRequirements.getFlavorId();
        String projectId = OpenStackPluginUtils.getProjectIdFrom(cloudUser);
        // Even if one or more private networks are informed in networkIds, we still need to include the default
        // network, since this is the only network that has external set to true, and a floating IP can only
        // be attached to such type of network.
        // We add the default network before any other network, because the order is very important to Openstack
        // request. Openstack will configure the routes to the external network using the first network found on
        // the request body.
        List<String> networkIds = getNetworkIds(computeOrder);
        String keyName = getKeyName(projectId, computeOrder.getPublicKey(), cloudUser);

        String imageId = computeOrder.getImageId();
        String userData = this.launchCommandGenerator.createLaunchCommand(computeOrder);
        String instanceName = computeOrder.getName();

        CreateComputeRequest request = getRequestBody(instanceName, imageId, flavorId, userData, keyName, networkIds);

        String body = request.toJson();

        String instanceId = doRequestInstance(projectId, keyName, body, cloudUser);
        setAllocationToOrder(computeOrder, hardwareRequirements);
        return instanceId;
    }
    @Override
    public ComputeInstance getInstance(ComputeOrder computeOrder, OpenStackV3User cloudUser) throws FogbowException {
        String instanceId = computeOrder.getInstanceId();
        LOGGER.info(String.format(Messages.Log.GETTING_INSTANCE_S, instanceId));
        String projectId = OpenStackPluginUtils.getProjectIdFrom(cloudUser);
        String endpoint = getComputeEndpoint(projectId, OpenStackConstants.SERVERS_ENDPOINT + "/" + computeOrder.getInstanceId());

        String jsonResponse = doGetInstance(endpoint, cloudUser);

        ComputeInstance computeInstance = getInstanceFromJson(jsonResponse);


        computeInstance.setNetworks(getComputeNetworks());
        return computeInstance;
    }

    @Override
    public void deleteInstance(ComputeOrder computeOrder, OpenStackV3User cloudUser) throws FogbowException {
        String instanceId = computeOrder.getInstanceId();
        LOGGER.info(String.format(Messages.Log.DELETING_INSTANCE_S, instanceId));
        String projectId = OpenStackPluginUtils.getProjectIdFrom(cloudUser);
        String endpoint = getComputeEndpoint(projectId, OpenStackConstants.SERVERS_ENDPOINT + "/" + computeOrder.getInstanceId());
        this.doDeleteRequest(endpoint, cloudUser);
    }

    @VisibleForTesting
    String doGetInstance(String endpoint, OpenStackV3User cloudUser) throws FogbowException {
        String jsonResponse = this.client.doGetRequest(endpoint, cloudUser);
        return jsonResponse;
    }

    @VisibleForTesting
    List<NetworkSummary> getComputeNetworks() {
        // The default network is always included in the order by the OpenStack plugin, thus it should be added
        // in the map of networks in the ComputeInstance by the plugin. The remaining networks passed by the user
        // are appended by the LocalCloudConnector.
        String defaultNetworkId = this.properties.getProperty(OpenStackPluginUtils.DEFAULT_NETWORK_ID_KEY);
        List<NetworkSummary> computeNetworks = new ArrayList<>();
        computeNetworks.add(new NetworkSummary(defaultNetworkId, SystemConstants.DEFAULT_NETWORK_NAME));
        return computeNetworks;
    }

    @VisibleForTesting
    List<String> getNetworkIds(ComputeOrder computeOrder) {
        List<String> networkIds = new ArrayList<>();
        String defaultNetworkId = this.properties.getProperty(OpenStackPluginUtils.DEFAULT_NETWORK_ID_KEY);
        networkIds.add(defaultNetworkId);
        List<String> userDefinedNetworks = computeOrder.getNetworkIds();
        if (!userDefinedNetworks.isEmpty()) {
            networkIds.addAll(userDefinedNetworks);
        }
        return networkIds;
    }

    @VisibleForTesting
    String doRequestInstance(String projectId, String keyName, String body, OpenStackV3User cloudUser)
            throws FogbowException {
        String endpoint = getComputeEndpoint(projectId, OpenStackConstants.SERVERS_ENDPOINT);

        String instanceId = null;

        try {
            String response = this.client.doPostRequest(endpoint, body, cloudUser);
            CreateComputeResponse createComputeResponse = CreateComputeResponse.fromJson(response);
            instanceId = createComputeResponse.getId();
        } catch (FogbowException e) {
            throw e;
        } finally {
            if (keyName != null) {
                deleteKeyName(projectId, keyName, cloudUser);
            }
        }

        return instanceId;
    }

    @VisibleForTesting
    void setAllocationToOrder(ComputeOrder computeOrder, HardwareRequirements hardwareRequirements) {
        synchronized (computeOrder) {
            ComputeAllocation actualAllocation = new ComputeAllocation(
                    1, hardwareRequirements.getCpu(),
                    hardwareRequirements.getRam(),
                    hardwareRequirements.getDisk());
            // When the ComputeOrder is remote, this field must be copied into its local counterpart
            // that is updated when the requestingProvider receives the reply from the providingProvider
            // (see RemoteFacade.java)
            computeOrder.setActualAllocation(actualAllocation);
        }
    }

    private void instantiateOtherAttributes() {
        this.hardwareRequirementsList = new TreeSet<HardwareRequirements>();
        this.initClient();
    }

    private void initClient() {
        this.client = new OpenStackHttpClient();
    }

    @VisibleForTesting
    String getKeyName(String projectId, String publicKey, OpenStackV3User cloudUser) throws FogbowException {
        String keyName = null;

        if (publicKey != null && !publicKey.isEmpty()) {
            String osKeypairEndpoint = getComputeEndpoint(projectId, OpenStackConstants.KEYPAIRS_ENDPOINT);
            keyName = getRandomUUID();
            CreateOsKeypairRequest request = new CreateOsKeypairRequest.Builder()
                    .name(keyName)
                    .publicKey(publicKey)
                    .build();

            String body = request.toJson();
            doCreateKeyName(osKeypairEndpoint, body, cloudUser);
        }

        return keyName;
    }

    @VisibleForTesting
    void doCreateKeyName(String osKeypairEndpoint, String body, OpenStackV3User cloudUser) throws FogbowException {
        this.client.doPostRequest(osKeypairEndpoint, body, cloudUser);
    }

    @VisibleForTesting
    String getComputeEndpoint(String projectId, String suffix) {
        return this.properties.getProperty(OpenStackPluginUtils.COMPUTE_NOVA_URL_KEY) +
                OpenStackConstants.NOVA_V2_API_ENDPOINT + OpenStackConstants.ENDPOINT_SEPARATOR + projectId + suffix;
    }

    @VisibleForTesting
    void deleteKeyName(String projectId, String keyName, OpenStackV3User cloudUser) throws FogbowException {
        String suffixEndpoint = OpenStackConstants.KEYPAIRS_ENDPOINT + "/" + keyName;
        String keyNameEndpoint = getComputeEndpoint(projectId, suffixEndpoint);

        doDeleteRequest(keyNameEndpoint, cloudUser);
    }

    @VisibleForTesting
    CreateComputeRequest getRequestBody(String instanceName, String imageRef, String flavorRef, String userdata,
                                                  String keyName, List<String> networksIds) {
        List<CreateComputeRequest.Network> networks = new ArrayList<>();
        List<CreateComputeRequest.SecurityGroup> securityGroups = new ArrayList<>();
        for (String networkId : networksIds) {
            networks.add(new CreateComputeRequest.Network(networkId));

            String defaultNetworkId = this.properties.getProperty(OpenStackPluginUtils.DEFAULT_NETWORK_ID_KEY);
            if (!networkId.equals(defaultNetworkId)) {
                String securityGroupName = OpenStackPluginUtils.getNetworkSecurityGroupName(networkId);
                securityGroups.add(new CreateComputeRequest.SecurityGroup(securityGroupName));
            }
        }

        return getCreateComputeRequest(instanceName, imageRef, flavorRef, userdata, keyName, networks, securityGroups);
    }

    @VisibleForTesting
    CreateComputeRequest getCreateComputeRequest(String instanceName, String imageRef, String flavorRef,
                                                        String userdata, String keyName, List<CreateComputeRequest.Network> networks,
                                                        List<CreateComputeRequest.SecurityGroup> securityGroups) {
        // do not specify security groups if no additional network was given
        securityGroups = securityGroups.isEmpty() ? null : securityGroups;

        CreateComputeRequest createComputeRequest = new CreateComputeRequest.Builder()
                .name(instanceName)
                .imageReference(imageRef)
                .flavorReference(flavorRef)
                .userData(userdata)
                .keyName(keyName)
                .networks(networks)
                .securityGroups(securityGroups)
                .build();

        return createComputeRequest;
    }

    @VisibleForTesting
    HardwareRequirements findSmallestFlavor(ComputeOrder computeOrder, OpenStackV3User cloudUser)
            throws FogbowException {
        HardwareRequirements bestFlavor = getBestFlavor(computeOrder, cloudUser);
        if (bestFlavor == null) {
            throw new UnacceptableOperationException();
        }
        return bestFlavor;
    }

    @VisibleForTesting
    HardwareRequirements getBestFlavor(ComputeOrder computeOrder, OpenStackV3User cloudUser)
            throws FogbowException {
        updateFlavors(computeOrder, cloudUser);
        TreeSet<HardwareRequirements> hardwareRequirementsList = getHardwareRequirementsList();
        for (HardwareRequirements hardwareRequirements : hardwareRequirementsList) {
            if (hardwareRequirements.getCpu() >= computeOrder.getvCPU()
                    && hardwareRequirements.getRam() >= computeOrder.getRam()
                    && hardwareRequirements.getDisk() >= computeOrder.getDisk()) {
                return hardwareRequirements;
            }
        }
        return null;
    }

    @VisibleForTesting
    void updateFlavors(ComputeOrder computeOrder, OpenStackV3User cloudUser) throws FogbowException {
        String projectId = OpenStackPluginUtils.getProjectIdFrom(cloudUser);
        String flavorsEndpoint = getComputeEndpoint(projectId, OpenStackConstants.FLAVORS_ENDPOINT);

        String jsonResponse = doGetRequest(flavorsEndpoint, cloudUser);
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
                detailFlavors(flavorsEndpoint, flavorsIds, cloudUser);
        setHardwareRequirementsList(newHardwareRequirements);
    }

    @VisibleForTesting
    boolean flavorHasRequirements(OpenStackV3User cloudUser, Map<String, String> requirements,
                                          String flavorId) throws FogbowException {
        String projectId = OpenStackPluginUtils.getProjectIdFrom(cloudUser);
        String specsEndpoint = getComputeEndpoint(projectId, OpenStackConstants.FLAVORS_ENDPOINT)  + "/" + flavorId +
                OpenStackConstants.EXTRA_SPECS_ENDPOINT;

        String jsonResponse = doGetRequest(specsEndpoint, cloudUser);
        GetFlavorExtraSpecsResponse getFlavorExtraSpecsResponse = GetFlavorExtraSpecsResponse.fromJson(jsonResponse);
        Map<String, String> flavorExtraSpecs = getFlavorExtraSpecsResponse.getFlavorExtraSpecs();

        for (String tag : requirements.keySet()) {
            if (!flavorExtraSpecs.containsKey(tag) || !flavorExtraSpecs.get(tag).equals(requirements.get(tag))) {
                return false;
            }
        }

        return true;
    }

    @VisibleForTesting
    TreeSet<HardwareRequirements> detailFlavors(String endpoint, List<String> flavorsIds, OpenStackV3User cloudUser)
            throws FogbowException {
        TreeSet<HardwareRequirements> newHardwareRequirements = new TreeSet<>();
        TreeSet<HardwareRequirements> flavorsCopy = getHardwareRequirementsList();

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
                HardwareRequirements requirements = fetchHardwareRequirements(endpoint, flavorId, cloudUser);
                newHardwareRequirements.add(requirements);
            }
        }
        return newHardwareRequirements;
    }

    @VisibleForTesting
    HardwareRequirements fetchHardwareRequirements(String endpoint, String flavorId, OpenStackV3User cloudUser) throws FogbowException {
        String hardwareRequirementsEndpoint = endpoint + "/" + flavorId;

        String getJsonResponse = null;

        getJsonResponse = doGetRequest(hardwareRequirementsEndpoint, cloudUser);
        GetFlavorResponse getFlavorResponse = GetFlavorResponse.fromJson(getJsonResponse);

        String id = getFlavorResponse.getId();
        String name = getFlavorResponse.getName();
        int disk = getFlavorResponse.getDisk();
        int memory = getFlavorResponse.getMemory();
        int vcpusCount = getFlavorResponse.getVcpusCount();

        return new HardwareRequirements(name, id, vcpusCount, memory, disk);
    }

    @VisibleForTesting
    ComputeInstance getInstanceFromJson(String getRawResponse) {
        GetComputeResponse getComputeResponse = GetComputeResponse.fromJson(getRawResponse);

        String openStackState = getComputeResponse.getStatus();
        String instanceId = getComputeResponse.getId();
        String hostName = getComputeResponse.getName();
        String faultMessage = getComputeResponse.getFaultMessage();

        Map<String, GetComputeResponse.Address[]> addressesContainer = getComputeResponse.getAddresses();
        List<String> ipAddresses = new ArrayList<>();

        if (addressesContainer != null) {
            for (GetComputeResponse.Address[] addresses : addressesContainer.values()) {
                for (GetComputeResponse.Address address : addresses) {
                    ipAddresses.add(address.getAddress());
                }
            }
        }

        return new ComputeInstance(instanceId, openStackState, hostName, ipAddresses, faultMessage);
    }

    @VisibleForTesting
    String doGetRequest(String endpoint, OpenStackV3User clouUser) throws FogbowException {
        String responseStr = this.client.doGetRequest(endpoint, clouUser);
        return responseStr;
    }

    @VisibleForTesting
    void doDeleteRequest(String endpoint, OpenStackV3User cloudUser) throws FogbowException {
        this.client.doDeleteRequest(endpoint, cloudUser);
    }

    @VisibleForTesting
    TreeSet<HardwareRequirements> getHardwareRequirementsList() {
        synchronized (this.hardwareRequirementsList) {
            return new TreeSet<HardwareRequirements>(this.hardwareRequirementsList);
        }
    }

    @VisibleForTesting
    void setHardwareRequirementsList(TreeSet<HardwareRequirements> hardwareRequirementsList) {
        synchronized (this.hardwareRequirementsList) {
            this.hardwareRequirementsList = hardwareRequirementsList;
        }
    }

    private String getRandomUUID() {
        return UUID.randomUUID().toString();
    }

    @VisibleForTesting
    void setClient(OpenStackHttpClient client) {
        this.client = client;
    }

    @VisibleForTesting
    void setLaunchCommandGenerator(LaunchCommandGenerator launchCommandGenerator) {
        this.launchCommandGenerator = launchCommandGenerator;
    }
}
