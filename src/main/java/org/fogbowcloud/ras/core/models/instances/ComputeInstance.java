package org.fogbowcloud.ras.core.models.instances;

import java.util.List;
import java.util.Map;

public class ComputeInstance extends Instance {
    private String name;
    private int vCPU;
    /**
     * Memory attribute, must be set in MB.
     */
    private int memory;
    /**
     * Disk attribute, must be set in GB.
     */
    private int disk;
    private List<String> ipAddresses;
    /**
     * Order-related properties
     */
    private Map<String, String> networks;
    private String imageId;
    private String publicKey;
    private String userData;

    public ComputeInstance(String id, InstanceState state, String name, int vCPU, int memory, int disk,
                           List<String> ipAddresses) {
        super(id, state);
        this.name = name;
        this.vCPU = vCPU;
        this.memory = memory;
        this.disk = disk;
        this.ipAddresses = ipAddresses;
    }

    public ComputeInstance(String id, InstanceState state, String name, int vCPU, int memory, int disk,
                           List<String> ipAddresses, String imageId, String publicKey, String userData) {
        super(id, state);
        this.name = name;
        this.vCPU = vCPU;
        this.memory = memory;
        this.disk = disk;
        this.ipAddresses = ipAddresses;
        this.imageId = imageId;
        this.publicKey = publicKey;
        this.userData = userData;
    }

    public ComputeInstance(String id) {
        super(id);
    }

    public int getDisk() {
        return this.disk;
    }

    public String getName() {
        return this.name;
    }

    public List<String> getIpAddresses() {
        return this.ipAddresses;
    }

    public int getMemory() {
        return this.memory;
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

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
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
        if (memory != that.memory) return false;
        if (disk != that.disk) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return ipAddresses != null ? ipAddresses.equals(that.ipAddresses) : that.ipAddresses == null;
    }
}
