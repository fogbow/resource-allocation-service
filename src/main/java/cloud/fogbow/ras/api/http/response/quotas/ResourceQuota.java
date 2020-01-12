package cloud.fogbow.ras.api.http.response.quotas;

import com.google.common.annotations.VisibleForTesting;

import cloud.fogbow.ras.api.http.response.quotas.allocation.Allocation;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ResourceAllocation;

public class ResourceQuota extends Quota {

    private ResourceAllocation totalQuota;
    private ResourceAllocation usedQuota;
    private ResourceAllocation availableQuota;
    
    public ResourceQuota(ResourceAllocation totalQuota, ResourceAllocation usedQuota) {
        this.totalQuota = totalQuota;
        this.usedQuota = usedQuota;
        this.availableQuota = calculateQuota(totalQuota, usedQuota);
    }

    @Override
    public Allocation getTotalQuota() {
        return this.totalQuota;
    }

    @Override
    public Allocation getUsedQuota() {
        return this.usedQuota;
    }

    @Override
    public Allocation getAvailableQuota() {
        return this.availableQuota;
    }

    @Override
    public String toString() {
        return "ResourceQuota { totalQuota = " + totalQuota 
                + ", usedQuota = " + usedQuota 
                + ", availableQuota = " + availableQuota + " }";
    }
    
    @VisibleForTesting
    ResourceAllocation calculateQuota(ResourceAllocation totalQuota, ResourceAllocation usedQuota) {
        int instance = totalQuota.getInstances() - usedQuota.getInstances();
        int vCPU = totalQuota.getvCPU() - usedQuota.getvCPU();
        int ram = totalQuota.getRam() - usedQuota.getRam();
        int disk = totalQuota.getDisk() - usedQuota.getDisk();
        int networks = totalQuota.getNetworks() - usedQuota.getNetworks();
        int publicIps = totalQuota.getPublicIps() - usedQuota.getPublicIps();
        
        ResourceAllocation resourceAllocation = ResourceAllocation.builder()
                .instances(instance)
                .vCPU(vCPU)
                .ram(ram)
                .disk(disk)
                .networks(networks)
                .publicIps(publicIps)
                .build();
        
        return resourceAllocation;
    }
    
}
