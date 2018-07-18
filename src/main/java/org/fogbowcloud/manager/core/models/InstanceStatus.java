package org.fogbowcloud.manager.core.models;

import org.fogbowcloud.manager.core.models.instances.InstanceState;

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

    public InstanceState getState() {
        return state;
    }

    public void setState(InstanceState state) {
        this.state = state;
    }
}
