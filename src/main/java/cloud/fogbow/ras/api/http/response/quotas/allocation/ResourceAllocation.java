package cloud.fogbow.ras.api.http.response.quotas.allocation;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;

import java.util.Objects;

@Embeddable
public class ResourceAllocation extends Allocation {

    @Embedded
    private ComputeAllocation computeAllocation;

    @Embedded
    private VolumeAllocation volumeAllocation;

    @Embedded
    private NetworkAllocation networkAllocation;

    @Embedded
    private PublicIpAllocation publicIpAllocation;
    
    public ResourceAllocation() {}

    public int getInstances() {
        return computeAllocation.getInstances();
    }

    public int getvCPU() {
        return computeAllocation.getvCPU();
    }

    public int getRam() {
        return computeAllocation.getRam();
    }

    public int getDisk() {
        return volumeAllocation.getDisk();
    }

    public int getNetworks() {
        return networkAllocation.getNetworks();
    }

    public int getPublicIps() {
        return publicIpAllocation.getPublicIps();
    }

    public static Builder builder() {
        return new ResourceAllocation.Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceAllocation that = (ResourceAllocation) o;
        return Objects.equals(computeAllocation, that.computeAllocation) &&
                Objects.equals(volumeAllocation, that.volumeAllocation) &&
                Objects.equals(networkAllocation, that.networkAllocation) &&
                Objects.equals(publicIpAllocation, that.publicIpAllocation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(computeAllocation, volumeAllocation, networkAllocation, publicIpAllocation);
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
        int instances = builder.instances;
        int vCPU = builder.vCPU;
        int ram = builder.ram;
        int disk = builder.disk;
        int networks = builder.networks;
        int publicIps = builder.publicIps;

        this.computeAllocation = new ComputeAllocation(vCPU, ram, instances);
        this.volumeAllocation = new VolumeAllocation(disk);
        this.networkAllocation = new NetworkAllocation(networks);
        this.publicIpAllocation = new PublicIpAllocation(publicIps);
    }
    
}
