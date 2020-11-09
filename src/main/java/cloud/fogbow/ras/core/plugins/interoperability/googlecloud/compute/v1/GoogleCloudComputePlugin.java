package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.compute.v1;

import cloud.fogbow.common.constants.GoogleCloudConstants;
import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.models.GoogleCloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.NetworkSummary;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.compute.models.CreateComputeRequest;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.compute.models.CreateComputeResponse;
import cloud.fogbow.common.util.connectivity.cloud.googlecloud.GoogleCloudHttpClient;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.HardwareRequirements;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.ComputePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.compute.models.GetComputeResponse;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.compute.models.GetFirewallRulesResponse;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.util.GoogleCloudPluginUtils;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.util.GoogleCloudStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.util.DefaultLaunchCommandGenerator;
import cloud.fogbow.ras.core.plugins.interoperability.util.LaunchCommandGenerator;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class GoogleCloudComputePlugin implements ComputePlugin<GoogleCloudUser> {
    private static final Logger LOGGER = Logger.getLogger(GoogleCloudComputePlugin.class);

    private static int RAM_MULTIPLE_NUMBER = 256;
    private static double LOWER_BOUND_RAM_PER_VCPU_GB = 0.9;
    private static double UPPER_BOUND_RAM_PER_VCPU_GB = 6.4;
    private static int LOWER_BOUND_DISK_GB = 10;
    private static int ONE_GB_IN_MB = 1024;

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
        List<CreateComputeRequest.Network> networks = getNetworkIds(projectId, computeOrder);

        CreateComputeRequest request = getComputeRequestBody(name, flavorId, metaData, networks, disks);
        String body = request.toJson();

        String instanceId = doRequestInstance(endpoint, body, cloudUser);
        deletePrePopulatedDefaultNetworkSecurityRules(projectId, cloudUser);
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
    public void pauseInstance(ComputeOrder order, GoogleCloudUser cloudUser) throws FogbowException {
        // ToDo: implement
    }

    @Override
    public void hibernateInstance(ComputeOrder order, GoogleCloudUser cloudUser) throws FogbowException {
        // ToDo: implement
    }

    @Override
    public void resumeInstance(ComputeOrder order, GoogleCloudUser cloudUser) throws FogbowException {
        // ToDo: implement
    }

    @Override
    public boolean isReady(String instanceState) {
        return GoogleCloudStateMapper.map(ResourceType.COMPUTE, instanceState).equals(InstanceState.READY);
    }

    @Override
    public boolean hasFailed(String instanceState) {
        return GoogleCloudStateMapper.map(ResourceType.COMPUTE, instanceState).equals(InstanceState.FAILED);
    }

    private void setAllocationToOrder(ComputeOrder computeOrder, HardwareRequirements hardwareRequirements) {
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

    private String doRequestInstance(String endpoint, String body, GoogleCloudUser cloudUser)
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

    private ComputeInstance doGetInstance(String endpoint, GoogleCloudUser cloudUser) throws FogbowException {
        String responseJson = this.client.doGetRequest(endpoint, cloudUser);

        ComputeInstance computeInstance = getInstanceFromJson(responseJson);

        return computeInstance;
    }

    private List<NetworkSummary> getComputeNetworks(String projectId) {
        String defaultNetworkId = getNetworkId(projectId, GoogleCloudConstants.Network.DEFAULT_NETWORK_NAME);
        List<NetworkSummary> computeNetworks = new ArrayList<>();
        computeNetworks.add(new NetworkSummary(defaultNetworkId, GoogleCloudConstants.Network.DEFAULT_NETWORK_NAME));
        return computeNetworks;
    }

    private CreateComputeRequest getComputeRequestBody(String name, String flavorId, CreateComputeRequest.MetaData metaData,
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

    private ComputeInstance getInstanceFromJson(String responseJson) {
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

    private void deletePrePopulatedDefaultNetworkSecurityRules(String projectId, GoogleCloudUser cloudUser) throws FogbowException {
        List<GetFirewallRulesResponse.FirewallRule> securityRules = getDefaultNetworkSecurityRules(projectId, cloudUser);
        if (securityRules != null) {
            for (GetFirewallRulesResponse.FirewallRule securityRule : securityRules) {
                if (isAPrePopulatedSecurityRule(securityRule)) {
                    deleteSecurityRule(securityRule.getId(), projectId, cloudUser);
                }
            }
        }
    }

    private List<GetFirewallRulesResponse.FirewallRule> getDefaultNetworkSecurityRules(String projectId, GoogleCloudUser cloudUser) throws FogbowException {
        String endpoint = getFirewallsEndpoint(projectId);
        String responseJson = this.client.doGetRequest(endpoint, cloudUser);
        GetFirewallRulesResponse firewallRuleResponse = GetFirewallRulesResponse.fromJson(responseJson);

        return firewallRuleResponse.getSecurityGroupRules();
    }

    private void deleteSecurityRule(String securityRuleId, String projectId, GoogleCloudUser cloudUser) throws FogbowException {
        String endpoint = getFirewallsEndpoint(projectId) + GoogleCloudConstants.ENDPOINT_SEPARATOR + securityRuleId;
        this.client.doDeleteRequest(endpoint, cloudUser);
    }

    private boolean isAPrePopulatedSecurityRule(GetFirewallRulesResponse.FirewallRule securityRule) {
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

    private List<CreateComputeRequest.Network> getNetworkIds(String projectId, ComputeOrder computeOrder) {
        List<CreateComputeRequest.Network> networks = new ArrayList<CreateComputeRequest.Network>();
        // Add default network
        addToNetworks(projectId, networks, GoogleCloudConstants.Network.DEFAULT_NETWORK_NAME);
        for (String networkId : computeOrder.getNetworkIds()) {
            addToNetworks(projectId, networks, networkId);
        }
        return networks;
    }

    private void addToNetworks(String projectId, List<CreateComputeRequest.Network> networks, String networkId) {
        String netword = getNetworkId(projectId, networkId);
        networks.add(new CreateComputeRequest.Network(netword));
    }

    private List<CreateComputeRequest.Disk> getDisks(String projectId, String imageId, int diskSizeGb) {
        List<CreateComputeRequest.Disk> disks = new ArrayList<CreateComputeRequest.Disk>();
        CreateComputeRequest.Disk disk = getDisk(projectId, imageId, diskSizeGb);
        disks.add(disk);
        return disks;
    }

    private CreateComputeRequest.Disk getDisk(String projectId, String imageId, int diskSizeGb) {
        String imageSourceId = getImageId(imageId, projectId);
        CreateComputeRequest.InicialeParams initializeParams =
                new CreateComputeRequest.InicialeParams(imageSourceId, diskSizeGb);
        return new CreateComputeRequest.Disk(GoogleCloudConstants.Compute.Disk.BOOT_DEFAULT_VALUE,
                GoogleCloudConstants.Compute.Disk.AUTO_DELETE_DEFAULT_VALUE,
                initializeParams);
    }

    private CreateComputeRequest.MetaData getMetaData(ComputeOrder computeOrder) throws InternalServerErrorException {
        List<CreateComputeRequest.Item> items = new ArrayList<CreateComputeRequest.Item>();
        CreateComputeRequest.Item userData = getUserData(computeOrder);
        if (userData != null)
            items.add(userData);

        CreateComputeRequest.Item publicSSHKey = getPublicSSHKey(computeOrder.getPublicKey());
        if(publicSSHKey != null)
            items.add(publicSSHKey);

        return new CreateComputeRequest.MetaData(items);
    }

    private CreateComputeRequest.Item getPublicSSHKey(String publicKey) {
        return new CreateComputeRequest.Item(GoogleCloudConstants.Compute.PUBLIC_SSH_KEY_JSON, publicKey);
    }

    private CreateComputeRequest.Item getUserData(ComputeOrder computeOrder) throws InternalServerErrorException {

        String userDataValue = this.launchCommandGenerator.createLaunchCommand(computeOrder);
        return new CreateComputeRequest.Item(GoogleCloudConstants.Compute.USER_DATA_KEY_JSON, userDataValue);
    }

    private String getImageId(String imageId, String projectId) {
        return GoogleCloudPluginUtils.getProjectEndpoint(projectId)
                + getPathWithId(GoogleCloudConstants.GLOBAL_IMAGES_ENDPOINT, imageId);
    }

    private String getFlavorId(String flavorId) {
        return getZoneEndpoint() + getPathWithId(GoogleCloudConstants.FLAVOR_ENDPOINT, flavorId);
    }

    private String getNetworkId(String projectId, String networkId) {
        return GoogleCloudPluginUtils.getProjectEndpoint(projectId)
                + getPathWithId(GoogleCloudConstants.GLOBAL_NETWORKS_ENDPOINT, networkId);
    }

    private String getPathWithId(String path, String id) {
        return path + GoogleCloudConstants.ENDPOINT_SEPARATOR + id;
    }

    private String getInstanceEndpoint(String projectId, String instanceId) {
        String computePath = getComputeEndpoint(projectId);
        return getPathWithId(computePath, instanceId);
    }

    private String getZoneEndpoint() {
        return getPathWithId(GoogleCloudConstants.ZONES_ENDPOINT, this.zone);
    }

    private String getComputeEndpoint(String projectId) {
        return getBaseUrl() + GoogleCloudPluginUtils.getProjectEndpoint(projectId) + getZoneEndpoint() +
                GoogleCloudConstants.INSTANCES_ENDPOINT;
    }

    private String getBaseUrl() {
        return GoogleCloudConstants.BASE_COMPUTE_API_URL + GoogleCloudConstants.COMPUTE_ENGINE_V1_ENDPOINT;
    }

    private String getFirewallsEndpoint(String projectId) {
        return getBaseUrl() + GoogleCloudPluginUtils.getProjectEndpoint(projectId) + GoogleCloudConstants.GLOBAL_FIREWALL_ENDPOINT;
    }

    private HardwareRequirements findSmallestFlavor(ComputeOrder computeOrder) {
        int vCPU = getSmallestvCPU(computeOrder.getvCPU());
        int ramSize = getSmallestRamSize(computeOrder.getRam(), computeOrder.getvCPU());
        int diskSize = getSmallestDiskSize(computeOrder.getDisk());
        String flavorId = getFlavorId(vCPU, ramSize);
        return new HardwareRequirements(computeOrder.getName(), flavorId, vCPU, ramSize, diskSize);
    }

    private String getFlavorId(int vCPU, int ramSize) {
        return GoogleCloudConstants.Compute.CUSTOM_FLAVOR + "-" + vCPU + "-" + ramSize;
    }

    private int getSmallestvCPU(int vCPU) {
        // The number of vCPUs must be 1 or an even number
        if (vCPU > 1 && vCPU % 2 != 0)
            vCPU--;
        return vCPU;
    }

    private int getSmallestRamSize(int ram, int vCPU) {
        ram = getSmallestMultipleRamSize(ram);
        ram = getSmallestRamSizePervCpu(ram, vCPU);
        return ram;
    }

    private int getSmallestMultipleRamSize(int ram) {
        // The size of RAM must be in MB and multiple of 256
        return (ram / RAM_MULTIPLE_NUMBER) * RAM_MULTIPLE_NUMBER;
    }

    private int getSmallestRamSizePervCpu(int ram, int vCPU) {
        // Memory per vCPU must be between 0.9 GB and 6.5 GB
        int betweenBoundsRamSizeInMb = 0;
        double memoryPervCPU = mbToGb(ram) / vCPU;
        if (memoryPervCPU <= LOWER_BOUND_RAM_PER_VCPU_GB)  {
            betweenBoundsRamSizeInMb = gbToMb(LOWER_BOUND_RAM_PER_VCPU_GB + 0.1) * vCPU;
        } else if (memoryPervCPU >= UPPER_BOUND_RAM_PER_VCPU_GB) {
            betweenBoundsRamSizeInMb = gbToMb(UPPER_BOUND_RAM_PER_VCPU_GB - 0.1) * vCPU;
        }
        if (betweenBoundsRamSizeInMb > 0) {
            ram = getSmallestMultipleRamSize(betweenBoundsRamSizeInMb);
        }
        // Ram size returned is between the boundaries and multiple of 256
        return ram;
    }

    private int getSmallestDiskSize(int disk) {
        // The size must be at least 10 GB
        if (disk < LOWER_BOUND_DISK_GB)
            disk = LOWER_BOUND_DISK_GB;
        return disk;
    }

    private int gbToMb(double Gb) {
        return (int) (Gb * ONE_GB_IN_MB);
    }

    private double mbToGb(int Mb) {
        return ((double) Mb / ONE_GB_IN_MB);
    }
}
