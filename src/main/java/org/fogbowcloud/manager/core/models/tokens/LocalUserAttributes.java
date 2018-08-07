package org.fogbowcloud.manager.core.models.tokens;

import java.util.Map;

public class LocalUserAttributes {

    private Map<String, String> attributes;

    private String tokenValue;

    public LocalUserAttributes() {}

    public LocalUserAttributes(String tokenValue) {
        this.tokenValue = tokenValue;
    }

    public String getTokenValue() {
        return this.tokenValue;
    }

}
