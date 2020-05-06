package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.quota;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.quotas.ResourceQuota;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ResourceAllocation;
import cloud.fogbow.ras.core.plugins.interoperability.QuotaPlugin;

import java.util.Properties;

public class EmulatedCloudQuotaPlugin implements QuotaPlugin<CloudUser> {

    private Properties properties;

    public EmulatedCloudQuotaPlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
    }

    public ResourceAllocation createTotalQuota() {
        int totalInstances = 100;
        int totalvCPU = 8;
        int tolalRam = 16384;
        int totalDisk = 30;
        int totalNetworks = 15;
        int totalPublicIps = 5;
        int totalVolumes = 200;

        ResourceAllocation totalQuota = ResourceAllocation.builder()
                .instances(totalInstances)
                .vCPU(totalvCPU)
                .ram(tolalRam)
                .storage(totalDisk)
                .networks(totalNetworks)
                .volumes(totalVolumes)
                .publicIps(totalPublicIps)
                .build();

        return totalQuota;
    }

    public ResourceAllocation createUsedQuota() {
        int usedInstances = 1;
        int usedvCPU = 2;
        int usedRam = 8192;
        int usedDisk = 8;
        int usedNetworks = 1;
        int usedPublicIps = 1;
        int usedVolumes = 2;

        ResourceAllocation usedQuota =  ResourceAllocation.builder()
                .instances(usedInstances)
                .vCPU(usedvCPU)
                .ram(usedRam)
                .storage(usedDisk)
                .volumes(usedVolumes)
                .networks(usedNetworks)
                .publicIps(usedPublicIps)
                .build();

        return usedQuota;
    }

    @Override
    public ResourceQuota getUserQuota(CloudUser cloudUser) throws FogbowException {
        ResourceAllocation totalQuota = createTotalQuota();
        ResourceAllocation usedQuota = createUsedQuota();
        return new ResourceQuota(totalQuota, usedQuota);
    }
}
