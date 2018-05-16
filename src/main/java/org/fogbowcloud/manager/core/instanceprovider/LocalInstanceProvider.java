package org.fogbowcloud.manager.core.instanceprovider;

import org.fogbowcloud.manager.core.models.exceptions.RequestException;
import org.fogbowcloud.manager.core.models.orders.*;
import org.fogbowcloud.manager.core.models.orders.instances.InstanceState;
import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.plugins.compute.ComputePlugin;

public class LocalInstanceProvider implements InstanceProvider {

    private final ComputePlugin computePlugin;

    public LocalInstanceProvider(ComputePlugin computePlugin) {
        this.computePlugin = computePlugin;
    }

    @Override
    public OrderInstance requestInstance(Order order) throws RequestException {
        if (order instanceof ComputeOrder) {
            return requestInstance((ComputeOrder) order);
        } else if (order instanceof StorageOrder) {
            throw new UnsupportedOperationException("Not implemented yet.");
        } else if (order instanceof NetworkOrder) {
            throw new UnsupportedOperationException("Not implemented yet.");
        } else {
            throw new UnsupportedOperationException("Not implemented yet.");
        }
    }

    private OrderInstance requestInstance(ComputeOrder order) throws RequestException {
        String imageName = order.getImageName();
        String id = computePlugin.requestInstance(order, imageName);
        return new OrderInstance(id, InstanceState.ACTIVE);
    }

    @Override
    public void deleteInstance(Token token, OrderInstance orderInstance) throws Exception {
        computePlugin.deleteInstance(token, orderInstance);
    }

}
