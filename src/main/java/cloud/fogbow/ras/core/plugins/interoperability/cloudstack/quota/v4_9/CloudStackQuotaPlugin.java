package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.quota.v4_9;

import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ResourceAllocation;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9.GetVirtualMachineRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9.GetVirtualMachineResponse;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9.GetVirtualMachineResponse.VirtualMachine;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.network.v4_9.GetNetworkRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.network.v4_9.GetNetworkResponse;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.network.v4_9.GetNetworkResponse.Network;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetVolumeRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetVolumeResponse;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetVolumeResponse.Volume;
import com.google.common.annotations.VisibleForTesting;
import org.apache.log4j.Logger;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.quota.v4_9.ListResourceLimitsResponse.ResourceLimit;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.quota.v4_9.ListPublicIpAddressResponse.PublicIpAddress;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.ras.api.http.response.quotas.ResourceQuota;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.QuotaPlugin;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Properties;

public class CloudStackQuotaPlugin implements QuotaPlugin<CloudStackUser> {

    private static final Logger LOGGER = Logger.getLogger(CloudStackQuotaPlugin.class);
    private static final String CLOUDSTACK_URL = "cloudstack_api_url";

    private static final String LIMIT_TYPE_INSTANCES = "0";
    private static final String LIMIT_TYPE_PUBLIC_IP = "1";
    private static final String LIMIT_TYPE_VOLUME = "2";
    private static final String LIMIT_TYPE_NETWORK = "6";
    private static final String LIMIT_TYPE_CPU = "8";
    private static final String LIMIT_TYPE_MEMORY = "9";
    private static final String LIMIT_TYPE_STORAGE = "10";
    private static final int DOMAIN_LIMIT_NOT_FOUND_VALUE = 0;

    private Properties properties;
    private CloudStackHttpClient client;
    private String cloudStackUrl;

    public CloudStackQuotaPlugin(@NotBlank String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        this.cloudStackUrl = this.properties.getProperty(CLOUDSTACK_URL);
        this.client = new CloudStackHttpClient();
    }

    @Override
    public ResourceQuota getUserQuota(@NotNull CloudStackUser cloudUser) throws FogbowException {
        ResourceAllocation totalQuota = getTotalQuota(cloudUser);
        ResourceAllocation usedQuota = getUsedQuota(cloudUser);
        return new ResourceQuota(totalQuota, usedQuota);
    }

    @VisibleForTesting
    ResourceAllocation getUsedQuota(@NotNull CloudStackUser cloudUser) throws FogbowException {
        List<VirtualMachine> virtualMachines = getVirtualMachines(cloudUser);
        List<Volume> volumes = getVolumes(cloudUser);
        List<Network> networks = getNetworks(cloudUser);
        List<PublicIpAddress> publicIps = getPublicIpAddresses(cloudUser);
        return getUsedAllocation(virtualMachines, volumes, networks, publicIps);
    }

    @VisibleForTesting
    List<PublicIpAddress> getPublicIpAddresses(@NotNull CloudStackUser cloudUser) throws FogbowException {
        ListPublicIpAddressRequest request = new ListPublicIpAddressRequest.Builder().build(this.cloudStackUrl);
        CloudStackUrlUtil.sign(request.getUriBuilder(), cloudUser.getToken());

        String uri = request.getUriBuilder().toString();
        String jsonResponse = doGetRequest(cloudUser, uri);

        ListPublicIpAddressResponse response = ListPublicIpAddressResponse.fromJson(jsonResponse);
        return response.getPublicIpAddresses();
    }

    @VisibleForTesting
    String doGetRequest(@NotNull CloudStackUser cloudUser, @NotBlank String uri) throws FogbowException {
        String response = null;

        try {
            LOGGER.debug(Messages.Log.GETTING_QUOTA);
            response = this.client.doGetRequest(uri, cloudUser);
        } catch (FogbowException e) {
            LOGGER.debug(Messages.Exception.FAILED_TO_GET_QUOTA);
            throw e;
        }

        return response;
    }

    @VisibleForTesting
    List<Network> getNetworks(@NotNull CloudStackUser cloudUser) throws FogbowException {
        GetNetworkRequest request = new GetNetworkRequest.Builder().build(this.cloudStackUrl);
        CloudStackUrlUtil.sign(request.getUriBuilder(), cloudUser.getToken());

        String uri = request.getUriBuilder().toString();
        String networkJsonResponse = doGetRequest(cloudUser, uri);
        GetNetworkResponse response = GetNetworkResponse.fromJson(networkJsonResponse);

        return response.getNetworks();
    }

    @VisibleForTesting
    ResourceAllocation getTotalQuota(@NotNull CloudStackUser cloudUser) throws FogbowException {
        List<ResourceLimit> resourceLimits = getResourceLimits(cloudUser);
        return getTotalAllocation(resourceLimits, cloudUser);
    }

    @VisibleForTesting
    List<Volume> getVolumes(@NotNull CloudStackUser cloudUser) throws FogbowException {
        GetVolumeRequest volumeRequest = new GetVolumeRequest.Builder().build(this.cloudStackUrl);
        CloudStackUrlUtil.sign(volumeRequest.getUriBuilder(), cloudUser.getToken());

        String uri = volumeRequest.getUriBuilder().toString();
        String volumeJsonResponse = doGetRequest(cloudUser, uri);
        GetVolumeResponse volumeResponse = GetVolumeResponse.fromJson(volumeJsonResponse);

        return volumeResponse.getVolumes();
    }

