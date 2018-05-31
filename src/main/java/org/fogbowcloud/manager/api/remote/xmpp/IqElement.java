package org.fogbowcloud.manager.api.remote.xmpp;

public enum IqElement {

    QUERY("query"),
    ORDER("order"),
    ORDER_ID("orderId"),
    FEDERATION_USER("federationUser"),
    INSTANCE("instance"), 
    ORDER_TYPE("orderType"),
    ORDER_CLASS_NAME("orderClassName"), 
    INSTANCE_CLASS_NAME("instanceClassName");

    private final String element;

    IqElement(final String elementName) {
        this.element = elementName;
    }

    @Override
    public String toString() {
        return element;
    }
}
