package org.fogbowcloud.manager.core.models.orders.instances;

public class StorageOrderInstance extends OrderInstance {

    private String name;
    private InstanceState state;
    private int size;

    public StorageOrderInstance(String id, String name, InstanceState state, int size) {
        super(id);
        this.name = name;
        this.state = state;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public InstanceState getState() {
        return state;
    }

    public void setState(InstanceState state) {
        this.state = state;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
