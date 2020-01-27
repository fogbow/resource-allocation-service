package cloud.fogbow.ras.api.http.response.quotas.allocation;

import io.swagger.annotations.ApiModelProperty;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import java.util.Objects;

@Embeddable
public class ComputeAllocation extends Allocation {
    @ApiModelProperty(position = 0, example = "2")
    @Column(name = "allocation_instances")
    private int instances;
    @ApiModelProperty(position = 1, example = "4")
    @Column(name = "allocation_vcpu")
    private int vCPU;
    @ApiModelProperty(position = 2, example = "8192")
    @Column(name = "allocation_ram")
    private int ram;
    @ApiModelProperty(position = 3, example = "30")
    @Column(name = "allocation_disk")
    private int disk;

    public ComputeAllocation(int vCPU, int ram, int instances, int disk) {
        this.vCPU = vCPU;
        this.ram = ram;
        this.instances = instances;
        this.disk = disk;
    }

    public ComputeAllocation(int vCPU, int ram, int instances) {
        this.vCPU = vCPU;
        this.ram = ram;
        this.instances = instances;
    }

    public ComputeAllocation() {
    }

    public int getvCPU() {
        return this.vCPU;
    }

    public int getRam() {
        return this.ram;
    }

    public int getInstances() {
        return this.instances;
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
