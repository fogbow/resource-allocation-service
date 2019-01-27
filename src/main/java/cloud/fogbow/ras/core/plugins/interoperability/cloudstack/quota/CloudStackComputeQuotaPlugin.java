package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.quota;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.core.constants.ConfigurationConstants;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9.GetVirtualMachineRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9.GetVirtualMachineResponse;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.quotas.ComputeQuota;
import cloud.fogbow.ras.core.models.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.core.plugins.interoperability.ComputeQuotaPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackHttpToFogbowExceptionMapper;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.util.connectivity.AuditableHttpRequestClient;

import java.util.List;
import java.util.Properties;

public class CloudStackComputeQuotaPlugin implements ComputeQuotaPlugin {
    private static final Logger LOGGER = Logger.getLogger(CloudStackComputeQuotaPlugin.class);

    private AuditableHttpRequestClient client = new AuditableHttpRequestClient(new Integer(PropertiesHolder.getInstance().getProperty(ConfigurationConstants.HTTP_REQUEST_TIMEOUT_KEY)));

    private static final String LIMIT_TYPE_INSTANCES = "0";
    private static final String LIMIT_TYPE_MEMORY = "9";
    private static final String LIMIT_TYPE_CPU = "8";
    private static final String CLOUDSTACK_URL = "cloudstack_api_url";

    private String cloudStackUrl;
    private Properties properties;

    public CloudStackComputeQuotaPlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        this.cloudStackUrl = this.properties.getProperty(CLOUDSTACK_URL);
    }

    @Override
    public ComputeQuota getUserQuota(CloudToken token) throws FogbowException {
        ListResourceLimitsRequest limitsRequest = new ListResourceLimitsRequest.Builder()
                .build(this.cloudStackUrl);
        CloudStackUrlUtil.sign(limitsRequest.getUriBuilder(), token.getTokenValue());

        String limitsResponse = null;
        try {
            limitsResponse = this.client.doGetRequest(limitsRequest.getUriBuilder().toString(), token);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowExceptionMapper.map(e);
        }

        ListResourceLimitsResponse response = ListResourceLimitsResponse.fromJson(limitsResponse);
        List<ListResourceLimitsResponse.ResourceLimit> resourceLimits = response.getResourceLimits();

        GetVirtualMachineRequest request = new GetVirtualMachineRequest.Builder()
                .build(this.cloudStackUrl);

        CloudStackUrlUtil.sign(request.getUriBuilder(), token.getTokenValue());

        String listMachinesResponse = null;
        try {
            listMachinesResponse = this.client.doGetRequest(request.getUriBuilder().toString(), token);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowExceptionMapper.map(e);
        }

        GetVirtualMachineResponse computeResponse = GetVirtualMachineResponse.fromJson(listMachinesResponse);
        List<GetVirtualMachineResponse.VirtualMachine> vms = computeResponse.getVirtualMachines();

        ComputeAllocation totalAllocation = getTotalAllocation(resourceLimits, token);
        ComputeAllocation usedQuota = getUsedAllocation(vms);
        return new ComputeQuota(totalAllocation, usedQuota);
    }

    private ComputeAllocation getUsedAllocation(List<GetVirtualMachineResponse.VirtualMachine> vms) {
        Integer vCpu = 0;
        Integer ram = 0;
        Integer instances = vms.size();
        for (GetVirtualMachineResponse.VirtualMachine vm : vms) {
            vCpu += vm.getCpuNumber();
            ram += vm.getMemory();
        }
        return new ComputeAllocation(vCpu, ram, instances);
    }

    private ComputeAllocation getTotalAllocation(List<ListResourceLimitsResponse.ResourceLimit> resourceLimits, CloudToken token) {
        int vCpu = Integer.MAX_VALUE;
        int ram = Integer.MAX_VALUE;
        int instances = Integer.MAX_VALUE;

        for (ListResourceLimitsResponse.ResourceLimit limit : resourceLimits) {
            // NOTE(pauloewerton): a -1 resource value means the account has unlimited quota. retrieve the domain
            // limit for this resource instead.
            if (limit.getMax() == -1) {
                try {
                    limit = getDomainResourceLimit(limit.getResourceType(), limit.getDomainId(), token);
                } catch (Exception ex) {
                    LOGGER.error(ex.getMessage(), ex);
                    continue;
                }
            }

            switch (limit.getResourceType()) {
                case LIMIT_TYPE_INSTANCES:
                    instances = Integer.valueOf(limit.getMax());
                    break;
                case LIMIT_TYPE_CPU:
                    vCpu = Integer.valueOf(limit.getMax());
                    break;
                case LIMIT_TYPE_MEMORY:
                    ram = Integer.valueOf(limit.getMax());
                    break;
            }
        }

        return new ComputeAllocation(vCpu, ram, instances);
    }

    private ListResourceLimitsResponse.ResourceLimit getDomainResourceLimit(String resourceType, String domainId, CloudToken token)
            throws FogbowException {
        ListResourceLimitsRequest limitsRequest = new ListResourceLimitsRequest.Builder()
                .domainId(domainId)
                .resourceType(resourceType)
                .build(this.cloudStackUrl);

        CloudStackUrlUtil.sign(limitsRequest.getUriBuilder(), token.getTokenValue());

        String limitsResponse = null;
        try {
            limitsResponse = this.client.doGetRequest(limitsRequest.getUriBuilder().toString(), token);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowExceptionMapper.map(e);
        }

        ListResourceLimitsResponse response = ListResourceLimitsResponse.fromJson(limitsResponse);
        // NOTE(pauloewerton): we're limiting result count by resource type, so request should only return one value
        ListResourceLimitsResponse.ResourceLimit resourceLimit = response.getResourceLimits().get(0);

        return resourceLimit;
    }

    protected void setClient(AuditableHttpRequestClient client) {
        this.client = client;
    }

}
