package org.fogbowcloud.manager.core.instanceprovider;

import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.NetworkOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.StorageOrder;
import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.manager.plugins.compute.ComputePlugin;

public class LocalInstanceProvider implements InstanceProvider {

    private final ComputePlugin computePlugin;

    public LocalInstanceProvider(ComputePlugin computePlugin) {
        this.computePlugin = computePlugin;
    }

    @Override
    public String requestInstance(Order order) throws RequestException {
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

    private String requestInstance(ComputeOrder order) throws RequestException {
        return computePlugin.requestInstance(order, order.getImageName());
    }

    @Override
    public void deleteInstance(Token token, OrderInstance orderInstance) throws Exception {
        computePlugin.deleteInstance(token, orderInstance);
    }

    @Override
    public OrderInstance getInstance(Order order) throws RequestException {
        return computePlugin.getInstance(order.getLocalToken(), order.getOrderInstance().getId());
    }

}
