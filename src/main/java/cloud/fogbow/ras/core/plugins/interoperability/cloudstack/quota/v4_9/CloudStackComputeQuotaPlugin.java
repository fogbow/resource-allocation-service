package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.quota.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpToFogbowExceptionMapper;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.api.http.response.quotas.ComputeQuota;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.constants.Messages;
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

    private CloudStackHttpClient client;
    private String cloudStackUrl;

    public CloudStackComputeQuotaPlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.cloudStackUrl = properties.getProperty(CloudStackCloudUtils.CLOUDSTACK_URL_CONFIG);
        this.client = new CloudStackHttpClient();
    }

    @Override
    public ComputeQuota getUserQuota(@NotNull CloudStackUser cloudStackUser) throws FogbowException {
        ComputeAllocation totalQuota = buildTotalComputeAllocation(cloudStackUser);
        ComputeAllocation usedQuota = buildUsedComputeAllocation(cloudStackUser);
        return new ComputeQuota(totalQuota, usedQuota);
    }

    @VisibleForTesting
    List<ListResourceLimitsResponse.ResourceLimit> requestResourcesLimits(
            @NotNull ListResourceLimitsRequest request,
            @NotNull CloudStackUser cloudStackUser) throws FogbowException {

        URIBuilder uriRequest = request.getUriBuilder();
        CloudStackUrlUtil.sign(uriRequest, cloudStackUser.getToken());

        try {
            String responseStr = CloudStackCloudUtils.doRequest(this.client,
                    uriRequest.toString(), cloudStackUser);
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

    /**
     * Returns the Account Resource Limit whether the max is not unlimited(-1) otherwise
     * returns Domain Resource Limit.
     */
    @VisibleForTesting
    ListResourceLimitsResponse.ResourceLimit normalizeResourceLimit(
            @NotNull ListResourceLimitsResponse.ResourceLimit accountResourceLimit,
            @NotNull CloudStackUser cloudStackUser) throws FogbowException {

        if (accountResourceLimit.getMax() == CloudStackCloudUtils.UNLIMITED_ACCOUNT_QUOTA) {
            return requestDomainResourceLimit(accountResourceLimit.getResourceType(),
                    accountResourceLimit.getDomainId(), cloudStackUser);
        }
        return accountResourceLimit;
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
        for (ListResourceLimitsResponse.ResourceLimit resourceLimit : resourceLimits) {
            try {
                resourceLimit = normalizeResourceLimit(resourceLimit, cloudStackUser);

                switch (resourceLimit.getResourceType()) {
                    case CloudStackCloudUtils.INSTANCES_LIMIT_TYPE:
                        instances = Integer.valueOf(resourceLimit.getMax());
                        break;
                    case CloudStackCloudUtils.CPU_LIMIT_TYPE:
                        vCpu = Integer.valueOf(resourceLimit.getMax());
                        break;
                    case CloudStackCloudUtils.MEMORY_LIMIT_TYPE:
                        ram = Integer.valueOf(resourceLimit.getMax());
                        break;
                }
            } catch (FogbowException e) {
                LOGGER.warn(Messages.Warn.UNABLE_TO_RETRIEVE_RESOURCE_LIMIT, e);
            }
        }

        return new ComputeAllocation(vCpu, ram, instances);
    }

    /**
     * Returns an unique Resource Limit when It's specified the domainId and the resourceType
     * in the CloudStack request.
     */
    @NotNull
    ListResourceLimitsResponse.ResourceLimit requestDomainResourceLimit(String resourceType,
                                                                        String domainId,
                                                                        @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        ListResourceLimitsRequest request = new ListResourceLimitsRequest.Builder()
                .domainId(domainId)
                .resourceType(resourceType)
                .build(this.cloudStackUrl);

        List<ListResourceLimitsResponse.ResourceLimit> resourceLimits = requestResourcesLimits(
                request, cloudStackUser);
        return resourceLimits.listIterator().next();
    }

    @VisibleForTesting
    void setClient(CloudStackHttpClient client) {
        this.client = client;
    }

}
