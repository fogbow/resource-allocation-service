package org.fogbowcloud.manager.core.instanceprovider;

import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;

public class LocalInstanceProvider implements InstanceProvider {

    @Override
    public OrderInstance requestInstance(Order order) throws Exception {
        return null;
    }
}
