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
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackStateMapper;
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

    protected static final String EXPUNGE_ON_DESTROY_KEY_CONF = "expunge_on_destroy";
    protected static final String CLOUDSTACK_URL_CONF = "cloudstack_api_url";
    protected static final String ZONE_ID_KEY_CONF = "zone_id";

    private static final String DEFAULT_EXPUNGE_ON_DEPLOY_VALUE = "true";
    protected static final String DEFAULT_VOLUME_TYPE_VALUE = "ROOT";
    protected static final int UNKNOWN_DISK_VALUE = -1;

    protected static final String FOGBOW_TAG_SEPARATOR = ":";
    protected static final double GIGABYTE_IN_BYTES = Math.pow(1024, 3);
    protected static final int AMOUNT_INSTANCE = 1;

    private LaunchCommandGenerator launchCommandGenerator;
    private CloudStackHttpClient client;
    private String expungeOnDestroy;
    private String defaultNetworkId;
    private String cloudStackUrl;
    private String zoneId;

    public CloudStackComputePlugin(String confFilePath) throws FatalErrorException {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.cloudStackUrl = properties.getProperty(CLOUDSTACK_URL_CONF);
        this.zoneId = properties.getProperty(ZONE_ID_KEY_CONF);
        this.expungeOnDestroy = properties.getProperty(
                EXPUNGE_ON_DESTROY_KEY_CONF, DEFAULT_EXPUNGE_ON_DEPLOY_VALUE);
        this.defaultNetworkId = properties.getProperty(CloudStackCloudUtils.DEFAULT_NETWORK_ID_KEY);
        this.client = new CloudStackHttpClient();
        this.launchCommandGenerator = new DefaultLaunchCommandGenerator();
    }

    @Override
    public boolean isReady(String cloudState) {
        return CloudStackStateMapper.map(ResourceType.COMPUTE, cloudState).equals(InstanceState.READY);
    }

    @Override
    public boolean hasFailed(String cloudState) {
        return CloudStackStateMapper.map(ResourceType.COMPUTE, cloudState).equals(InstanceState.FAILED);
    }

    @Override
    public String requestInstance(@NotNull ComputeOrder computeOrder, @NotNull CloudStackUser cloudUser)
            throws FogbowException {

        String templateId = getTemplateId(computeOrder);
        String userData = this.launchCommandGenerator.createLaunchCommand(computeOrder);
        String networksId = normalizeNetworksID(computeOrder);
        GetAllServiceOfferingsResponse.ServiceOffering serviceOffering =
                getServiceOffering(computeOrder, cloudUser);
        GetAllDiskOfferingsResponse.DiskOffering diskOffering = getDiskOffering(computeOrder, cloudUser);
        String instanceName = normalizeInstanceName(computeOrder.getName());

        DeployVirtualMachineRequest request = new DeployVirtualMachineRequest.Builder()
                .serviceOfferingId(serviceOffering.getId())
                .templateId(templateId)
                .zoneId(this.zoneId)
                .name(instanceName)
                .diskOfferingId(diskOffering.getId())
                .userData(userData)
                .networksId(networksId)
                .build(this.cloudStackUrl);

        return doRequestInstance(request,
                serviceOffering, diskOffering, computeOrder, cloudUser);
    }

    @Override
    public ComputeInstance getInstance(@NotNull ComputeOrder order,
                                       @NotNull CloudStackUser cloudUser)
            throws FogbowException {

        GetVirtualMachineRequest request = new GetVirtualMachineRequest.Builder()
                .id(order.getInstanceId())
                .build(this.cloudStackUrl);

        return doGetInstance(request, cloudUser);
    }

    @Override
    public void deleteInstance(@NotNull ComputeOrder order, @NotNull CloudStackUser cloudUser)
            throws FogbowException {

        DestroyVirtualMachineRequest request = new DestroyVirtualMachineRequest.Builder()
                .id(order.getInstanceId())
                .expunge(this.expungeOnDestroy)
                .build(this.cloudStackUrl);

        doDeleteInstance(request, cloudUser, order.getInstanceId());
    }

    @NotNull
    @VisibleForTesting
    ComputeInstance doGetInstance(@NotNull GetVirtualMachineRequest request,
                                  @NotNull CloudStackUser cloudStackUser) throws FogbowException {

        GetVirtualMachineResponse response = requestGetVirtualMachine(request, cloudStackUser);
        return buildComputeInstance(response, cloudStackUser);
    }

    @NotNull
    @VisibleForTesting
    String doRequestInstance(@NotNull DeployVirtualMachineRequest request,
                             @NotNull GetAllServiceOfferingsResponse.ServiceOffering serviceOffering,
                             @NotNull GetAllDiskOfferingsResponse.DiskOffering diskOffering,
                             @NotNull ComputeOrder computeOrder,
                             @NotNull CloudStackUser cloudUser) throws FogbowException {

        DeployVirtualMachineResponse response = requestDeployVirtualMachine(request, cloudUser);
        updateComputeOrder(computeOrder, serviceOffering, diskOffering);
        return response.getId();
    }

    @VisibleForTesting
    void doDeleteInstance(@NotNull DestroyVirtualMachineRequest request,
                          @NotNull CloudStackUser cloudStackUser, String instanceId)
            throws FogbowException {

        URIBuilder uriRequest = request.getUriBuilder();
        String token = cloudStackUser.getToken();
        CloudStackUrlUtil.sign(uriRequest, token);

        try {
            doGet(uriRequest.toString(), cloudStackUser);
            LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE, instanceId, token));
        } catch (HttpResponseException e) {
            LOGGER.error(String.format(Messages.Error.UNABLE_TO_DELETE_INSTANCE, instanceId), e);
            throw CloudStackHttpToFogbowExceptionMapper.get(e);
        }
    }

    @NotNull
    @VisibleForTesting
    GetAllServiceOfferingsResponse.ServiceOffering getServiceOffering(
            @NotNull ComputeOrder computeOrder, @NotNull CloudStackUser cloudUser)
            throws FogbowException {

        GetAllServiceOfferingsResponse response = getServiceOfferings(cloudUser);
        List<GetAllServiceOfferingsResponse.ServiceOffering> serviceOfferings = response.
                getServiceOfferings();

        if (!serviceOfferings.isEmpty()) {
            List<GetAllServiceOfferingsResponse.ServiceOffering> serviceOfferingsFiltered =
                    filterServicesOfferingByRequirements(serviceOfferings, computeOrder);
            for (GetAllServiceOfferingsResponse.ServiceOffering serviceOffering : serviceOfferingsFiltered) {
                if (serviceOffering.getCpuNumber() >= computeOrder.getvCPU() &&
                        serviceOffering.getMemory() >= computeOrder.getRam()) {
                    return serviceOffering;
                }
            }
        }

        throw new NoAvailableResourcesException(
                Messages.Error.UNABLE_TO_COMPLETE_REQUEST_SERVICE_OFFERING_CLOUDSTACK);
    }

    @VisibleForTesting
    List<GetAllServiceOfferingsResponse.ServiceOffering> filterServicesOfferingByRequirements(
            @NotNull List<GetAllServiceOfferingsResponse.ServiceOffering> serviceOfferings,
            @NotNull ComputeOrder computeOrder) {

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
    GetAllServiceOfferingsResponse getServiceOfferings(@NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        GetAllServiceOfferingsRequest request = new GetAllServiceOfferingsRequest.Builder()
                .build(this.cloudStackUrl);
        URIBuilder uriRequest = request.getUriBuilder();
        String token = cloudStackUser.getToken();
        CloudStackUrlUtil.sign(uriRequest, token);

        try {
            String jsonResponse = doGet(uriRequest.toString(), cloudStackUser);
            return GetAllServiceOfferingsResponse.fromJson(jsonResponse);
        } catch (HttpResponseException e) {
            throw CloudStackHttpToFogbowExceptionMapper.get(e);
        }
    }

    @NotNull
    @VisibleForTesting
    GetAllDiskOfferingsResponse.DiskOffering getDiskOffering(@NotNull ComputeOrder computeOrder,
                                                             @NotNull CloudStackUser cloudUser)
            throws FogbowException {

        GetAllDiskOfferingsResponse diskOfferingsResponse = getDiskOfferings(cloudUser);
        List<GetAllDiskOfferingsResponse.DiskOffering> diskOfferings = diskOfferingsResponse.getDiskOfferings();

        int diskSize = computeOrder.getDisk();
        if (!diskOfferings.isEmpty()) {
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
    GetAllDiskOfferingsResponse getDiskOfferings(@NotNull CloudStackUser cloudUser)
            throws FogbowException {

        GetAllDiskOfferingsRequest request = new GetAllDiskOfferingsRequest.Builder()
                .build(this.cloudStackUrl);
        URIBuilder uriRequest = request.getUriBuilder();
        String token = cloudUser.getToken();
        CloudStackUrlUtil.sign(uriRequest, token);

        try {
            String jsonResponse = doGet(uriRequest.toString(), cloudUser);
            return GetAllDiskOfferingsResponse.fromJson(jsonResponse);
        } catch (HttpResponseException e) {
            throw CloudStackHttpToFogbowExceptionMapper.get(e);
        }
    }

    @NotNull
    @VisibleForTesting
    String normalizeNetworksID(@NotNull ComputeOrder computeOrder) {
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
    ComputeInstance createComputeInstance(@NotNull GetVirtualMachineResponse.VirtualMachine virtualMachine,
                                          int disk) {

        String instanceId = virtualMachine.getId();
        String hostName = virtualMachine.getName();
        int vcpusCount = virtualMachine.getCpuNumber();
        int memory = virtualMachine.getMemory();
        String cloudStackState = virtualMachine.getState();

        GetVirtualMachineResponse.Nic[] nics = virtualMachine.getNic();
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
    int getVirtualMachineDiskSize(String virtualMachineId, @NotNull CloudStackUser cloudUser) {
        try {
            GetVolumeRequest request = new GetVolumeRequest.Builder()
                    .virtualMachineId(virtualMachineId)
                    .type(DEFAULT_VOLUME_TYPE_VALUE)
                    .build(this.cloudStackUrl);

            URIBuilder uriRequest = request.getUriBuilder();
            String token = cloudUser.getToken();
            CloudStackUrlUtil.sign(uriRequest, token);
            String jsonResponse = doGet(uriRequest.toString(), cloudUser);
            GetVolumeResponse response = GetVolumeResponse.fromJson(jsonResponse);

            List<GetVolumeResponse.Volume> volumes = response.getVolumes();
            if (!volumes.isEmpty()) {
                GetVolumeResponse.Volume firstVolume = volumes.get(0);
                long sizeInBytes = firstVolume.getSize();
                return convertBytesToGigabyte(sizeInBytes);
            }
        } catch (Exception e) {
            LOGGER.debug(Messages.Error.ERROR_WHILE_GETTING_DISK_SIZE, e);
        }
        LOGGER.warn(String.format(Messages.Warn.UNABLE_TO_RETRIEVE_ROOT_VOLUME, virtualMachineId));
        return UNKNOWN_DISK_VALUE;
    }

    @NotNull
    @VisibleForTesting
    String doGet(String url, @NotNull CloudStackUser cloudUser) throws HttpResponseException {
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
                    AMOUNT_INSTANCE, serviceOffering.getCpuNumber(),
                    serviceOffering.getMemory(),
                    diskOffering.getDiskSize());
            computeOrder.setActualAllocation(actualAllocation);
        }
    }

    @NotNull
    @VisibleForTesting
    DeployVirtualMachineResponse requestDeployVirtualMachine(
            @NotNull DeployVirtualMachineRequest request,
            @NotNull CloudStackUser cloudUser)
            throws FogbowException {

        URIBuilder uriRequest = request.getUriBuilder();
        String token = cloudUser.getToken();
        CloudStackUrlUtil.sign(uriRequest, token);

        try {
            String jsonResponse = doGet(uriRequest.toString(), cloudUser);
            return DeployVirtualMachineResponse.fromJson(jsonResponse);
        } catch (HttpResponseException e) {
            throw CloudStackHttpToFogbowExceptionMapper.get(e);
        }
    }

    @NotNull
    @VisibleForTesting
    GetVirtualMachineResponse requestGetVirtualMachine(@NotNull GetVirtualMachineRequest request,
                                                       @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        URIBuilder uriRequest = request.getUriBuilder();
        String token = cloudStackUser.getToken();
        CloudStackUrlUtil.sign(uriRequest, token);

        try {
            String jsonResponse = doGet(uriRequest.toString(), cloudStackUser);
            return GetVirtualMachineResponse.fromJson(jsonResponse);
        } catch (HttpResponseException e) {
            throw CloudStackHttpToFogbowExceptionMapper.get(e);
        }
    }

    @NotNull
    @VisibleForTesting
    ComputeInstance buildComputeInstance(@NotNull GetVirtualMachineResponse response,
                                         @NotNull CloudStackUser cloudStackUser)
            throws InstanceNotFoundException {

        List<GetVirtualMachineResponse.VirtualMachine> virtualMachines = response.getVirtualMachines();
        if (!virtualMachines.isEmpty()) {
            GetVirtualMachineResponse.VirtualMachine virtualMachine = virtualMachines.get(0);
            int disk = getVirtualMachineDiskSize(virtualMachine.getId(), cloudStackUser);
            return createComputeInstance(virtualMachine, disk);
        } else {
            throw new InstanceNotFoundException();
        }
    }

    @NotNull
    @VisibleForTesting
    String getTemplateId(@NotNull ComputeOrder computeOrder) throws InvalidParameterException {
        String templateId = computeOrder.getImageId();
        if (templateId == null || templateId.isEmpty()) {
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

}
