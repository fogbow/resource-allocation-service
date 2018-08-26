package org.fogbowcloud.ras.core.models;

import org.fogbowcloud.ras.core.models.instances.InstanceState;

public class InstanceStatus {
    private String instanceId;
    private String provider;
    private InstanceState state;

    public InstanceStatus(String instanceId, String provider, InstanceState state) {
        this.instanceId = instanceId;
        this.provider = provider;
        this.state = state;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getProvider() {
        return provider;
    }

    public InstanceState getState() {
        return state;
    }

    public void setState(InstanceState state) {
        this.state = state;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.instanceId == null) ? 0 : this.instanceId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        InstanceStatus other = (InstanceStatus) obj;
        if (this.instanceId == null) {
            if (other.getInstanceId() != null) return false;
        } else if (!this.instanceId.equals(other.getInstanceId())) return false;
        return true;
    }
}
