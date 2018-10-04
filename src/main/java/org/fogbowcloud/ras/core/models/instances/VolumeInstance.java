package org.fogbowcloud.ras.core.models.instances;

public class VolumeInstance extends Instance {
    private String name;
    private int size;

    public VolumeInstance(String id, InstanceState instanceState, String name, int size) {
        super(id, instanceState);
        this.name = name;
        this.size = size;
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

    public int getSize() {
        return this.size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
