package org.fogbowcloud.manager.api.remote.xmpp;

public enum IqElement {

    QUERY("query"),
    ORDER("order");

    private final String element;

    IqElement(final String elementName) {
        this.element = elementName;
    }

    @Override
    public String toString() {
        return element;
    }
}
