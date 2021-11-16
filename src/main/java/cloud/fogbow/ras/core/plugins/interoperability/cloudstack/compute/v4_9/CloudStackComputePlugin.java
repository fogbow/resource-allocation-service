package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.BinaryUnit;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.NetworkSummary;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.plugins.interoperability.ComputePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.compute.model.*;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.volume.model.GetAllDiskOfferingsRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.volume.model.GetAllDiskOfferingsResponse;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.volume.model.GetVolumeRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.volume.model.GetVolumeResponse;
import cloud.fogbow.ras.core.plugins.interoperability.util.DefaultLaunchCommandGenerator;
import cloud.fogbow.ras.core.plugins.interoperability.util.LaunchCommandGenerator;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class CloudStackComputePlugin implements ComputePlugin<CloudStackUser> {
    private static final Logger LOGGER = Logger.getLogger(CloudStackComputePlugin.class);

    @VisibleForTesting
    static final String EXPUNGE_ON_DESTROY_KEY_CONF = "expunge_on_destroy";
    @VisibleForTesting
    static final String CLOUDSTACK_URL_CONF = "cloudstack_api_url";
    @VisibleForTesting
    static final String ZONE_ID_KEY_CONF = "zone_id";

    private static final String DEFAULT_EXPUNGE_ON_DEPLOY_VALUE = "true";
    @VisibleForTesting
    static final String DEFAULT_VOLUME_TYPE_VALUE = "ROOT";
    @VisibleForTesting
    static final int UNKNOWN_DISK_VALUE = -1;

    @VisibleForTesting
    static final String FOGBOW_TAG_SEPARATOR = ":";
    @VisibleForTesting
    static final double GIGABYTE_IN_BYTES = Math.pow(1024, 3);
    @VisibleForTesting
    static final int AMOUNT_INSTANCE = 1;

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
    public String requestInstance(ComputeOrder computeOrder, CloudStackUser cloudUser)
            throws FogbowException {
        LOGGER.info(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER);
        String templateId = getTemplateId(computeOrder);
        String userData = this.launchCommandGenerator.createLaunchCommand(computeOrder);
        String networksId = normalizeNetworksID(computeOrder);
        String rootDiskSize = String.valueOf(computeOrder.getDisk());
        GetAllServiceOfferingsResponse.ServiceOffering serviceOffering =
                getServiceOffering(computeOrder, cloudUser);
        String instanceName = normalizeInstanceName(computeOrder.getName());

        DeployVirtualMachineRequest request = new DeployVirtualMachineRequest.Builder()
                .serviceOfferingId(serviceOffering.getId())
                .templateId(templateId)
                .zoneId(this.zoneId)
                .name(instanceName)
                .rootDiskSize(rootDiskSize)
                .userData(userData)
                .networksId(networksId)
                .build(this.cloudStackUrl);

        return doRequestInstance(request,
                serviceOffering, rootDiskSize, computeOrder, cloudUser);
    }

    @Override
    public ComputeInstance getInstance(ComputeOrder order,
                                       CloudStackUser cloudUser)
            throws FogbowException {
        String instanceId = order.getInstanceId();
        LOGGER.info(String.format(Messages.Log.GETTING_INSTANCE_S, instanceId));

        GetVirtualMachineRequest request = new GetVirtualMachineRequest.Builder()
                .id(instanceId)
                .build(this.cloudStackUrl);

        return doGetInstance(request, cloudUser);
    }

    @Override
    public void deleteInstance(ComputeOrder order, CloudStackUser cloudUser)
            throws FogbowException {
        String instanceId = order.getInstanceId();
        LOGGER.info(String.format(Messages.Log.DELETING_INSTANCE_S, instanceId));

        DestroyVirtualMachineRequest request = new DestroyVirtualMachineRequest.Builder()
                .id(instanceId)
                .expunge(this.expungeOnDestroy)
                .build(this.cloudStackUrl);

        doDeleteInstance(request, cloudUser, instanceId);
    }

    @Override
    public void takeSnapshot(ComputeOrder computeOrder, String name, CloudStackUser cloudUser) throws FogbowException {
        // ToDo: implement
    }

    @Override
    public void pauseInstance(ComputeOrder order, CloudStackUser cloudUser) throws FogbowException {
        // ToDo: implement
    }

    @Override
    public void hibernateInstance(ComputeOrder order, CloudStackUser cloudUser) throws FogbowException {
        // ToDo: implement
    }

    @Override
    public void stopInstance(ComputeOrder order, CloudStackUser cloudUser) throws FogbowException {
        // TODO implement
        throw new NotImplementedOperationException();
    }
    
    @Override
    public void resumeInstance(ComputeOrder order, CloudStackUser cloudUser) throws FogbowException {
        // ToDo: implement
    }

    @Override
    public boolean isPaused(String cloudState) throws FogbowException {
        return false;
    }

    @Override
    public boolean isHibernated(String cloudState) throws FogbowException {
        return false;
    }
    
    @Override
    public boolean isStopped(String cloudState) throws FogbowException {
        // TODO implement
        return false;
    }

    @VisibleForTesting
    ComputeInstance doGetInstance(GetVirtualMachineRequest request,
                                  CloudStackUser cloudStackUser) throws FogbowException {

        GetVirtualMachineResponse response = requestGetVirtualMachine(request, cloudStackUser);
        return buildComputeInstance(response, cloudStackUser);
    }

    @VisibleForTesting
    String doRequestInstance(DeployVirtualMachineRequest request,
                             GetAllServiceOfferingsResponse.ServiceOffering serviceOffering,
                             String diskSize,
                             ComputeOrder computeOrder,
                             CloudStackUser cloudUser) throws FogbowException {

        DeployVirtualMachineResponse response = requestDeployVirtualMachine(request, cloudUser);
        updateComputeOrder(computeOrder, serviceOffering, diskSize);
        return response.getId();
    }

    @VisibleForTesting
    void doDeleteInstance(DestroyVirtualMachineRequest request,
                          CloudStackUser cloudStackUser, String instanceId)
            throws FogbowException {

        URIBuilder uriRequest = request.getUriBuilder();
        String token = cloudStackUser.getToken();
        CloudStackUrlUtil.sign(uriRequest, token);

        try {
            doGet(uriRequest.toString(), cloudStackUser);
            LOGGER.info(String.format(Messages.Log.DELETING_INSTANCE_S_WITH_TOKEN_S, instanceId, token));
        } catch (FogbowException e) {
            LOGGER.error(String.format(Messages.Log.UNABLE_TO_DELETE_INSTANCE_S, instanceId), e);
            throw e;
        }
    }

    @VisibleForTesting
    GetAllServiceOfferingsResponse.ServiceOffering getServiceOffering(
            ComputeOrder computeOrder, CloudStackUser cloudUser)
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

        throw new UnacceptableOperationException(
                Messages.Exception.UNABLE_TO_COMPLETE_REQUEST_SERVICE_OFFERING_CLOUDSTACK);
    }

    @VisibleForTesting
    List<GetAllServiceOfferingsResponse.ServiceOffering> filterServicesOfferingByRequirements(
            List<GetAllServiceOfferingsResponse.ServiceOffering> serviceOfferings,
            ComputeOrder computeOrder) {

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

    @VisibleForTesting
    GetAllServiceOfferingsResponse getServiceOfferings(CloudStackUser cloudStackUser)
            throws FogbowException {

        GetAllServiceOfferingsRequest request = new GetAllServiceOfferingsRequest.Builder()
                .build(this.cloudStackUrl);
        URIBuilder uriRequest = request.getUriBuilder();
        String token = cloudStackUser.getToken();
        CloudStackUrlUtil.sign(uriRequest, token);

        String jsonResponse = doGet(uriRequest.toString(), cloudStackUser);
        return GetAllServiceOfferingsResponse.fromJson(jsonResponse);
    }

    @VisibleForTesting
    GetAllDiskOfferingsResponse getDiskOfferings(CloudStackUser cloudUser)
            throws FogbowException {

        GetAllDiskOfferingsRequest request = new GetAllDiskOfferingsRequest.Builder()
                .build(this.cloudStackUrl);
        URIBuilder uriRequest = request.getUriBuilder();
        String token = cloudUser.getToken();
        CloudStackUrlUtil.sign(uriRequest, token);

        String jsonResponse = doGet(uriRequest.toString(), cloudUser);
        return GetAllDiskOfferingsResponse.fromJson(jsonResponse);
    }

    @VisibleForTesting
    String normalizeNetworksID(ComputeOrder computeOrder) {
        List<String> networks = new ArrayList<>();
        networks.add(this.defaultNetworkId);
        List<String> userDefinedNetworks = computeOrder.getNetworkIds();
        if (!userDefinedNetworks.isEmpty()) {
            networks.addAll(userDefinedNetworks);
        }
        return StringUtils.join(networks, ",");
    }

    @VisibleForTesting
    String normalizeInstanceName(String instanceName) {
        return instanceName != null ? instanceName
                : SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + getRandomUUID();
    }

    @VisibleForTesting
    ComputeInstance createComputeInstance(GetVirtualMachineResponse.VirtualMachine virtualMachine,
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
    int getVirtualMachineDiskSize(String virtualMachineId, CloudStackUser cloudUser) {
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
                return (int) BinaryUnit.bytes(sizeInBytes).asGigabytes();
            }
        } catch (Exception e) {
            LOGGER.debug(Messages.Log.ERROR_WHILE_GETTING_DISK_SIZE, e);
        }
        LOGGER.warn(String.format(Messages.Log.UNABLE_TO_RETRIEVE_ROOT_VOLUME_S, virtualMachineId));
        return UNKNOWN_DISK_VALUE;
    }

    @VisibleForTesting
    String doGet(String url, CloudStackUser cloudUser) throws FogbowException {
        return this.client.doGetRequest(url, cloudUser);
    }

    @VisibleForTesting
    void updateComputeOrder(ComputeOrder computeOrder,
                            GetAllServiceOfferingsResponse.ServiceOffering serviceOffering,
                            String diskSize) {

        synchronized (computeOrder) {
            int disk = Integer.parseInt(diskSize);
            ComputeAllocation actualAllocation = new ComputeAllocation(
                    AMOUNT_INSTANCE, serviceOffering.getCpuNumber(),
                    serviceOffering.getMemory(),
                    disk);
            computeOrder.setActualAllocation(actualAllocation);
        }
    }

    @VisibleForTesting
    DeployVirtualMachineResponse requestDeployVirtualMachine(
            DeployVirtualMachineRequest request,
            CloudStackUser cloudUser)
            throws FogbowException {

        URIBuilder uriRequest = request.getUriBuilder();
        String token = cloudUser.getToken();
        CloudStackUrlUtil.sign(uriRequest, token);

        String jsonResponse = doGet(uriRequest.toString(), cloudUser);
        return DeployVirtualMachineResponse.fromJson(jsonResponse);
    }

    @VisibleForTesting
    GetVirtualMachineResponse requestGetVirtualMachine(GetVirtualMachineRequest request,
                                                       CloudStackUser cloudStackUser)
            throws FogbowException {

        URIBuilder uriRequest = request.getUriBuilder();
        String token = cloudStackUser.getToken();
        CloudStackUrlUtil.sign(uriRequest, token);

        String jsonResponse = doGet(uriRequest.toString(), cloudStackUser);
        return GetVirtualMachineResponse.fromJson(jsonResponse);
    }

    @VisibleForTesting
    ComputeInstance buildComputeInstance(GetVirtualMachineResponse response,
                                         CloudStackUser cloudStackUser)
            throws InstanceNotFoundException {

        List<GetVirtualMachineResponse.VirtualMachine> virtualMachines = response.getVirtualMachines();
        if (virtualMachines == null || virtualMachines.isEmpty()) {
            throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
        } else {
            GetVirtualMachineResponse.VirtualMachine virtualMachine = virtualMachines.listIterator().next();
            int disk = getVirtualMachineDiskSize(virtualMachine.getId(), cloudStackUser);
            return createComputeInstance(virtualMachine, disk);
        }
    }

    @VisibleForTesting
    String getTemplateId(ComputeOrder computeOrder) throws InvalidParameterException {
        String templateId = computeOrder.getImageId();
        if (templateId == null || templateId.isEmpty()) {
            throw new InvalidParameterException(Messages.Exception.UNABLE_TO_COMPLETE_REQUEST_CLOUDSTACK);
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
