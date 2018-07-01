package org.fogbowcloud.manager.core.models.instances;

public class VolumeInstance extends Instance {

    private String name;
    private int size;

    public VolumeInstance(String id, InstanceState state, String name, int size) {
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
}
