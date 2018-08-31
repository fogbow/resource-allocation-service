package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.quota;

import org.apache.http.client.HttpResponseException;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.quotas.ComputeQuota;
import org.fogbowcloud.ras.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.ComputeQuotaPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackHttpToFogbowRasExceptionMapper;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.compute.v4_9.GetVirtualMachineRequest;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.compute.v4_9.GetVirtualMachineResponse;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;

import java.util.List;


public class CloudStackComputeQuotaPlugin implements ComputeQuotaPlugin {

    private HttpRequestClientUtil client = new HttpRequestClientUtil();

    private static final String LIMIT_TYPE_INSTANCES = "0";
    private static final String LIMIT_TYPE_MEMORY = "9";
    private static final String LIMIT_TYPE_CPU = "8";

    @Override
    public ComputeQuota getUserQuota(Token token) throws FogbowRasException, UnexpectedException {
        ListResourceLimitsRequest limitsRequest = new ListResourceLimitsRequest();
        CloudStackUrlUtil.sign(limitsRequest.getUriBuilder(), token.getTokenValue());

        String limitsResponse = null;
        try {
            limitsResponse = this.client.doGetRequest(limitsRequest.getUriBuilder().toString(), token);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowRasExceptionMapper.map(e);
        }

        ListResourceLimitsResponse response = ListResourceLimitsResponse.fromJson(limitsResponse);
        List<ListResourceLimitsResponse.ResourceLimit> resourceLimits = response.getResourceLimits();

        GetVirtualMachineRequest request = new GetVirtualMachineRequest.Builder()
                .build();

        CloudStackUrlUtil.sign(request.getUriBuilder(), token.getTokenValue());

        String listMachinesResponse = null;
        try {
            listMachinesResponse = this.client.doGetRequest(request.getUriBuilder().toString(), token);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowRasExceptionMapper.map(e);
        }

        GetVirtualMachineResponse computeResponse = GetVirtualMachineResponse.fromJson(listMachinesResponse);
        List<GetVirtualMachineResponse.VirtualMachine> vms = computeResponse.getVirtualMachines();

        ComputeAllocation totalAllocation = getTotalAllocation(resourceLimits);
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

    private ComputeAllocation getTotalAllocation(List<ListResourceLimitsResponse.ResourceLimit> resourceLimits) {
        int vCpu = Integer.MAX_VALUE;
        int ram = Integer.MAX_VALUE;
        int instances = Integer.MAX_VALUE;
        for (ListResourceLimitsResponse.ResourceLimit limit :
                resourceLimits) {
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
                default:
                    break;
            }
        }
        return new ComputeAllocation(vCpu, ram, instances);
    }

}
