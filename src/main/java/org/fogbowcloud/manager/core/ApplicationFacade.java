package org.fogbowcloud.manager.core;

import java.util.ArrayList;
import java.util.List;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import org.fogbowcloud.manager.core.manager.constants.Operation;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.services.AAAController;

public class ApplicationFacade {

    private static ApplicationFacade instance;

    private AAAController aaaController;
    private OrderController orderController;

    private ApplicationFacade() {}

    public static ApplicationFacade getInstance() {
        synchronized (ApplicationFacade.class) {
            if (instance == null) {
                instance = new ApplicationFacade();
            }
            return instance;
        }
    }

    public void deleteCompute(String computeId, String federationTokenValue) throws Exception {
        deleteOrder(computeId, federationTokenValue, OrderType.COMPUTE);
    }

    private void deleteOrder(String orderId, String federationTokenValue, OrderType orderType)
            throws Exception {
        this.aaaController.authenticate(federationTokenValue);

        Token federationToken = this.aaaController.getFederationToken(federationTokenValue);
        Order order = this.orderController.getOrder(orderId, federationToken.getUser(), orderType);
        this.aaaController.authorize(federationToken, Operation.DELETE, order);

        this.orderController.deleteOrder(order);
    }

    public List<ComputeOrder> getAllComputes(String federationTokenValue)
            throws UnauthorizedException, UnauthenticatedException {
        List<ComputeOrder> computeOrders = new ArrayList<ComputeOrder>();

        // TODO is there a better way to do this?
        List<Order> allOrders = getAllOrders(federationTokenValue, OrderType.COMPUTE);
        for (Order order : allOrders) {
            computeOrders.add((ComputeOrder) order);
        }
        return computeOrders;
    }

    private List<Order> getAllOrders(String federationTokenValue, OrderType orderType)
            throws UnauthorizedException, UnauthenticatedException {
        this.aaaController.authenticate(federationTokenValue);
        Token federationToken = this.aaaController.getFederationToken(federationTokenValue);
        this.aaaController.authorize(federationToken, Operation.GET_ALL, orderType);

        return this.orderController.getAllOrders(federationToken.getUser(), orderType);
    }

    public ComputeOrder getCompute(String computeId, String federationTokenValue) throws Exception {
        return (ComputeOrder) getOrder(computeId, federationTokenValue, OrderType.COMPUTE);
    }

    private Order getOrder(String id, String federationTokenValue, OrderType type)
            throws Exception {
        this.aaaController.authenticate(federationTokenValue);
        Token federationToken = this.aaaController.getFederationToken(federationTokenValue);

        Order order = this.orderController.getOrder(id, federationToken.getUser(), type);
        this.aaaController.authorize(federationToken, Operation.GET, order);

        return order;
    }

    public void createCompute(ComputeOrder order, String federationTokenValue)
            throws UnauthenticatedException, UnauthorizedException, OrderManagementException {
        activateOrder(order, federationTokenValue, OrderType.COMPUTE);
    }

    private void activateOrder(Order order, String federationTokenValue, OrderType type)
            throws OrderManagementException, UnauthorizedException, UnauthenticatedException {
        this.aaaController.authenticate(federationTokenValue);
        Token federationToken = this.aaaController.getFederationToken(federationTokenValue);
        this.aaaController.authorize(federationToken, Operation.CREATE, type);

        this.orderController.activateOrder(order, federationToken);
    }

    public void setAAAController(AAAController aaaController) {
        this.aaaController = aaaController;
    }

    public void setOrderController(OrderController orderController) {
        this.orderController = orderController;
    }
}
