package org.fogbowcloud.ras.core.models.instances;

public enum InstanceState {
    DISPATCHED("dispatched"),
    READY("ready"),
    INACTIVE("inactive"),
    SPAWNING("spawning"),
    CREATING("creating"),
    ATTACHING("attaching"),
    IN_USE("in-use"),
    UNAVAILABLE("unavailable"),
    FAILED("failed"),
    INCONSISTENT("inconsistent");

    private String value;

    private InstanceState(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
