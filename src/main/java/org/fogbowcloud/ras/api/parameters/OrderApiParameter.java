package org.fogbowcloud.ras.api.parameters;

import org.fogbowcloud.ras.core.models.orders.Order;

public interface OrderApiParameter<T extends Order> {

    T getOrder();

}
