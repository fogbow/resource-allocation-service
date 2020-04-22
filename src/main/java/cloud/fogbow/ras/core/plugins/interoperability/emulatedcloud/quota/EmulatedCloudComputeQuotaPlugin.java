package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.quota;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.quotas.ComputeQuota;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.core.plugins.interoperability.ComputeQuotaPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.emulatedmodels.EmulatedComputeQuota;

import java.util.Properties;

public class EmulatedCloudComputeQuotaPlugin implements ComputeQuotaPlugin<CloudUser> {

    private Properties properties;

    public EmulatedCloudComputeQuotaPlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
    }

    @Override
    public ComputeQuota getUserQuota(CloudUser cloudUser) throws FogbowException {
        EmulatedComputeQuota computeQuota = new EmulatedComputeQuota();

        ComputeAllocation totalAllocation = getAllocationValue(computeQuota.getTotalQuota());
        ComputeAllocation usedAllocation = getAllocationValue(computeQuota.getUsedQuota());

        return new ComputeQuota(totalAllocation, usedAllocation);
    }

    private ComputeAllocation getAllocationValue(EmulatedComputeQuota.Quota quota) {
        int vcpu = quota.getVcpu();
        int memory = quota.getMemory();
        int disk = quota.getDisk();
        int instances = quota.getIntances();

        return new ComputeAllocation(instances, vcpu, memory, disk);
    }
}
