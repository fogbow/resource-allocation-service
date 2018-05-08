package org.fogbowcloud.manager.core.models;

public enum RequestHeaders {

    CONTENT_TYPE("Content-Type"), ACCEPT("Accept"), X_AUTH_TOKEN("X-Auth-Token"), JSON_CONTENT_TYPE("application/json");

    private String value;

    private RequestHeaders(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
