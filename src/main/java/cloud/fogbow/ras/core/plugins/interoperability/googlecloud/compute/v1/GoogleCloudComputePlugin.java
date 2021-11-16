package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.compute.v1;

import cloud.fogbow.common.constants.GoogleCloudConstants;
import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.NotImplementedOperationException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.models.GoogleCloudUser;
import cloud.fogbow.common.util.BinaryUnit;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.NetworkSummary;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.compute.models.*;
import cloud.fogbow.common.util.connectivity.cloud.googlecloud.GoogleCloudHttpClient;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.HardwareRequirements;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.ComputePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.util.GoogleCloudPluginUtils;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.util.GoogleCloudStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.util.DefaultLaunchCommandGenerator;
import cloud.fogbow.ras.core.plugins.interoperability.util.LaunchCommandGenerator;
import com.google.common.annotations.VisibleForTesting;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class GoogleCloudComputePlugin implements ComputePlugin<GoogleCloudUser> {
    private static final Logger LOGGER = Logger.getLogger(GoogleCloudComputePlugin.class);

    @VisibleForTesting
    static int RAM_MULTIPLE_NUMBER = 256;
    @VisibleForTesting
    static double LOWER_BOUND_RAM_PER_VCPU_GB = 0.9;
    @VisibleForTesting
    static double UPPER_BOUND_RAM_PER_VCPU_GB = 6.4;
    @VisibleForTesting
    static int LOWER_BOUND_DISK_GB = 10;

    @VisibleForTesting
    static String DEFAULT_USER_DATA_ENCODING = "base64";

    @VisibleForTesting
    static String SSH_PARTS_SEPARATOR = " ";
    @VisibleForTesting
    static String COLON_SEPARATOR = ":";

    private Properties properties;
    private GoogleCloudHttpClient client;
    private LaunchCommandGenerator launchCommandGenerator;
    private String zone;

    public GoogleCloudComputePlugin(String confFilePath) throws FatalErrorException {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        this.launchCommandGenerator = new DefaultLaunchCommandGenerator();
        this.client = new GoogleCloudHttpClient();
        this.zone = this.properties.getProperty(GoogleCloudConstants.ZONE_KEY_CONFIG);
    }
    
    public GoogleCloudComputePlugin(String confFilePath, LaunchCommandGenerator launchCommandGenerator, 
            GoogleCloudHttpClient googleCloudClient) 
            throws FatalErrorException {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        this.launchCommandGenerator = launchCommandGenerator;
        this.client = googleCloudClient;
        this.zone = this.properties.getProperty(GoogleCloudConstants.ZONE_KEY_CONFIG);
    }

    @Override
    public String requestInstance(ComputeOrder computeOrder, GoogleCloudUser cloudUser) throws FogbowException {
        LOGGER.info(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER);
        String projectId = GoogleCloudPluginUtils.getProjectIdFrom(cloudUser);
        String endpoint = getComputeEndpoint(projectId);

        HardwareRequirements hardwareRequirements = findSmallestFlavor(computeOrder);
        String name = computeOrder.getName();
        String flavorId = getFlavorId(hardwareRequirements.getFlavorId());
        CreateComputeRequest.MetaData metaData = getMetaData(computeOrder);
        List<CreateComputeRequest.Disk> disks = getDisks(projectId, computeOrder.getImageId(), hardwareRequirements.getDisk());
        List<CreateComputeRequest.Network> networks = getNetworkIds(projectId, computeOrder, cloudUser);

        CreateComputeRequest request = getComputeRequestBody(name, flavorId, metaData, networks, disks);
        String body = request.toJson();

        String instanceId = doRequestInstance(endpoint, body, cloudUser);
        // By default, Fogbow will keep the default security rules of default network
        //deletePrePopulatedDefaultNetworkSecurityRules(projectId, cloudUser);
        setAllocationToOrder(computeOrder, hardwareRequirements);
        return instanceId;
    }

    @Override
    public ComputeInstance getInstance(ComputeOrder computeOrder, GoogleCloudUser cloudUser) throws FogbowException {
        String instanceId = computeOrder.getInstanceId();
        LOGGER.info(String.format(Messages.Log.GETTING_INSTANCE_S, instanceId));
        String projectId = GoogleCloudPluginUtils.getProjectIdFrom(cloudUser);
        String endpoint = getInstanceEndpoint(projectId, instanceId);

        ComputeInstance computeInstance = doGetInstance(endpoint, cloudUser);

        computeInstance.setNetworks(getComputeNetworks(projectId));
        return computeInstance;
    }

    @Override
    public void deleteInstance(ComputeOrder computeOrder, GoogleCloudUser cloudUser) throws FogbowException {
        String instanceId = computeOrder.getInstanceId();
        LOGGER.info(String.format(Messages.Log.DELETING_INSTANCE_S, instanceId));

        String projectId = GoogleCloudPluginUtils.getProjectIdFrom(cloudUser);

        String computeEndpoint = getComputeEndpoint(projectId);
        String endpoint = getPathWithId(computeEndpoint, instanceId);

        this.client.doDeleteRequest(endpoint, cloudUser);
    }

    @Override
    public void takeSnapshot(ComputeOrder computeOrder, String name, GoogleCloudUser cloudUser) throws FogbowException {
        // TODO implement
        throw new NotImplementedOperationException();
    }

    @Override
    public void pauseInstance(ComputeOrder order, GoogleCloudUser cloudUser) throws FogbowException {
        throw new NotImplementedOperationException();
    }

    @Override
    public void hibernateInstance(ComputeOrder order, GoogleCloudUser cloudUser) throws FogbowException {
        throw new NotImplementedOperationException();
    }

    @Override
    public void stopInstance(ComputeOrder computeOrder, GoogleCloudUser cloudUser) throws FogbowException {
        String instanceId = computeOrder.getInstanceId();
        LOGGER.info(String.format(Messages.Log.STOPPING_INSTANCE_S, instanceId));

        String projectId = GoogleCloudPluginUtils.getProjectIdFrom(cloudUser);

        String stopEndpoint = getStopEndpoint(projectId, instanceId);
        String bodyContent = "";
        
        this.client.doPostRequest(stopEndpoint, bodyContent, cloudUser);
    }

    @Override
    public void resumeInstance(ComputeOrder computeOrder, GoogleCloudUser cloudUser) throws FogbowException {
        String instanceId = computeOrder.getInstanceId();
        LOGGER.info(String.format(Messages.Log.RESUMING_INSTANCE_S, instanceId));

        String projectId = GoogleCloudPluginUtils.getProjectIdFrom(cloudUser);

        String resumeEndpoint = getResumeEndpoint(projectId, instanceId);
        String bodyContent = "";
        
        this.client.doPostRequest(resumeEndpoint, bodyContent, cloudUser);
    }

    @Override
    public boolean isPaused(String instanceState) throws FogbowException {
        return false;
    }

    @Override
    public boolean isHibernated(String instanceState) throws FogbowException {
        return false;
    }
    
    @Override
    public boolean isStopped(String instanceState) throws FogbowException {
        return GoogleCloudStateMapper.map(ResourceType.COMPUTE, instanceState).equals(InstanceState.STOPPED);
    }

    @Override
    public boolean isReady(String instanceState) {
        return GoogleCloudStateMapper.map(ResourceType.COMPUTE, instanceState).equals(InstanceState.READY);
    }

    @Override
    public boolean hasFailed(String instanceState) {
        return GoogleCloudStateMapper.map(ResourceType.COMPUTE, instanceState).equals(InstanceState.FAILED);
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

    @VisibleForTesting
    String doRequestInstance(String endpoint, String body, GoogleCloudUser cloudUser)
            throws FogbowException {
        String instanceId = null;

        try {
            String response = this.client.doPostRequest(endpoint, body, cloudUser);
            CreateComputeResponse createComputeResponse = CreateComputeResponse.fromJson(response);
            instanceId = createComputeResponse.getId();
        } catch (FogbowException exception) {
            throw exception;
        }

        return instanceId;
    }

    @VisibleForTesting
    ComputeInstance doGetInstance(String endpoint, GoogleCloudUser cloudUser) throws FogbowException {
        String responseJson = this.client.doGetRequest(endpoint, cloudUser);

        ComputeInstance computeInstance = getInstanceFromJson(responseJson);

        return computeInstance;
    }

    @VisibleForTesting
    String doGetNetworkInstanceName(String endpoint, GoogleCloudUser cloudUser) throws FogbowException {
        String responseJson = this.client.doGetRequest(endpoint, cloudUser);

        GetNetworkResponse networkResponse = GetNetworkResponse.fromJson(responseJson);

        return networkResponse.getName();
    }

    @VisibleForTesting
    List<NetworkSummary> getComputeNetworks(String projectId) {
        String defaultNetworkId = getNetworkId(projectId, GoogleCloudConstants.Network.DEFAULT_NETWORK_NAME);
        List<NetworkSummary> computeNetworks = new ArrayList<>();
        computeNetworks.add(new NetworkSummary(defaultNetworkId, GoogleCloudConstants.Network.DEFAULT_NETWORK_NAME));
        return computeNetworks;
    }

    @VisibleForTesting
    CreateComputeRequest getComputeRequestBody(String name, String flavorId, CreateComputeRequest.MetaData metaData,
                                                       List<CreateComputeRequest.Network> networks, List<CreateComputeRequest.Disk> disks) {

        CreateComputeRequest createComputeRequest = new CreateComputeRequest.Builder()
                .name(name)
                .flavorId(flavorId)
                .metaData(metaData)
                .networks(networks)
                .disks(disks)
                .build();

        return createComputeRequest;
    }

    @VisibleForTesting
    ComputeInstance getInstanceFromJson(String responseJson) {
        GetComputeResponse getComputeResponse = GetComputeResponse.fromJson(responseJson);

        String instanceId = getComputeResponse.getId();
        String name = getComputeResponse.getName();
        String status = getComputeResponse.getStatus();
        String faultMessage = getComputeResponse.getFaultMessage();

        List<GetComputeResponse.Network> addressesList = getComputeResponse.getAddresses();
        List<String> ipAddresses = new ArrayList<String>();
        for (GetComputeResponse.Network network : addressesList) {
            ipAddresses.add(network.getAddress());
        }

        return new ComputeInstance(instanceId, status, name, ipAddresses, faultMessage);
    }

    @VisibleForTesting
    void deletePrePopulatedDefaultNetworkSecurityRules(String projectId, GoogleCloudUser cloudUser) throws FogbowException {
        List<GetFirewallRulesResponse.FirewallRule> securityRules = getDefaultNetworkSecurityRules(projectId, cloudUser);
        if (securityRules != null) {
            for (GetFirewallRulesResponse.FirewallRule securityRule : securityRules) {
                if (isAPrePopulatedSecurityRule(securityRule)) {
                    deleteSecurityRule(securityRule.getId(), projectId, cloudUser);
                }
            }
        }
    }

    @VisibleForTesting
    List<GetFirewallRulesResponse.FirewallRule> getDefaultNetworkSecurityRules(String projectId, GoogleCloudUser cloudUser) throws FogbowException {
        String endpoint = getFirewallsEndpoint(projectId);
        String responseJson = this.client.doGetRequest(endpoint, cloudUser);
        GetFirewallRulesResponse firewallRuleResponse = GetFirewallRulesResponse.fromJson(responseJson);

        return firewallRuleResponse.getSecurityGroupRules();
    }

    @VisibleForTesting
    void deleteSecurityRule(String securityRuleId, String projectId, GoogleCloudUser cloudUser) throws FogbowException {
        String endpoint = getFirewallsEndpoint(projectId) + GoogleCloudConstants.ENDPOINT_SEPARATOR + securityRuleId;
        this.client.doDeleteRequest(endpoint, cloudUser);
    }

    @VisibleForTesting
    boolean isAPrePopulatedSecurityRule(GetFirewallRulesResponse.FirewallRule securityRule) {
        String securityRuleName = securityRule.getName();
        boolean isDefaultAllowICMP =
                securityRuleName.equals(GoogleCloudConstants.Network.Firewall.DEFAULT_ALLOW_ICMP_NAME);
        boolean isDefaultAllowInternal =
                securityRuleName.equals(GoogleCloudConstants.Network.Firewall.DEFAULT_ALLOW_INTERNAL_NAME);
        boolean isDefaultAllowRDP =
                securityRuleName.equals(GoogleCloudConstants.Network.Firewall.DEFAULT_ALLOW_RDP_NAME);
        boolean isDefaultAllowSSH =
                securityRuleName.equals(GoogleCloudConstants.Network.Firewall.DEFAULT_ALLOW_SSH_NAME);
        return isDefaultAllowICMP || isDefaultAllowInternal || isDefaultAllowRDP || isDefaultAllowSSH;
    }

    @VisibleForTesting
    List<CreateComputeRequest.Network> getNetworkIds(String projectId, ComputeOrder computeOrder, GoogleCloudUser cloudUser) throws FogbowException {
        List<CreateComputeRequest.Network> networks = new ArrayList<CreateComputeRequest.Network>();
        for (String networkId : computeOrder.getNetworkIds()) {
            String endpoint = getNetworkEndpoint(projectId, networkId);
            String networkName = doGetNetworkInstanceName(endpoint, cloudUser);
            addToNetworksByName(networks, networkName);
        }
        return networks;
    }

    @VisibleForTesting
    String getNetworkEndpoint(String projectId, String networkId) {
        return getBaseUrl() + GoogleCloudPluginUtils.getProjectEndpoint(projectId)
                + getPathWithId(GoogleCloudConstants.GLOBAL_NETWORKS_ENDPOINT, networkId);
    }

    @VisibleForTesting
    void addToNetworksByName(List<CreateComputeRequest.Network> networks, String networkName) {
        String subnetwork = getSubnetworkId(networkName);
        networks.add(new CreateComputeRequest.Network(subnetwork));
    }

    @VisibleForTesting
    List<CreateComputeRequest.Disk> getDisks(String projectId, String imageId, int diskSizeGb) {
        List<CreateComputeRequest.Disk> disks = new ArrayList<CreateComputeRequest.Disk>();
        CreateComputeRequest.Disk disk = getDisk(projectId, imageId, diskSizeGb);
        disks.add(disk);
        return disks;
    }

    @VisibleForTesting
    CreateComputeRequest.Disk getDisk(String projectId, String imageId, int diskSizeGb) {
        CreateComputeRequest.InicialeParams initializeParams =
                new CreateComputeRequest.InicialeParams(imageId, diskSizeGb);
        return new CreateComputeRequest.Disk(GoogleCloudConstants.Compute.Disk.BOOT_DEFAULT_VALUE,
                GoogleCloudConstants.Compute.Disk.AUTO_DELETE_DEFAULT_VALUE,
                initializeParams);
    }

    @VisibleForTesting
    CreateComputeRequest.MetaData getMetaData(ComputeOrder computeOrder) throws InternalServerErrorException {
        List<CreateComputeRequest.Item> items = new ArrayList<CreateComputeRequest.Item>();
        String publicKey = computeOrder.getPublicKey();

        // Item related to public key
        if (publicKey != null) {
            CreateComputeRequest.Item publicSSHKey = getPublicSSHKey(publicKey);
            items.add(publicSSHKey);
        }

        // Items related to user-data
        CreateComputeRequest.Item userData = getUserData(computeOrder);
        items.add(userData);
        CreateComputeRequest.Item userDataEncoding = getUserDataEncoding();
        items.add(userDataEncoding);

        return new CreateComputeRequest.MetaData(items);
    }

    @VisibleForTesting
    CreateComputeRequest.Item getUserDataEncoding() {
        return new CreateComputeRequest.Item(GoogleCloudConstants.Compute.USER_DATA_ENCODING_KEY_JSON,
                DEFAULT_USER_DATA_ENCODING);
    }

    @VisibleForTesting
    CreateComputeRequest.Item getPublicSSHKey(String publicKey) {
        String[] publicKeySplit = publicKey.split(SSH_PARTS_SEPARATOR);
        int sshUserNameIndex = publicKeySplit.length - 1;
        String userName = publicKeySplit[sshUserNameIndex];
        publicKey = userName.concat(COLON_SEPARATOR).concat(publicKey);
        return new CreateComputeRequest.Item(GoogleCloudConstants.Compute.PUBLIC_SSH_KEY_JSON, publicKey);
    }

    @VisibleForTesting
    CreateComputeRequest.Item getUserData(ComputeOrder computeOrder) throws InternalServerErrorException {

        String userDataValue = this.launchCommandGenerator.createLaunchCommand(computeOrder);
        return new CreateComputeRequest.Item(GoogleCloudConstants.Compute.USER_DATA_KEY_JSON, userDataValue);
    }

    @VisibleForTesting
    String getImageId(String imageId, String projectId) {
        return GoogleCloudPluginUtils.getProjectEndpoint(projectId)
                + getPathWithId(GoogleCloudConstants.GLOBAL_IMAGES_ENDPOINT, imageId);
    }

    @VisibleForTesting
    String getFlavorId(String flavorId) {
        return getZoneEndpoint() + getPathWithId(GoogleCloudConstants.FLAVOR_ENDPOINT, flavorId);
    }

    @VisibleForTesting
    String getNetworkId(String projectId, String networkId) {
        return GoogleCloudPluginUtils.getProjectEndpoint(projectId)
                + getPathWithId(GoogleCloudConstants.GLOBAL_NETWORKS_ENDPOINT, networkId);
    }

    @VisibleForTesting
    String getSubnetworkId(String networkName) {
        String region = GoogleCloudPluginUtils.getRegionByZone(this.zone);
        return getPathWithId(GoogleCloudConstants.REGIONS_ENDPOINT, region)
                + getPathWithId(GoogleCloudConstants.SUBNETS_ENDPOINT, networkName);
    }

    @VisibleForTesting
    String getPathWithId(String path, String id) {
        return path + GoogleCloudConstants.ENDPOINT_SEPARATOR + id;
    }

    @VisibleForTesting
    String getInstanceEndpoint(String projectId, String instanceId) {
        String computePath = getComputeEndpoint(projectId);
        return getPathWithId(computePath, instanceId);
    }

    @VisibleForTesting
    String getZoneEndpoint() {
        return getPathWithId(GoogleCloudConstants.ZONES_ENDPOINT, this.zone);
    }

    @VisibleForTesting
    String getComputeEndpoint(String projectId) {
        return getBaseUrl() + GoogleCloudPluginUtils.getProjectEndpoint(projectId) + getZoneEndpoint() +
                GoogleCloudConstants.INSTANCES_ENDPOINT;
    }

    @VisibleForTesting
    String getBaseUrl() {
        return GoogleCloudConstants.BASE_COMPUTE_API_URL + GoogleCloudConstants.COMPUTE_ENGINE_V1_ENDPOINT;
    }

    @VisibleForTesting
    String getFirewallsEndpoint(String projectId) {
        return getBaseUrl() + GoogleCloudPluginUtils.getProjectEndpoint(projectId) + GoogleCloudConstants.GLOBAL_FIREWALL_ENDPOINT;
    }
    
    @VisibleForTesting
    String getStopEndpoint(String projectId, String instanceId) {
        String computeEndpoint = getComputeEndpoint(projectId);
        String endpoint = getPathWithId(computeEndpoint, instanceId);
        endpoint += GoogleCloudConstants.ENDPOINT_SEPARATOR + GoogleCloudConstants.Compute.STOP_ENDPOINT;
        
        return endpoint;
    }

    @VisibleForTesting
    String getResumeEndpoint(String projectId, String instanceId) {
        String computeEndpoint = getComputeEndpoint(projectId);
        String endpoint = getPathWithId(computeEndpoint, instanceId);
        endpoint += GoogleCloudConstants.ENDPOINT_SEPARATOR + GoogleCloudConstants.Compute.START_ENDPOINT;
        
        return endpoint;
    }
    
    @VisibleForTesting
    HardwareRequirements findSmallestFlavor(ComputeOrder computeOrder) {
        int vCPU = getSmallestvCPU(computeOrder.getvCPU());
        int ramSize = getSmallestRamSize(computeOrder.getRam(), computeOrder.getvCPU());
        int diskSize = getSmallestDiskSize(computeOrder.getDisk());
        String flavorId = getFlavorId(vCPU, ramSize);
        return new HardwareRequirements(computeOrder.getName(), flavorId, vCPU, ramSize, diskSize);
    }

    @VisibleForTesting
    String getFlavorId(int vCPU, int ramSize) {
        return GoogleCloudConstants.Compute.CUSTOM_FLAVOR + GoogleCloudConstants.ELEMENT_SEPARATOR
                + vCPU + GoogleCloudConstants.ELEMENT_SEPARATOR + ramSize;
    }

    @VisibleForTesting
    int getSmallestvCPU(int vCPU) {
        // The number of vCPUs must be 1 or an even number
        if (vCPU > 1 && vCPU % 2 != 0)
            vCPU--;
        return vCPU;
    }

    @VisibleForTesting
    int getSmallestRamSize(int ram, int vCPU) {
        ram = getSmallestMultipleRamSize(ram);
        ram = getSmallestRamSizePervCpu(ram, vCPU);
        return ram;
    }

    @VisibleForTesting
    int getSmallestMultipleRamSize(int ram) {
        // The size of RAM must be in MB and multiple of 256
        return (ram / RAM_MULTIPLE_NUMBER) * RAM_MULTIPLE_NUMBER;
    }

    @VisibleForTesting
    int getSmallestRamSizePervCpu(int ram, int vCPU) {
        // Memory per vCPU must be between 0.9 GB and 6.5 GB
        int betweenBoundsRamSizeInMb = 0;
        double memoryPervCPU = BinaryUnit.megabytes(ram).asGigabytes() / vCPU;
        if (memoryPervCPU <= LOWER_BOUND_RAM_PER_VCPU_GB)  {
            betweenBoundsRamSizeInMb = (int) BinaryUnit.gigabytes(LOWER_BOUND_RAM_PER_VCPU_GB + 0.1).asMegabytes() * vCPU;
        } else if (memoryPervCPU >= UPPER_BOUND_RAM_PER_VCPU_GB) {
            betweenBoundsRamSizeInMb = (int) (BinaryUnit.gigabytes(UPPER_BOUND_RAM_PER_VCPU_GB - 0.1).asMegabytes() * vCPU);
        }

        if (betweenBoundsRamSizeInMb > 0) {
            ram = getSmallestMultipleRamSize(betweenBoundsRamSizeInMb);
        }
        // Ram size returned is between the boundaries and multiple of 256
        return ram;
    }

    @VisibleForTesting
    int getSmallestDiskSize(int disk) {
        // The size must be at least 10 GB
        if (disk < LOWER_BOUND_DISK_GB)
            disk = LOWER_BOUND_DISK_GB;
        return disk;
    }
}