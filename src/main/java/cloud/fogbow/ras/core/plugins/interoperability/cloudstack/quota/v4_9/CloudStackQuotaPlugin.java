package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.quota.v4_9;

import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpToFogbowExceptionMapper;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ResourceAllocation;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9.GetVirtualMachineRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9.GetVirtualMachineResponse;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetVolumeRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetVolumeResponse;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.ras.api.http.response.quotas.ResourceQuota;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.QuotaPlugin;

import java.util.List;
import java.util.Properties;

public class CloudStackQuotaPlugin implements QuotaPlugin<CloudStackUser> {

    private static final Logger LOGGER = Logger.getLogger(CloudStackQuotaPlugin.class);
    private static final String CLOUDSTACK_URL = "cloudstack_api_url";

    private static final String LIMIT_TYPE_INSTANCES = "0";
    private static final String LIMIT_TYPE_PUBLIC_IP = "1";
    private static final String LIMIT_TYPE_NETWORK = "6";
    private static final String LIMIT_TYPE_CPU = "8";
    private static final String LIMIT_TYPE_MEMORY = "9";
    private static final String LIMIT_TYPE_STORAGE = "10";

    private Properties properties;
    private CloudStackHttpClient client;
    private String cloudStackUrl;

    public CloudStackQuotaPlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        this.cloudStackUrl = this.properties.getProperty(CLOUDSTACK_URL);
        this.client = new CloudStackHttpClient();
    }

    @Override
    public ResourceQuota getUserQuota(CloudStackUser cloudUser) throws FogbowException {
        ResourceAllocation totalQuota = getTotalQuota(cloudUser);
        ResourceAllocation usedQuota = getUsedQuota(cloudUser);
        return new ResourceQuota(totalQuota, usedQuota);
    }

    private ResourceAllocation getUsedQuota(CloudStackUser cloudUser) throws FogbowException {
        List<GetVirtualMachineResponse.VirtualMachine> virtualMachines = getVirtualMachines(cloudUser);
        List<GetVolumeResponse.Volume> volumes = getVolumes(cloudUser);
        return getUsedAllocation(virtualMachines, volumes);
    }

    private ResourceAllocation getTotalQuota(CloudStackUser cloudUser) throws FogbowException {
        List<ListResourceLimitsResponse.ResourceLimit> resourceLimits = getResourceLimits(cloudUser);
        return getTotalAllocation(resourceLimits, cloudUser);
    }

    private List<GetVolumeResponse.Volume> getVolumes(CloudStackUser cloudUser) throws FogbowException {
        GetVolumeRequest volumeRequest = new GetVolumeRequest.Builder().build(this.cloudStackUrl);
        CloudStackUrlUtil.sign(volumeRequest.getUriBuilder(), cloudUser.getToken());

        String volumeJsonResponse;
        GetVolumeResponse volumeResponse = null;

        try {
            volumeJsonResponse = this.client.doGetRequest(volumeRequest.getUriBuilder().toString(), cloudUser);
            volumeResponse = GetVolumeResponse.fromJson(volumeJsonResponse);
        } catch (HttpResponseException e) {
            e.printStackTrace();
        }

        return volumeResponse.getVolumes();
    }

    private List<GetVirtualMachineResponse.VirtualMachine> getVirtualMachines(CloudStackUser cloudUser) throws FogbowException {
        GetVirtualMachineRequest request = new GetVirtualMachineRequest.Builder().build(this.cloudStackUrl);
        CloudStackUrlUtil.sign(request.getUriBuilder(), cloudUser.getToken());

        String responseJson;
        GetVirtualMachineResponse response = null;

        try {
            responseJson = this.client.doGetRequest(request.getUriBuilder().toString(), cloudUser);
            response = GetVirtualMachineResponse.fromJson(responseJson);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowExceptionMapper.map(e);
        }

        return response.getVirtualMachines();
    }

    private List<ListResourceLimitsResponse.ResourceLimit> getResourceLimits(CloudStackUser cloudUser) throws FogbowException {
        ListResourceLimitsRequest limitsRequest = new ListResourceLimitsRequest.Builder()
                .build(this.cloudStackUrl);
        CloudStackUrlUtil.sign(limitsRequest.getUriBuilder(), cloudUser.getToken());

        String limitsResponse = null;

        try {
            limitsResponse = this.client.doGetRequest(limitsRequest.getUriBuilder().toString(), cloudUser);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowExceptionMapper.map(e);
        }

        ListResourceLimitsResponse response = ListResourceLimitsResponse.fromJson(limitsResponse);
        return response.getResourceLimits();
    }


    private ResourceAllocation getUsedAllocation(List<GetVirtualMachineResponse.VirtualMachine> vms, List<GetVolumeResponse.Volume> volumes) {
        int usedCores = 0;
        int usedRam = 0;
        int usedInstances = vms.size();
        long usedDisk = 0;
        int usedPublicIps = 0;
        int usedNetworks = 0;

        for (GetVirtualMachineResponse.VirtualMachine vm : vms) {
            usedCores += vm.getCpuNumber();
            usedRam += vm.getMemory();
        }

        for (GetVolumeResponse.Volume volume: volumes) {
            usedDisk += volume.getSize();
        }

        int usedDiskInGigabyte = CloudStackCloudUtils.convertToGigabyte(usedDisk);

        ResourceAllocation usedAllocation = ResourceAllocation.builder()
                .instances(usedInstances)
                .ram(usedRam)
                .vCPU(usedCores)
                .disk(usedDiskInGigabyte)
                .publicIps(usedPublicIps)
                .networks(usedNetworks)
                .build();

        return usedAllocation;
    }

    private ResourceAllocation getTotalAllocation(List<ListResourceLimitsResponse.ResourceLimit> resourceLimits, CloudStackUser cloudUser) {
        int vCpu = Integer.MAX_VALUE;
        int ram = Integer.MAX_VALUE;
        int instances = Integer.MAX_VALUE;
        int disk = Integer.MAX_VALUE;
        int networks = Integer.MAX_VALUE;
        int publicIps = Integer.MAX_VALUE;

        for (ListResourceLimitsResponse.ResourceLimit limit : resourceLimits) {
            // NOTE(pauloewerton): a -1 resource value means the account has unlimited quota. retrieve the domain
            // limit for this resource instead.
            if (limit.getMax() == -1) {
                try {
                    limit = getDomainResourceLimit(limit.getResourceType(), limit.getDomainId(), cloudUser);
                } catch (Exception ex) {
                    LOGGER.error(ex.getMessage(), ex);
                    continue;
                }
            }

            switch (limit.getResourceType()) {
                case LIMIT_TYPE_INSTANCES:
                    instances = Integer.valueOf(limit.getMax());
                    break;
                case LIMIT_TYPE_PUBLIC_IP:
                    publicIps = Integer.valueOf(limit.getMax());
                    break;
                case LIMIT_TYPE_STORAGE:
                    disk = Integer.valueOf(limit.getMax());
                    break;
                case LIMIT_TYPE_NETWORK:
                    networks = Integer.valueOf(limit.getMax());
                    break;
                case LIMIT_TYPE_CPU:
                    vCpu = Integer.valueOf(limit.getMax());
                    break;
                case LIMIT_TYPE_MEMORY:
                    ram = Integer.valueOf(limit.getMax());
                    break;
            }
        }

        ResourceAllocation totalAllocation = ResourceAllocation.builder()
                .vCPU(vCpu)
                .ram(ram)
                .instances(instances)
                .disk(disk)
                .publicIps(publicIps)
                .networks(networks)
                .build();

        return totalAllocation;
    }

    private ListResourceLimitsResponse.ResourceLimit getDomainResourceLimit(String resourceType, String domainId, CloudStackUser cloudUser)
            throws FogbowException {
        ListResourceLimitsRequest limitsRequest = new ListResourceLimitsRequest.Builder()
                .domainId(domainId)
                .resourceType(resourceType)
                .build(this.cloudStackUrl);

        CloudStackUrlUtil.sign(limitsRequest.getUriBuilder(), cloudUser.getToken());

        String limitsResponse = null;
        try {
            limitsResponse = this.client.doGetRequest(limitsRequest.getUriBuilder().toString(), cloudUser);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowExceptionMapper.map(e);
        }

        ListResourceLimitsResponse response = ListResourceLimitsResponse.fromJson(limitsResponse);
        // NOTE(pauloewerton): we're limiting result count by resource type, so request should only return one value
        ListResourceLimitsResponse.ResourceLimit resourceLimit = response.getResourceLimits().get(0);
        return resourceLimit;
    }
}
