package org.fogbowcloud.manager.core.models.instances;

public enum InstanceState {
    READY("ready"),
    INACTIVE("inactive"),
    SPAWNING("spawning"),
    FAILED("failed");

    private String value;

    private InstanceState(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
