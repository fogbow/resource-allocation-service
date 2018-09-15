package org.fogbowcloud.ras.core.models.quotas;

import org.fogbowcloud.ras.core.models.quotas.allocation.ComputeAllocation;

public class ComputeQuota implements Quota {
    private ComputeAllocation totalQuota;
    private ComputeAllocation usedQuota;
    private ComputeAllocation availableQuota;

    public ComputeQuota(ComputeAllocation totalQuota, ComputeAllocation usedQuota) {
        this.totalQuota = totalQuota;
        this.usedQuota = usedQuota;
        this.availableQuota = calculateQuota(totalQuota, usedQuota);
    }

    @Override
    public ComputeAllocation getTotalQuota() {
        return this.totalQuota;
    }

    @Override
    public ComputeAllocation getUsedQuota() {
        return this.usedQuota;
    }

    @Override
    public ComputeAllocation getAvailableQuota() {
        return this.availableQuota;
    }

    private ComputeAllocation calculateQuota(ComputeAllocation totalQuota, ComputeAllocation usedQuota) {
        int availableVCpu = totalQuota.getvCPU() - usedQuota.getvCPU();
        int availableRam = totalQuota.getRam() - usedQuota.getRam();
        int availableInstance = totalQuota.getInstances() - usedQuota.getInstances();
        return new ComputeAllocation(availableVCpu, availableRam, availableInstance);
    }

}
