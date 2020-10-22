package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.compute.v1;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.models.GoogleCloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.compute.models.CreateComputeRequest;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.compute.models.CreateComputeResponse;
import static cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.compute.models.CreateComputeRequest.*;
import static cloud.fogbow.ras.core.plugins.interoperability.googlecloud.util.GoogleCloudConstants.*;
import cloud.fogbow.common.util.connectivity.cloud.googlecloud.GoogleCloudHttpClient;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.HardwareRequirements;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.ComputePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.network.v1.GoogleCloudNetworkPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.compute.models.GetComputeResponse;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.util.GoogleCloudConstants;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.util.GoogleCloudPluginUtils;
import cloud.fogbow.ras.core.plugins.interoperability.util.DefaultLaunchCommandGenerator;
import cloud.fogbow.ras.core.plugins.interoperability.util.LaunchCommandGenerator;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class GoogleCloudComputePlugin implements ComputePlugin<GoogleCloudUser> {
    private static final Logger LOGGER = Logger.getLogger(GoogleCloudNetworkPlugin.class);

    private Properties properties;
    private GoogleCloudHttpClient client;
    private LaunchCommandGenerator launchCommandGenerator;
    private String zone;

    public GoogleCloudComputePlugin(String confFilePath) throws FatalErrorException {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        this.launchCommandGenerator = new DefaultLaunchCommandGenerator();
        this.client = new GoogleCloudHttpClient();
        this.zone = this.properties.getProperty(ZONE_KEY_CONFIG);
    }

    @Override
    public String requestInstance(ComputeOrder computeOrder, GoogleCloudUser cloudUser) throws FogbowException {
        LOGGER.info(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER);
        String projectId = GoogleCloudPluginUtils.getProjectIdFrom(cloudUser);
        String endpoint = getComputeEndpoint(projectId);

        HardwareRequirements hardwareRequirements = findSmallestFlavor(computeOrder);
        String name = computeOrder.getName();
        String flavorId = getFlavorId(hardwareRequirements.getFlavorId());
        MetaData metaData = getMetaData(computeOrder);
        List<Disk> disks = getDisks(projectId, computeOrder.getImageId(), hardwareRequirements.getDisk());
        List<CreateComputeRequest.Network> networks = getNetworkIds(projectId, computeOrder);

        CreateComputeRequest request = getComputeResquestBody(name, flavorId, metaData, networks, disks);
        String body = request.toJson();

        String instanceId = doRequestInstance(endpoint, body, cloudUser);
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

        return computeInstance;
    }

    @Override
    public void deleteInstance(ComputeOrder computeOrder, GoogleCloudUser cloudUser) throws FogbowException {
        String instanceId = computeOrder.getInstanceId();
        LOGGER.info(String.format(Messages.Log.DELETING_INSTANCE_S, instanceId));

        String projectId = GoogleCloudPluginUtils.getProjectIdFrom(cloudUser);

        String endpoint = getComputeEndpoint(projectId);

        this.doDeleteRequest(endpoint, cloudUser);
    }

    @Override
    public boolean isReady(String instanceState) {
        return false;
    }

    @Override
    public boolean hasFailed(String instanceState) {
        return false;
    }

    private void setAllocationToOrder(ComputeOrder computeOrder, HardwareRequirements hardwareRequirements) {
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
        return null;
    }

    private void doDeleteRequest(String endpoint, GoogleCloudUser cloudUser) throws FogbowException {
        this.client.doDeleteRequest(endpoint, cloudUser);
    }

    private CreateComputeRequest getComputeResquestBody(String name, String flavorId, MetaData metaData,
                                                        List<CreateComputeRequest.Network> networks, List<Disk> disks) {

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
        String flavorId = getComputeResponse.getFlavorId();
        String status = getComputeResponse.getStatus();
        String faultMessage = getComputeResponse.getFaultMessage();

        List<GetComputeResponse.Network> addressesList = getComputeResponse.getAddresses();
        List<String> ipAddresses = new ArrayList<String>();
        for (GetComputeResponse.Network network : addressesList) {
            ipAddresses.add(network.getAddress());
        }

        return new ComputeInstance(instanceId, status, name, ipAddresses, faultMessage);
    }


    private List<CreateComputeRequest.Network> getNetworkIds(String projectId, ComputeOrder computeOrder) {
        List<CreateComputeRequest.Network> networks = new ArrayList<CreateComputeRequest.Network>();
        for (String networkId : computeOrder.getNetworkIds()) {
            addToNetworks(projectId, networks, networkId);
        }
        if (networks.isEmpty()) {
            addToNetworks(projectId, networks, GoogleCloudConstants.Network.DEFAULT_NETWORK_KEY);
        }
        return networks;
    }

    private void addToNetworks(String projectId, List<CreateComputeRequest.Network> networks, String network) {
        String networkId = getNetworkId(projectId, network);
        networks.add(new CreateComputeRequest.Network(networkId));
    }

    private List<Disk> getDisks(String projectId, String imageId, int diskSizeGb) {
        List<Disk> disks = new ArrayList<Disk>();
        Disk disk = getDisk(projectId, imageId, diskSizeGb);
        disks.add(disk);
        return disks;
    }

    private Disk getDisk(String projectId, String imageId, int diskSizeGb) {
        String imageSourceId = getImageId(imageId, projectId);
        CreateComputeRequest.InicialeParams initializeParams =
                new CreateComputeRequest.InicialeParams(imageSourceId, diskSizeGb);
        return new Disk(Compute.Disk.BOOT_DEFAULT_VALUE, initializeParams);
    }

    private MetaData getMetaData(ComputeOrder computeOrder) throws InternalServerErrorException {
        List<Item> items = new ArrayList<Item>();
        Item userData = getUserData(computeOrder);
        if (userData != null)
            items.add(userData);

        Item publicSSHKey = getPublicSSHKey(computeOrder.getPublicKey());
        if(publicSSHKey != null)
            items.add(publicSSHKey);

        return new MetaData(items);
    }

    private Item getPublicSSHKey(String publicKey) {
        return new Item(Compute.PUBLIC_SSH_KEY_JSON, publicKey);
    }

    private Item getUserData(ComputeOrder computeOrder) throws InternalServerErrorException {
        String userDataValue = this.launchCommandGenerator.createLaunchCommand(computeOrder);
        return new Item(Compute.USER_DATA_KEY_JSON, userDataValue);
    }

    private String getImageId(String imageId, String projectId) {
        return GoogleCloudPluginUtils.getProjectEndpoint(projectId) + GLOBAL_IMAGES_ENDPOINT + LINE_SEPARATOR
                + imageId;
    }

    private String getFlavorId(String flavorId) {
        return getZoneEndpoint() + LINE_SEPARATOR +  flavorId;
    }

    private String getNetworkId(String projectId, String networkId) {
        return GoogleCloudPluginUtils.getProjectEndpoint(projectId) + GLOBAL_NETWORKS_ENDPOINT + LINE_SEPARATOR
                + networkId;
    }

    private String getInstanceEndpoint(String projectId, String instanceId) {
        return getComputeEndpoint(projectId) + LINE_SEPARATOR + instanceId;
    }

    private String getZoneEndpoint() {
        return PATH_ZONE + LINE_SEPARATOR + this.zone;
    }

    private String getComputeEndpoint(String projectId) {
        return GoogleCloudPluginUtils.getProjectEndpoint(projectId) + getZoneEndpoint() +
                COMPUTE_ENDPOINT;
    }

    private HardwareRequirements findSmallestFlavor(ComputeOrder computeOrder) {
        int vCPU = getSmallestvCPU(computeOrder.getvCPU());
        int ramSize = getSmallestRamSize(computeOrder.getRam(), computeOrder.getvCPU());
        int diskSize = getSmallestDiskSize(computeOrder.getDisk());
        String flavorId = getFlavorId(vCPU, ramSize);
        return new HardwareRequirements(computeOrder.getName(), flavorId, vCPU, ramSize, diskSize);
    }

    private String getFlavorId(int vCPU, int ramSize) {
        return Compute.CUSTOM_FLAVOR_KEY + "-" + vCPU + "-" + ramSize;
    }

    private int getSmallestvCPU(int vCPU) {
        // The number of vCPUs must be 1 or an even number
        if (vCPU > 1 && vCPU % 2 != 0)
            vCPU++;
        return vCPU;
    }

    private int getSmallestRamSize(int ram, int vCPU) {
        // The size of RAM must be in MB and multiple of 256
        if (ram % 256 != 0) {
            int multiplePredecessor = (ram / 256) * 256;
            ram = multiplePredecessor + 256;
        }
        return ram;
    }

    private int getSmallestDiskSize(int disk) {
        // The size must be at least 10 GB
        if (disk < 10)
            disk = 10;
        return disk;
    }
}
