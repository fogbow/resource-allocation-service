package org.fogbowcloud.manager.api.remote;

import org.fogbowcloud.manager.core.OrderController;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.manager.constants.Operation;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.orders.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.orders.instances.Instance;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.fogbowcloud.manager.core.services.AAAController;

public class RemoteFacade {

    private static RemoteFacade instance;

    private AAAController aaaController;
    private OrderController orderController;

    public static RemoteFacade getInstance() {
        synchronized (RemoteFacade.class) {
            if (instance == null) {
                instance = new RemoteFacade();
            }
            return instance;
        }
    }

    public void createCompute(ComputeOrder order) throws UnauthorizedException, OrderManagementException {
        activateOrder(order);
    }

    public ComputeInstance getCompute(String orderId, FederationUser federationUser) throws UnauthenticatedException,
            PropertyNotSpecifiedException, RequestException, InstanceNotFoundException, TokenCreationException, UnauthorizedException {

        return (ComputeInstance) getResourceInstance(orderId, federationUser,
                OrderType.COMPUTE);
    }

    public void deleteCompute(String computeId, FederationUser federationUser)
            throws OrderManagementException, UnauthorizedException, UnauthenticatedException {
        deleteOrder(computeId, federationUser, OrderType.COMPUTE);
    }

    private void activateOrder(Order order) throws UnauthorizedException, OrderManagementException {
        this.aaaController.authorize(order.getFederationUser(), Operation.CREATE, order);
        this.orderController.activateOrder(order, order.getFederationUser());
    }

    private Instance getResourceInstance(String orderId, FederationUser federationUser,
                                         OrderType type) throws UnauthorizedException, PropertyNotSpecifiedException,
            TokenCreationException, RequestException, InstanceNotFoundException {

        Order order = this.orderController.getOrder(orderId, federationUser, type);
        this.aaaController.authorize(federationUser, Operation.GET, order);

        return this.orderController.getResourceInstance(order);
    }

    private void deleteOrder(String orderId, FederationUser federationUser, OrderType orderType)
            throws UnauthenticatedException, UnauthorizedException, OrderManagementException {
        Order order = this.orderController.getOrder(orderId, federationUser, orderType);
        this.aaaController.authorize(federationUser, Operation.DELETE, order);

        this.orderController.deleteOrder(order);
    }

    public void setAAAController(AAAController aaaController) {
        this.aaaController = aaaController;
    }

    public void setOrderController(OrderController orderController) {
        this.orderController = orderController;
    }

}
