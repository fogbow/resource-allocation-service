package org.fogbowcloud.manager.core.models.token;

import java.util.Map;

public class FederationUser {

    private Long id;

    private Map<String, String> attributes;

    public FederationUser(Long id, Map<String, String> attributes) {
        this.id = id;
        this.attributes = attributes;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }
}
