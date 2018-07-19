package org.fogbowcloud.manager.core.models.instances;

public class Instance {

    // TODO: the id should not be empty. Is necessary to check it in the constructor method.
    // TODO: check above comment; it seems that instead, we can get rid of this id attribute altogether.
    // Removing this attribute might require changes in the Attachment processing.
    private String id;
    private InstanceState state;
    private String provider;

    public Instance(String id) {
        this.id = id;
    }

    public Instance(String id, InstanceState state) {
        this(id);
        this.state = state;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public InstanceState getState() {
        return state;
    }

    public void setState(InstanceState state) {
        this.state = state;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    // TODO: add comment explaining why we need to override these methods
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Instance other = (Instance) obj;
        if (id == null) {
            if (other.id != null) return false;
        } else if (!id.equals(other.id)) return false;
        return true;
    }
}
