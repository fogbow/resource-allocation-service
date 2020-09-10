package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.quota;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.quotas.ResourceQuota;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ResourceAllocation;
import cloud.fogbow.ras.core.plugins.interoperability.QuotaPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.quota.EmulatedCloudQuotaManager;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.quota.models.EmulatedQuota;

import java.util.Properties;

public class EmulatedCloudQuotaPlugin implements QuotaPlugin<CloudUser> {

    private Properties properties;

    public EmulatedCloudQuotaPlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        EmulatedCloudUtils.checkQuotaProperties(this.properties);
    }

    @Override
    public ResourceQuota getUserQuota(CloudUser cloudUser) throws FogbowException {
        EmulatedCloudQuotaManager quotaManager = EmulatedCloudQuotaManager.getInstance(properties);
        ResourceAllocation totalQuota = parseResourceAllocation(quotaManager.totalQuota());
        ResourceAllocation usedQuota = parseResourceAllocation(quotaManager.usedQuota());
        return new ResourceQuota(totalQuota, usedQuota);
    }

    private ResourceAllocation parseResourceAllocation(EmulatedQuota quota) {
        return ResourceAllocation.builder()
                .instances(quota.getInstances())
                .vCPU(quota.getvCPU())
                .ram(quota.getRam())
                .volumes(quota.getVolumes())
                .storage(quota.getStorage())
                .networks(quota.getNetworks())
                .publicIps(quota.getPublicIps())
                .build();
    }
}