    @VisibleForTesting
    List<VirtualMachine> getVirtualMachines(@NotNull CloudStackUser cloudUser) throws FogbowException {
        GetVirtualMachineRequest request = new GetVirtualMachineRequest.Builder().build(this.cloudStackUrl);
        CloudStackUrlUtil.sign(request.getUriBuilder(), cloudUser.getToken());

        String uri = request.getUriBuilder().toString();
        String responseJson = doGetRequest(cloudUser, uri);
        GetVirtualMachineResponse response = GetVirtualMachineResponse.fromJson(responseJson);

        return response.getVirtualMachines();
    }

    @VisibleForTesting
    List<ResourceLimit> getResourceLimits(@NotNull CloudStackUser cloudUser) throws FogbowException {
        ListResourceLimitsRequest request = new ListResourceLimitsRequest.Builder()
                .build(this.cloudStackUrl);
        CloudStackUrlUtil.sign(request.getUriBuilder(), cloudUser.getToken());

        String uri = request.getUriBuilder().toString();
        String limitsResponse = doGetRequest(cloudUser, uri);
        ListResourceLimitsResponse response = ListResourceLimitsResponse.fromJson(limitsResponse);
        return response.getResourceLimits();
    }

    @VisibleForTesting
    ResourceAllocation getUsedAllocation(@NotNull List<VirtualMachine> vms,
                                         @NotNull List<Volume> volumes,
                                         @NotNull List<Network> networks,
                                         @NotNull List<PublicIpAddress> publicIps) {
        int usedPublicIps = getPublicIpAllocation(publicIps);
        int usedNetworks = getNetworkAllocation(networks);
        int usedDisk = getVolumeAllocation(volumes);
        int usedVolumes = volumes.size();
        ComputeAllocation computeAllocation = this.buildComputeAllocation(vms);

        ResourceAllocation usedAllocation = ResourceAllocation.builder()
                .instances(computeAllocation.getInstances())
                .ram(computeAllocation.getRam())
                .vCPU(computeAllocation.getvCPU())
                .publicIps(usedPublicIps)
                .volumes(usedVolumes)
                .networks(usedNetworks)
                .storage(usedDisk)
                .build();

        return usedAllocation;
    }

    @VisibleForTesting
    int getNetworkAllocation(@NotNull List<Network> networks) {
        return networks.size();
    }

    @VisibleForTesting
    int getPublicIpAllocation(@NotNull List<PublicIpAddress> publicIps) {
        return publicIps.size();
    }

    @VisibleForTesting
    ComputeAllocation buildComputeAllocation(@NotNull List<VirtualMachine> vms) {
        int instances = vms.size();
        int cores = 0;
        int ram = 0;

        for (VirtualMachine vm : vms) {
            cores += vm.getCpuNumber();
            ram += vm.getMemory();
        }

        return new ComputeAllocation(instances, cores, ram);
    }

    @VisibleForTesting
    int getVolumeAllocation(@NotNull List<Volume> volumes) {
        long sizeInBytes = 0;

        for (Volume volume: volumes) {
            sizeInBytes += volume.getSize();
        }

        return CloudStackCloudUtils.convertToGigabyte(sizeInBytes);
    }

    @VisibleForTesting
    ResourceLimit getDomainResourceLimit(@NotNull ResourceLimit limit, @NotNull CloudStackUser cloudUser) {
        try {
            limit = doGetDomainResourceLimit(limit.getResourceType(), limit.getDomainId(), cloudUser);
        } catch (Exception ex) {
            limit = new ResourceLimit(limit.getResourceType(), limit.getDomainId(), DOMAIN_LIMIT_NOT_FOUND_VALUE);
        }

        return limit;
    }

    @VisibleForTesting
    ResourceAllocation getTotalAllocation(@NotNull List<ResourceLimit> resourceLimits, @NotNull CloudStackUser cloudUser) {
        ResourceAllocation.Builder builder = ResourceAllocation.builder();
        int max = 0;

        for (ResourceLimit resourceLimit : resourceLimits) {
            if (resourceLimit.getMax() == -1) {
                resourceLimit = getDomainResourceLimit(resourceLimit, cloudUser);
            }

            max = Integer.valueOf(resourceLimit.getMax());

            switch (resourceLimit.getResourceType()) {
                case LIMIT_TYPE_INSTANCES: builder.instances(max); break;
                case LIMIT_TYPE_PUBLIC_IP: builder.publicIps(max); break;
                case LIMIT_TYPE_STORAGE: builder.storage(max); break;
                case LIMIT_TYPE_VOLUME: builder.volumes(max); break;
                case LIMIT_TYPE_NETWORK: builder.networks(max); break;
                case LIMIT_TYPE_CPU: builder.vCPU(max); break;
                case LIMIT_TYPE_MEMORY: builder.ram(max); break;
            }
        }

        ResourceAllocation totalAllocation = builder.build();
        return totalAllocation;
    }

    @VisibleForTesting
    ResourceLimit doGetDomainResourceLimit(@NotBlank String resourceType, @NotBlank String domainId, @NotNull CloudStackUser cloudUser)
            throws FogbowException {
        ListResourceLimitsRequest request = new ListResourceLimitsRequest.Builder()
                .domainId(domainId)
                .resourceType(resourceType)
                .build(this.cloudStackUrl);

        CloudStackUrlUtil.sign(request.getUriBuilder(), cloudUser.getToken());

        String uri = request.getUriBuilder().toString();
        String limitsResponse = doGetRequest(cloudUser, uri);
        ListResourceLimitsResponse response = ListResourceLimitsResponse.fromJson(limitsResponse);
        // NOTE(pauloewerton): we're limiting result count by resource type, so request should only return one value
        ResourceLimit resourceLimit = response.getResourceLimits().listIterator().next();
        return resourceLimit;
    }

    @VisibleForTesting
    void setClient(@NotNull CloudStackHttpClient client) {
        this.client = client;
    }
}
