package org.fogbowcloud.manager.api.remote.xmpp;

public enum IqElement {

    QUERY("query"),
    ORDER("order"),
    REMOTE_ORDER_ID("remoteOrderId"),
    FEDERATION_USER("federationUser"),
    INSTANCE("Instance");

    private final String element;

    IqElement(final String elementName) {
        this.element = elementName;
    }

    @Override
    public String toString() {
        return element;
    }
}
