package org.fogbowcloud.manager.core.services;

import org.fogbowcloud.manager.core.controllers.ApplicationController;
import org.fogbowcloud.manager.core.datastructures.SharedOrderHolders;
import org.fogbowcloud.manager.core.exceptions.OrdersServiceException;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.plugins.identity.exceptions.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Map;

public class OrdersService {

    // TODO: usar log ao longo do código
    private static final Logger LOGGER = LoggerFactory.getLogger(OrdersService.class);

    private ApplicationController applicationController;
    private SharedOrderHolders ordersHolder;

    public OrdersService() {
        applicationController = ApplicationController.getInstance();
        ordersHolder = SharedOrderHolders.getInstance();
    }

    public void createOrder(Order order, String accessId, String localTokenId) throws OrdersServiceException, UnauthorizedException {
        // TODO para os headers
        if (order == null) {
            // TODO create msg
            String msg = "";
            throw new OrdersServiceException(msg);
        }
        Token federatedToken = applicationController.authenticate(accessId);
        Token localToken = createLocalToken(localTokenId);
        setOrderTokens(order, localToken, federatedToken);
        order.setOrderState(OrderState.OPEN);
        addOrderInActiveOrdersMap(order);
    }

    private Token createLocalToken(String localTokenId) {
        Token localToken = new Token();
        localToken.setAccessId(localTokenId);
        return localToken;
    }

    private void setOrderTokens(Order order, Token localToken, Token federatedToken) {
        order.setFederationToken(federatedToken);
        order.setLocalToken(localToken);
    }

    protected void addOrderInActiveOrdersMap(Order order) throws OrdersServiceException {
        Map<String, Order> activeOrdersMap = ordersHolder.getActiveOrdersMap();

        if (activeOrdersMap.containsKey(order.getId())) {
            String message = String.format("Order with id %s is already in Map with active orders.", order.getId());
            throw new OrdersServiceException(message);
        }

        synchronized (order) {
            activeOrdersMap.put(order.getId(), order);
            ordersHolder.getOrdersList(OrderState.OPEN).addItem(order);
        }
    }

}
