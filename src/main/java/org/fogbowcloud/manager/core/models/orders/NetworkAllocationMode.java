package org.fogbowcloud.manager.core.models.orders;

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

    public static NetworkAllocationMode fromValue(String value) {
        for (NetworkAllocationMode networkAllocation : NetworkAllocationMode.values()) {
            if (networkAllocation.getValue().equalsIgnoreCase(value)) {
                return networkAllocation;
            }
        }

        return null;
    }
}
