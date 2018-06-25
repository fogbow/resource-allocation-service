package org.fogbowcloud.manager.core.models.orders;

import com.fasterxml.jackson.annotation.JsonValue;

public enum NetworkAllocation {
    DYNAMIC("dynamic"), STATIC("static");
    
    private String value;
    
    NetworkAllocation(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static NetworkAllocation fromValue(String value) {
        for (NetworkAllocation networkAllocation : NetworkAllocation.values()) {
            if (networkAllocation.getValue().equalsIgnoreCase(value)) {
                return networkAllocation;
            }
        }

        return null;
    }
}
