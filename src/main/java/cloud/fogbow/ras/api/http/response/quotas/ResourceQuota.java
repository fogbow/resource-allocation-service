package cloud.fogbow.ras.api.http.response.quotas;

import java.util.Objects;

import com.google.common.annotations.VisibleForTesting;

import cloud.fogbow.ras.api.http.response.quotas.allocation.Allocation;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ResourceAllocation;

public class ResourceQuota extends Quota {

    public static final int UNLIMITED_RESOURCE = -1;
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
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ResourceQuota that = (ResourceQuota) o;
        return Objects.equals(totalQuota, that.totalQuota) 
                && Objects.equals(usedQuota, that.usedQuota)
                && Objects.equals(availableQuota, that.availableQuota);
    }

    @Override
    public String toString() {
        return "ResourceQuota { totalQuota = " + totalQuota 
                + ", usedQuota = " + usedQuota 
                + ", availableQuota = " + availableQuota + " }";
    }
    
    @VisibleForTesting
    ResourceAllocation calculateQuota(ResourceAllocation totalQuota, ResourceAllocation usedQuota) {
        int instance = calculateResource(totalQuota.getInstances(), usedQuota.getInstances());
        int vCPU = calculateResource(totalQuota.getvCPU(), usedQuota.getvCPU());;
        int ram = calculateResource(totalQuota.getRam(), usedQuota.getRam());
        int disk = calculateResource(totalQuota.getStorage(), usedQuota.getStorage());;
        int networks = calculateResource(totalQuota.getNetworks(), usedQuota.getNetworks());;
        int publicIps = calculateResource(totalQuota.getPublicIps(), usedQuota.getPublicIps());;

        ResourceAllocation resourceAllocation = ResourceAllocation.builder()
                .instances(instance)
                .vCPU(vCPU)
                .ram(ram)
                .storage(disk)
                .networks(networks)
                .publicIps(publicIps)
                .build();
        
        return resourceAllocation;
    }

    @VisibleForTesting
    int calculateResource(int total, int used) {
        int available = total - used;

        // NOTE(jadsonluan): returns -1 if available resource is negative. This will happen when total resource is
        // unlimited (-1)
        return available < 0 ? UNLIMITED_RESOURCE : available;
    }

}
