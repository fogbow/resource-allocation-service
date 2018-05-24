package org.fogbowcloud.manager.core.models.orders;

public enum NetworkAllocation {
    DYNAMIC("dynamic"), STATIC("static");
    
    private String value;
    
    NetworkAllocation(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
}
