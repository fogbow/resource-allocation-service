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

}
