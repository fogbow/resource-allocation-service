package org.fogbowcloud.manager.core.models.instances;

public enum InstanceType {
    COMPUTE("compute"),
    NETWORK("network"),
    VOLUME("volume"),
    ATTACHMENT("attachment");

    private String value;

    private InstanceType(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
