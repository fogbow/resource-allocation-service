package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpToFogbowExceptionMapper;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.NetworkSummary;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.ComputePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9.CloudStackPublicIpPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetAllDiskOfferingsRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetAllDiskOfferingsResponse;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetVolumeRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetVolumeResponse;
import cloud.fogbow.ras.core.plugins.interoperability.util.DefaultLaunchCommandGenerator;
import cloud.fogbow.ras.core.plugins.interoperability.util.LaunchCommandGenerator;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

public class CloudStackComputePlugin implements ComputePlugin<CloudStackUser> {
    private static final Logger LOGGER = Logger.getLogger(CloudStackComputePlugin.class);

    protected static final String CLOUDUSER_NULL_EXCEPTION_MSG =
            String.format(Messages.Error.IRREGULAR_VALUE_NULL_EXCEPTION_MSG, "Cloud User");

    private static final String EXPUNGE_ON_DESTROY_KEY_CONF = "expunge_on_destroy";
    private static final String CLOUDSTACK_URL_CONF = "cloudstack_api_url";
    protected static final String ZONE_ID_KEY_CONF = "zone_id";

    private static final String DEFAULT_EXPUNGE_ON_DEPLOY_VALUE = "true";
    private static final String DEFAULT_VOLUME_TYPE_VALUE = "ROOT";
    protected static final int UNKNOWN_DISK_VALUE = -1;

    protected static final String FOGBOW_TAG_SEPARATOR = ":";
    protected static final double GIGABYTE_IN_BYTES = Math.pow(1024, 3);
    protected static final int AMOUNT_INSTANCE = 1;

    private LaunchCommandGenerator launchCommandGenerator;
    private CloudStackHttpClient client;
    private String expungeOnDestroy;
    private String defaultNetworkId;
    private Properties properties;
    private String cloudStackUrl;
    private String zoneId;

    public CloudStackComputePlugin(String confFilePath) throws FatalErrorException {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        this.cloudStackUrl = this.properties.getProperty(CLOUDSTACK_URL_CONF);
        this.zoneId = this.properties.getProperty(ZONE_ID_KEY_CONF);
        this.expungeOnDestroy = this.properties.getProperty(
                EXPUNGE_ON_DESTROY_KEY_CONF, DEFAULT_EXPUNGE_ON_DEPLOY_VALUE);
        this.defaultNetworkId = this.properties.getProperty(CloudStackPublicIpPlugin.DEFAULT_NETWORK_ID_KEY);
        this.client = new CloudStackHttpClient();
        this.launchCommandGenerator = new DefaultLaunchCommandGenerator();
    }

    @VisibleForTesting
    CloudStackComputePlugin() {}

    @Override
    public boolean isReady(String cloudState) {
        return CloudStackStateMapper.map(ResourceType.COMPUTE, cloudState).equals(InstanceState.READY);
    }

    @Override
    public boolean hasFailed(String cloudState) {
        return CloudStackStateMapper.map(ResourceType.COMPUTE, cloudState).equals(InstanceState.FAILED);
    }

    @Override
    public String requestInstance(@NotNull ComputeOrder computeOrder, final CloudStackUser cloudUser)
            throws FogbowException {

        checkCloudUser(cloudUser);
        String templateId = getTemplateId(computeOrder);
        String userData = this.launchCommandGenerator.createLaunchCommand(computeOrder);
        String networksId = normalizeNetworksID(computeOrder);
        GetAllServiceOfferingsResponse.ServiceOffering serviceOffering =
                getServiceOffering(computeOrder, cloudUser);
        int disk = computeOrder.getDisk();
        GetAllDiskOfferingsResponse.DiskOffering diskOffering = getDiskOffering(disk, cloudUser);
        String instanceName = normalizeInstanceName(computeOrder.getName());

        DeployVirtualMachineRequest deployVirtualMachineRequest = new DeployVirtualMachineRequest.Builder()
                .serviceOfferingId(serviceOffering.getId())
                .templateId(templateId)
                .zoneId(this.zoneId)
                .name(instanceName)
                .diskOfferingId(diskOffering.getId())
                .userData(userData)
                .networksId(networksId)
                .build(this.cloudStackUrl);
        DeployVirtualMachineResponse deployVirtualMachineResponse = doRequestInstance(
                deployVirtualMachineRequest, cloudUser);

        updateComputeOrder(computeOrder, serviceOffering, diskOffering);
        return deployVirtualMachineResponse.getId();
    }

