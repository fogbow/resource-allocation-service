package org.fogbowcloud.manager.core.models.token;

import java.util.Map;

public class FederationUser {

    private String id;

    private Map<String, String> attributes;

    public FederationUser(String id, Map<String, String> attributes) {
        this.id = id;
        this.attributes = attributes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }
}
