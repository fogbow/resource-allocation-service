package org.fogbowcloud.manager.core.instanceprovider;

import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import org.fogbowcloud.manager.core.manager.plugins.compute.ComputePlugin;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.NetworkOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.StorageOrder;
import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.services.AAAController;

public class LocalInstanceProvider implements InstanceProvider {

    private final ComputePlugin computePlugin;
    private final AAAController aaaController;

    public LocalInstanceProvider(ComputePlugin computePlugin, AAAController aaaController) {
        this.computePlugin = computePlugin;
        this.aaaController = aaaController;
    }

    @Override
    public String requestInstance(Order order)
            throws UnauthenticatedException, TokenCreationException, RequestException,
                    PropertyNotSpecifiedException, UnauthorizedException {
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

    private String requestInstance(ComputeOrder order)
            throws RequestException, TokenCreationException, UnauthorizedException,
                    UnauthenticatedException, PropertyNotSpecifiedException {

        Token localToken = this.aaaController.getLocalToken();
        return this.computePlugin.requestInstance(order, localToken, order.getImageName());
    }

    @Override
    public void deleteInstance(OrderInstance orderInstance) throws Exception {
        Token localToken = this.aaaController.getLocalToken();
        this.computePlugin.deleteInstance(localToken, orderInstance);
    }

    @Override
    public OrderInstance getInstance(Order order)
            throws RequestException, TokenCreationException, UnauthorizedException,
                    PropertyNotSpecifiedException {
        Token localToken = this.aaaController.getLocalToken();
        String orderInstanceId = order.getOrderInstance().getId();
        return this.computePlugin.getInstance(localToken, orderInstanceId);
    }
}