    @Override
    public ComputeInstance getInstance(@NotNull final ComputeOrder order,
                                       final CloudStackUser cloudUser)
            throws FogbowException {

        checkCloudUser(cloudUser);
        GetVirtualMachineRequest getVirtualMachineRequest = new GetVirtualMachineRequest.Builder()
                .id(order.getInstanceId())
                .build(this.cloudStackUrl);
        GetVirtualMachineResponse getVirtualMachineResponse = doGetInstance(getVirtualMachineRequest, cloudUser);
        return getVM(getVirtualMachineResponse, cloudUser);
    }

    @VisibleForTesting
    void doDeleteInstance(@NotNull DestroyVirtualMachineRequest destroyVirtualMachineRequest,
                          @NotNull final CloudStackUser cloudStackUser, String instanceId)
            throws FogbowException {

        URIBuilder uriRequest = destroyVirtualMachineRequest.getUriBuilder();
        String token = cloudStackUser.getToken();
        CloudStackUrlUtil.sign(uriRequest, token);

        try {
            doGet(uriRequest.toString(), cloudStackUser);
            LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE, instanceId, token));
        } catch (HttpResponseException e) {
            LOGGER.error(String.format(Messages.Error.UNABLE_TO_DELETE_INSTANCE, instanceId), e);
            CloudStackHttpToFogbowExceptionMapper.map(e);
        }
    }

    @Override
    public void deleteInstance(@NotNull final ComputeOrder order, final CloudStackUser cloudUser)
            throws FogbowException {

        checkCloudUser(cloudUser);
        DestroyVirtualMachineRequest request = new DestroyVirtualMachineRequest.Builder()
                .id(order.getInstanceId())
                .expunge(this.expungeOnDestroy)
                .build(this.cloudStackUrl);
        doDeleteInstance(request, cloudUser, order.getInstanceId());
    }

    @NotNull
    @VisibleForTesting
    GetAllServiceOfferingsResponse.ServiceOffering getServiceOffering(
            final ComputeOrder computeOrder, final CloudStackUser cloudUser) throws FogbowException {

        GetAllServiceOfferingsResponse serviceOfferingsResponse = getServiceOfferings(cloudUser);
        List<GetAllServiceOfferingsResponse.ServiceOffering> serviceOfferings = serviceOfferingsResponse.
                getServiceOfferings();

        if (serviceOfferings != null && !serviceOfferings.isEmpty()) {
            List<GetAllServiceOfferingsResponse.ServiceOffering> serviceOfferingsFiltered =
                    filterServicesOfferingByRequirements(serviceOfferings, computeOrder);
            for (GetAllServiceOfferingsResponse.ServiceOffering serviceOffering : serviceOfferingsFiltered) {
                if (serviceOffering.getCpuNumber() >= computeOrder.getvCPU() &&
                        serviceOffering.getMemory() >= computeOrder.getMemory()) {
                    return serviceOffering;
                }
            }
        }

        throw new NoAvailableResourcesException(
                Messages.Error.UNABLE_TO_COMPLETE_REQUEST_SERVICE_OFFERING_CLOUDSTACK);
    }

    @VisibleForTesting
    List<GetAllServiceOfferingsResponse.ServiceOffering> filterServicesOfferingByRequirements(
            @NotNull final List<GetAllServiceOfferingsResponse.ServiceOffering> serviceOfferings,
            @NotNull final ComputeOrder computeOrder) {

        List<GetAllServiceOfferingsResponse.ServiceOffering> serviceOfferingsFilted = serviceOfferings;
        Map<String, String> requirements = computeOrder.getRequirements();
        if (requirements == null || requirements.size() == 0) {
            return serviceOfferings;
        }

        for (Map.Entry<String, String> tag : requirements.entrySet()) {
            String tagFromRequirements = tag.getKey() + FOGBOW_TAG_SEPARATOR + tag.getValue();
            serviceOfferingsFilted = serviceOfferingsFilted.stream().filter(serviceOffering -> {
                String tagsServiceOffering = serviceOffering.getTags();
                boolean isMatchingWithRequirements = tagsServiceOffering != null &&
                        !tagsServiceOffering.isEmpty() &&
                        tagsServiceOffering.contains(tagFromRequirements);
                return isMatchingWithRequirements;
            }).collect(Collectors.toList());
        }

        return serviceOfferingsFilted;
    }

    @NotNull
    @VisibleForTesting
    GetAllServiceOfferingsResponse getServiceOfferings(@NotNull final CloudStackUser cloudStackUser)
            throws FogbowException {

        GetAllServiceOfferingsRequest getAllServiceOfferingsRequest = new GetAllServiceOfferingsRequest.Builder()
                .build(this.cloudStackUrl);
        URIBuilder uriRequest = getAllServiceOfferingsRequest.getUriBuilder();
        String token = cloudStackUser.getToken();
        CloudStackUrlUtil.sign(uriRequest, token);

        try {
            String jsonResponse = doGet(uriRequest.toString(), cloudStackUser);
            return GetAllServiceOfferingsResponse.fromJson(jsonResponse);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowExceptionMapper.map(e);
            return null;
        }
    }

    @NotNull
    @VisibleForTesting
    GetAllDiskOfferingsResponse.DiskOffering getDiskOffering(int diskSize,
                                                             @NotNull final CloudStackUser cloudUser)
            throws FogbowException {

        GetAllDiskOfferingsResponse diskOfferingsResponse = getDiskOfferings(cloudUser);
        List<GetAllDiskOfferingsResponse.DiskOffering> diskOfferings = diskOfferingsResponse.getDiskOfferings();

        if (diskOfferings != null && !diskOfferings.isEmpty()) {
            for (GetAllDiskOfferingsResponse.DiskOffering diskOffering : diskOfferings) {
                if (diskOffering.getDiskSize() >= diskSize) {
                    return diskOffering;
                }
            }
        }

        throw new NoAvailableResourcesException(
                Messages.Error.UNABLE_TO_COMPLETE_REQUEST_DISK_OFFERING_CLOUDSTACK);
    }

    @NotNull
    @VisibleForTesting
    GetAllDiskOfferingsResponse getDiskOfferings(@NotNull final CloudStackUser cloudUser)
            throws FogbowException {

        GetAllDiskOfferingsRequest getAllDiskOfferingsRequest = new GetAllDiskOfferingsRequest.Builder()
                .build(this.cloudStackUrl);
        URIBuilder uriRequest = getAllDiskOfferingsRequest.getUriBuilder();
        String token = cloudUser.getToken();
        CloudStackUrlUtil.sign(uriRequest, token);

        try {
            String jsonResponse = doGet(uriRequest.toString(), cloudUser);
            return GetAllDiskOfferingsResponse.fromJson(jsonResponse);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowExceptionMapper.map(e);
            return null;
        }
    }

    @NotNull
    @VisibleForTesting
    String normalizeNetworksID(@NotNull final ComputeOrder computeOrder) {
        List<String> networks = new ArrayList<>();
        networks.add(this.defaultNetworkId);
        List<String> userDefinedNetworks = computeOrder.getNetworkIds();
        if (!userDefinedNetworks.isEmpty()) {
            networks.addAll(userDefinedNetworks);
        }
        return StringUtils.join(networks, ",");
    }

    @NotNull
    @VisibleForTesting
    String normalizeInstanceName(String instanceName) {
        return instanceName != null ? instanceName
                : SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + getRandomUUID();
    }

    @NotNull
    @VisibleForTesting
    ComputeInstance getComputeInstance(@NotNull GetVirtualMachineResponse.VirtualMachine vm,
                                       @NotNull final CloudStackUser cloudUser) {
        String instanceId = vm.getId();
        String hostName = vm.getName();
        int vcpusCount = vm.getCpuNumber();
        int memory = vm.getMemory();

        int disk = UNKNOWN_DISK_VALUE;
        try {
            disk = getVirtualMachineDiskSize(instanceId, cloudUser);
        } catch (FogbowException e) {
            LOGGER.warn(String.format(Messages.Warn.UNABLE_TO_RETRIEVE_ROOT_VOLUME, vm.getId()), e);
        }

        String cloudStackState = vm.getState();
        GetVirtualMachineResponse.Nic[] nics = vm.getNic();
        List<String> addresses = new ArrayList<>();

        for (GetVirtualMachineResponse.Nic nic : nics) {
            addresses.add(nic.getIpAddress());
        }

        ComputeInstance computeInstance = new ComputeInstance(
                instanceId, cloudStackState, hostName, vcpusCount, memory, disk, addresses);

        // The default network is always included in the order by the CloudStack plugin, thus it should be added
        // in the map of networks in the ComputeInstance by the plugin. The remaining networks passed by the user
        // are appended by the LocalCloudConnector.
        List<NetworkSummary> computeNetworks = new ArrayList<>();
        computeNetworks.add(new NetworkSummary(this.defaultNetworkId, SystemConstants.DEFAULT_NETWORK_NAME));
        computeInstance.setNetworks(computeNetworks);
        return computeInstance;
    }

    @VisibleForTesting
    int getVirtualMachineDiskSize(String virtualMachineId, @NotNull final CloudStackUser cloudUser)
            throws FogbowException {

        GetVolumeRequest getVolumeRequest = new GetVolumeRequest.Builder()
                .virtualMachineId(virtualMachineId)
                .type(DEFAULT_VOLUME_TYPE_VALUE)
                .build(this.cloudStackUrl);
        URIBuilder uriRequest = getVolumeRequest.getUriBuilder();
        String token = cloudUser.getToken();
        CloudStackUrlUtil.sign(uriRequest, token);

        GetVolumeResponse volumeResponse = null;
        try {
            String jsonResponse = doGet(uriRequest.toString(), cloudUser);
            volumeResponse = GetVolumeResponse.fromJson(jsonResponse);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowExceptionMapper.map(e);
        }

        List<GetVolumeResponse.Volume> volumes = volumeResponse.getVolumes();
        if (volumes != null && !volumes.isEmpty()) {
            GetVolumeResponse.Volume firstVolume = volumes.get(0);
            long sizeInBytes = firstVolume.getSize();
            return convertBytesToGigabyte(sizeInBytes);
        } else {
            throw new InstanceNotFoundException();
        }
    }

    @NotNull
    @VisibleForTesting
    String doGet(String url, @NotNull final CloudStackUser cloudUser) throws HttpResponseException {
        try {
            return this.client.doGetRequest(url, cloudUser);
        } catch (FogbowException e) {
            throw  new HttpResponseException(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @VisibleForTesting
    int convertBytesToGigabyte(long bytes) {
        return (int) (bytes / GIGABYTE_IN_BYTES);
    }

    @VisibleForTesting
    void updateComputeOrder(@NotNull ComputeOrder computeOrder,
                                    @NotNull GetAllServiceOfferingsResponse.ServiceOffering serviceOffering,
                                    @NotNull GetAllDiskOfferingsResponse.DiskOffering diskOffering) {

        synchronized (computeOrder) {
            ComputeAllocation actualAllocation = new ComputeAllocation(
                    serviceOffering.getCpuNumber(),
                    serviceOffering.getMemory(),
                    AMOUNT_INSTANCE,
                    diskOffering.getDiskSize());
            computeOrder.setActualAllocation(actualAllocation);
        }
    }

    @NotNull
    @VisibleForTesting
    DeployVirtualMachineResponse doRequestInstance(
            @NotNull DeployVirtualMachineRequest deployVirtualMachineRequest,
            @NotNull final CloudStackUser cloudUser)
            throws FogbowException {

        URIBuilder uriRequest = deployVirtualMachineRequest.getUriBuilder();
        String token = cloudUser.getToken();
        CloudStackUrlUtil.sign(uriRequest, token);

        try {
            String jsonResponse = doGet(uriRequest.toString(), cloudUser);
            return DeployVirtualMachineResponse.fromJson(jsonResponse);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowExceptionMapper.map(e);
            return null;
        }
    }

    private void checkCloudUser(final CloudStackUser cloudUser) throws FogbowException {
        if (cloudUser == null) {
            throw new FogbowException(CLOUDUSER_NULL_EXCEPTION_MSG);
        }
    }

    @NotNull
    @VisibleForTesting
    GetVirtualMachineResponse doGetInstance(@NotNull GetVirtualMachineRequest getVirtualMachineRequest,
                                            @NotNull final CloudStackUser cloudStackUser)
            throws FogbowException {

        URIBuilder uriRequest = getVirtualMachineRequest.getUriBuilder();
        CloudStackUrlUtil.sign(uriRequest, cloudStackUser.getToken());
        try {
            String jsonResponse = doGet(uriRequest.toString(), cloudStackUser);
            return GetVirtualMachineResponse.fromJson(jsonResponse);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowExceptionMapper.map(e);
            return null;
        }
    }

    @NotNull
    @VisibleForTesting
    ComputeInstance getVM(@NotNull GetVirtualMachineResponse getVirtualMachineResponse,
                          @NotNull final CloudStackUser cloudStackUser)
            throws InstanceNotFoundException {

        List<GetVirtualMachineResponse.VirtualMachine> vms = getVirtualMachineResponse.getVirtualMachines();
        if (vms != null && !vms.isEmpty()) {
            GetVirtualMachineResponse.VirtualMachine firstVirtualMachine = vms.get(0);
            return getComputeInstance(firstVirtualMachine, cloudStackUser);
        } else {
            throw new InstanceNotFoundException();
        }
    }

    private String getTemplateId(@NotNull final ComputeOrder computeOrder) throws InvalidParameterException {
        String templateId = computeOrder.getImageId();
        if (templateId == null) {
            throw new InvalidParameterException(Messages.Error.UNABLE_TO_COMPLETE_REQUEST_CLOUDSTACK);
        }
        return templateId;
    }

    private String getRandomUUID() {
        return UUID.randomUUID().toString();
    }

    @VisibleForTesting
    void setLaunchCommandGenerator(LaunchCommandGenerator commandGenerator) {
        this.launchCommandGenerator = commandGenerator;
    }

    @VisibleForTesting
    void setClient(CloudStackHttpClient client) {
        this.client = client;
    }

    @VisibleForTesting
    String getCloudStackUrl() {
        return cloudStackUrl;
    }
}
