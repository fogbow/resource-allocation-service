package org.fogbowcloud.ras.core.models.orders;

import com.fasterxml.jackson.annotation.JsonValue;

public enum NetworkAllocationMode {
    DYNAMIC("dynamic"),
    STATIC("static");

    private String value;

    NetworkAllocationMode(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
