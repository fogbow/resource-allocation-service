package org.fogbowcloud.manager.core.models.orders.instances;

public class VolumeOrderInstance extends OrderInstance {

    private String name;
    private int size;

    public VolumeOrderInstance(String id, String name, InstanceState state, int size) {
        super(id, state);
        this.name = name;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
