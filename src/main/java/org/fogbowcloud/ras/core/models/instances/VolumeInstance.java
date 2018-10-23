package org.fogbowcloud.ras.core.models.instances;

public class VolumeInstance extends Instance {
    private String name;
    private int volumeSize;

    public VolumeInstance(String id, InstanceState instanceState, String name, int volumeSize) {
        super(id, instanceState);
        this.name = name;
        this.volumeSize = volumeSize;
    }

    public VolumeInstance(String id) {
        super(id);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getVolumeSize() {
        return this.volumeSize;
    }

    public void setVolumeSize(int volumeSize) {
        this.volumeSize = volumeSize;
    }
}
