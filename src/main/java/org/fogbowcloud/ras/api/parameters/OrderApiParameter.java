package org.fogbowcloud.ras.api.parameters;

import org.fogbowcloud.ras.core.models.orders.Order;

public abstract class OrderApiParameter<T extends Order> {

    private String providingMember;

    public String getProvidingMember() {
        return providingMember;
    }

    public abstract T getOrder();

}
