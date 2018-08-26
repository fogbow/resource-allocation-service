package org.fogbowcloud.ras.core.models.instances;

public class ComputeInstance extends Instance {
    private String hostName;
    private int vCPU;
    /**
     * Memory attribute, must be set in MB.
     */
    private int ram;
    /**
     * Disk attribute, must be set in GB.
     */
    private int disk;
    private String localIpAddress;

    public ComputeInstance(String id, InstanceState state, String hostName, int vCPU, int ram, int disk,
                           String localIpAddress) {
        super(id, state);
        this.hostName = hostName;
        this.vCPU = vCPU;
        this.ram = ram;
        this.disk = disk;
        this.localIpAddress = localIpAddress;
    }

    public ComputeInstance(String id) {
        super(id);
    }

    public int getDisk() {
        return this.disk;
    }

    public String getHostName() {
        return this.hostName;
    }

    public String getLocalIpAddress() {
        return this.localIpAddress;
    }

    public int getRam() {
        return this.ram;
    }

    public int getvCPU() {
        return this.vCPU;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ComputeInstance that = (ComputeInstance) o;

        if (vCPU != that.vCPU) return false;
        if (ram != that.ram) return false;
        if (disk != that.disk) return false;
        if (hostName != null ? !hostName.equals(that.hostName) : that.hostName != null) return false;
        return localIpAddress != null ? localIpAddress.equals(that.localIpAddress) : that.localIpAddress == null;
    }
}
