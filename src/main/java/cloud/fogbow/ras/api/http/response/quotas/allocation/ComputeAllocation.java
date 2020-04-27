package cloud.fogbow.ras.api.http.response.quotas.allocation;

import io.swagger.annotations.ApiModelProperty;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;

import java.util.Objects;

@Embeddable
public class ComputeAllocation extends Allocation {
    @ApiModelProperty(position = 0, example = "2")
    @Transient
    private int instances;
    @ApiModelProperty(position = 1, example = "4")
    @Column(name = "vcpu_allocation")
    private int vCPU;
    @ApiModelProperty(position = 2, example = "8192")
    @Column(name = "ram_allocation")
    private int ram;
    @ApiModelProperty(position = 3, example = "30")
    @Column(name = "disk_allocation")
    private int disk;

    public ComputeAllocation(int instances, int vCPU, int ram, int disk) {
        this.instances = instances;
        this.vCPU = vCPU;
        this.ram = ram;
        this.disk = disk;
    }

    public ComputeAllocation(int instances, int vCPU, int ram) {
        this.vCPU = vCPU;
        this.ram = ram;
        this.instances = instances;
    }

    public ComputeAllocation() {
    }

    public int getInstances() {
        return this.instances;
    }
    public int getvCPU() {
        return this.vCPU;
    }

    public int getRam() {
        return this.ram;
    }

    public int getDisk() {
        return this.disk;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComputeAllocation that = (ComputeAllocation) o;
        return instances == that.instances &&
                vCPU == that.vCPU &&
                ram == that.ram &&
                disk == that.disk;
    }

    @Override
    public int hashCode() {
        return Objects.hash(instances, vCPU, ram, disk);
    }
}
