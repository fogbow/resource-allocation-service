package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.quota.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpToFogbowExceptionMapper;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.api.http.response.quotas.ComputeQuota;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.core.plugins.interoperability.ComputeQuotaPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9.GetVirtualMachineRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9.GetVirtualMachineResponse;
import com.google.common.annotations.VisibleForTesting;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Properties;

public class CloudStackComputeQuotaPlugin implements ComputeQuotaPlugin<CloudStackUser> {
    private static final Logger LOGGER = Logger.getLogger(CloudStackComputeQuotaPlugin.class);

    private static final String LIMIT_INSTANCES_TYPE = "0";
    private static final String LIMIT_MEMORY_TYPE = "9";
    private static final String LIMIT_CPU_TYPE = "8";
    private static final int UNLIMITED_ACCOUNT_QUOTA = -1;

    private CloudStackHttpClient client;
    private String cloudStackUrl;

    public CloudStackComputeQuotaPlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.cloudStackUrl = properties.getProperty(CloudStackCloudUtils.CLOUDSTACK_URL_CONFIG);
        this.client = new CloudStackHttpClient();
    }

    @Override
    public ComputeQuota getUserQuota(@NotNull CloudStackUser cloudStackUser) throws FogbowException {
        ComputeAllocation totalAllocation = buildTotalComputeAllocation(cloudStackUser);
        ComputeAllocation usedQuota = buildUsedComputeAllocation(cloudStackUser);
        return new ComputeQuota(totalAllocation, usedQuota);
    }

    @VisibleForTesting
    List<ListResourceLimitsResponse.ResourceLimit> requestResourcesLimits(
            @NotNull ListResourceLimitsRequest request,
            @NotNull CloudStackUser cloudStackUser) throws FogbowException {

        URIBuilder uriRequest = request.getUriBuilder();
        CloudStackUrlUtil.sign(uriRequest, cloudStackUser.getToken());

        try {
            String responseStr = this.client.doGetRequest(uriRequest.toString(), cloudStackUser);
            ListResourceLimitsResponse response = ListResourceLimitsResponse.fromJson(responseStr);
            return response.getResourceLimits();
        } catch (HttpResponseException e) {
            throw CloudStackHttpToFogbowExceptionMapper.get(e);
        }
    }

    @VisibleForTesting
    ComputeAllocation buildUsedComputeAllocation(@NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        GetVirtualMachineRequest request = new GetVirtualMachineRequest.Builder()
                .build(this.cloudStackUrl);
        GetVirtualMachineResponse response = CloudStackCloudUtils.requestGetVirtualMachine(
                this.client, request, cloudStackUser);
        List<GetVirtualMachineResponse.VirtualMachine> vms = response.getVirtualMachines();

        Integer vCpu = 0;
        Integer ram = 0;
        Integer instances = vms.size();
        for (GetVirtualMachineResponse.VirtualMachine vm : vms) {
            vCpu += vm.getCpuNumber();
            ram += vm.getMemory();
        }
        return new ComputeAllocation(vCpu, ram, instances);
    }

    @VisibleForTesting
    ComputeAllocation buildTotalComputeAllocation(@NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        ListResourceLimitsRequest request = new ListResourceLimitsRequest.Builder()
                .build(this.cloudStackUrl);
        List<ListResourceLimitsResponse.ResourceLimit> resourceLimits = requestResourcesLimits(
                request, cloudStackUser);

        int vCpu = Integer.MAX_VALUE;
        int ram = Integer.MAX_VALUE;
        int instances = Integer.MAX_VALUE;
        for (ListResourceLimitsResponse.ResourceLimit limit : resourceLimits) {
            // NOTE(pauloewerton): a -1 resource value means the account has unlimited quota. retrieve the domain
            // limit for this resource instead.
            if (limit.getMax() == UNLIMITED_ACCOUNT_QUOTA) {
                try {
                    limit = getDomainResourceLimit(limit.getResourceType(), limit.getDomainId(), cloudStackUser);
                } catch (Exception ex) {
                    LOGGER.error(ex.getMessage(), ex);
                    continue;
                }
            }

            switch (limit.getResourceType()) {
                case LIMIT_INSTANCES_TYPE:
                    instances = Integer.valueOf(limit.getMax());
                    break;
                case LIMIT_CPU_TYPE:
                    vCpu = Integer.valueOf(limit.getMax());
                    break;
                case LIMIT_MEMORY_TYPE:
                    ram = Integer.valueOf(limit.getMax());
                    break;
            }
        }

        return new ComputeAllocation(vCpu, ram, instances);
    }

    @NotNull
    ListResourceLimitsResponse.ResourceLimit getDomainResourceLimit(String resourceType,
                                                                    String domainId,
                                                                    @NotNull CloudStackUser cloudUser)
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

    @NotNull
    void setClient(CloudStackHttpClient client) {
        this.client = client;
    }

}
