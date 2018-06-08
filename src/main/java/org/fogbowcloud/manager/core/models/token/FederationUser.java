package org.fogbowcloud.manager.core.models.token;

import java.util.Map;

public class FederationUser {

    public static String MANDATORY_NAME_ATTRIBUTE = "user-name";

    private String id;

    private Map<String, String> attributes;

    public FederationUser(String id, Map<String, String> attributes) {
        this.id = id;
        this.attributes = attributes;
// TODO uncomment this code when breaking everything is affordable
//        if (this.attributes.get(MANDATORY_NAME_ATTRIBUTE) == null) {
//            throw new Exception();
//        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return this.attributes.get(MANDATORY_NAME_ATTRIBUTE);
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }
}
