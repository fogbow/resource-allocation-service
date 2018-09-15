package org.fogbowcloud.ras.core.models.quotas.allocation;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class ComputeAllocation extends Allocation {
    @Column(name = "allocation_vcpu")
    private int vCPU;
    @Column(name = "allocation_ram")
    private int ram;
    @Column(name = "allocation_instances")
    private int instances;

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
}
