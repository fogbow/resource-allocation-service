package org.fogbowcloud.manager.core.models.orders.instances;

public class VolumeInstance extends Instance {

    private String name;
    private int size;

    public VolumeInstance(String id, String name, InstanceState state, int size) {
        super(id, state);
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
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
