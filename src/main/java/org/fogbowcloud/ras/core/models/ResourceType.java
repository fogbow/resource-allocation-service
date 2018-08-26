package org.fogbowcloud.ras.core.models;

public enum ResourceType {
    COMPUTE("compute"),
    NETWORK("network"),
    VOLUME("volume"),
    ATTACHMENT("attachment"),
    IMAGE("image");

    private String value;

    private ResourceType(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
