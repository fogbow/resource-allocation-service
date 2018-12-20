package org.fogbowcloud.ras.core.models;

public enum ResourceType {
    COMPUTE("compute"),
    NETWORK("network"),
    VOLUME("volume"),
    ATTACHMENT("attachment"),
    IMAGE("image"),
    PUBLIC_IP("publicIp"),
    SECURITY_RULE("securityRule"),
    CLOUD_NAMES("cloudNames"),
    GENERIC_REQUEST("genericRequest");

    private String value;

    private ResourceType(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
