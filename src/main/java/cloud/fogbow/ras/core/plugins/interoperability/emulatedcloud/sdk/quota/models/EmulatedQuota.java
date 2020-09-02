package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.quota.models;

public class EmulatedQuota {
    private int instances;
    private int vCPU;
    private int ram;

    private int storage;
    private int volumes;

    private int networks;
    private int publicIps;

    public EmulatedQuota(int instances, int vCPU, int ram, int storage, int volumes, int networks, int publicIps) {
        this.instances = instances;
        this.vCPU = vCPU;
        this.ram = ram;
        this.storage = storage;
        this.volumes = volumes;
        this.networks = networks;
        this.publicIps = publicIps;
    }

    public static class Builder {
        private int instances;
        private int vCPU;
        private int ram;
        private int storage;
        private int volumes;
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

        public Builder storage(int storage) {
            this.storage = storage;
            return this;
        }

        public Builder volumes(int volumes) {
            this.volumes = volumes;
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

        public EmulatedQuota build() {
            return new EmulatedQuota(instances, vCPU, ram, storage, volumes, networks, publicIps);
        }
    }

    public int getInstances() {
        return instances;
    }

    public int getvCPU() {
        return vCPU;
    }

    public int getRam() {
        return ram;
    }

    public int getStorage() {
        return storage;
    }

    public int getVolumes() {
        return volumes;
    }

    public int getNetworks() {
        return networks;
    }

    public int getPublicIps() {
        return publicIps;
    }
}
