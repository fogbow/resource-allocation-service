package org.fogbowcloud.ras.core.models.instances;

import java.util.List;
import java.util.Map;

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
    private List<String> ipAddresses;
    /**
     * Order-related properties
     */
    private Map<String, String> networks;
    private String image;
    private String publicKey;
    private String userData;

    public ComputeInstance(String id, InstanceState state, String hostName, int vCPU, int ram, int disk,
                           List<String> ipAddresses) {
        super(id, state);
        this.hostName = hostName;
        this.vCPU = vCPU;
        this.ram = ram;
        this.disk = disk;
        this.ipAddresses = ipAddresses;
    }

    public ComputeInstance(String id, InstanceState state, String hostName, int vCPU, int ram, int disk,
                           List<String> ipAddresses, String image, String publicKey, String userData) {
        super(id, state);
        this.hostName = hostName;
        this.vCPU = vCPU;
        this.ram = ram;
        this.disk = disk;
        this.ipAddresses = ipAddresses;
        this.image = image;
        this.publicKey = publicKey;
        this.userData = userData;
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

    public List<String> getIpAddresses() {
        return this.ipAddresses;
    }

    public int getRam() {
        return this.ram;
    }

    public int getvCPU() {
        return this.vCPU;
    }

    public Map<String, String> getNetworks() {
        return networks;
    }

    public void setNetworks(Map<String, String> networks) {
        this.networks = networks;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getUserData() {
        return userData;
    }

    public void setUserData(String userData) {
        this.userData = userData;
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
        return ipAddresses != null ? ipAddresses.equals(that.ipAddresses) : that.ipAddresses == null;
    }
}
