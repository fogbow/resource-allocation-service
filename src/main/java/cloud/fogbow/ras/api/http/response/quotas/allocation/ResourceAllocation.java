package cloud.fogbow.ras.api.http.response.quotas.allocation;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import io.swagger.annotations.ApiModelProperty;

@Embeddable
public class ResourceAllocation extends Allocation {

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

    @ApiModelProperty(position = 4, example = "15")
    @Column(name = "allocation_networks")
    private int networks;

    @ApiModelProperty(position = 5, example = "5")
    @Column(name = "allocation_public_ips")
    private int publicIps;

    public ResourceAllocation() {}

    public int getInstances() {
        return instances;
    }

    public int getvCPU() {
        return vCPU;
    }

    public int getRam() {
        return ram;
    }

    public int getDisk() {
        return disk;
    }

    public int getNetworks() {
        return networks;
    }

    public int getPublicIps() {
        return publicIps;
    }

    public static Builder builder() {
        return new ResourceAllocation.Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ResourceAllocation that = (ResourceAllocation) o;
        return instances == that.instances
                && vCPU == that.vCPU
                && ram == that.ram
                && disk == that.disk
                && networks == that.networks
                && publicIps == that.publicIps;
    }

    public static class Builder {
        private int instances;
        private int vCPU;
        private int ram;
        private int disk;
        private int networks;
        private int publicIps;

        public Builder instances(int instances) {
            this.instances = instances;
            return this;
        }

        public Builder vCPU(int vCPU) {
            this.vCPU = vCPU;
            return this;
        }

        public Builder ram(int ram) {
            this.ram = ram;
            return this;
        }

        public Builder disk(int disk) {
            this.disk = disk;
            return this;
        }

        public Builder networks(int networks) {
            this.networks = networks;
            return this;
        }

        public Builder publicIps(int publicIps) {
            this.publicIps = publicIps;
            return this;
        }

        public ResourceAllocation build() {
            return new ResourceAllocation(this);
        }
    }

    public ResourceAllocation(Builder builder) {
        this.instances = builder.instances;
        this.vCPU = builder.vCPU;
        this.ram = builder.ram;
        this.disk = builder.disk;
        this.networks = builder.networks;
        this.publicIps = builder.publicIps;
    }

}
