package cloud.fogbow.ras.api.http.response.quotas;

import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import io.swagger.annotations.ApiModelProperty;

public class ComputeQuota extends Quota {
    @ApiModelProperty(position = 0, example = "{\n" +
            "        \"instances\": 2,\n" +
            "            \"vCPU\": 4,\n" +
            "            \"ram\": 8192\n" +
            "    }")
    private ComputeAllocation totalQuota;
    @ApiModelProperty(position = 1, example = "{\n" +
            "        \"instances\": 0,\n" +
            "            \"vCPU\": 0,\n" +
            "            \"ram\": 0\n" +
            "    }")
    private ComputeAllocation usedQuota;
    @ApiModelProperty(position = 2, example = "{\n" +
            "        \"instances\": 2,\n" +
            "            \"vCPU\": 4,\n" +
            "            \"ram\": 8192\n" +
            "    }")
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
        int availableDisk = totalQuota.getDisk() - usedQuota.getDisk();
        return new ComputeAllocation(availableInstance, availableVCpu, availableRam, availableDisk);
    }

    @Override
    public String toString() {
        return "ComputeQuota{" +
                "totalQuota=" + totalQuota +
                ", usedQuota=" + usedQuota +
                ", availableQuota=" + availableQuota +
                '}';
    }
}
