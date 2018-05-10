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

    private SharedOrderHolders ordersHolder;

    public OrdersService() {
        ordersHolder = SharedOrderHolders.getInstance();
    }

    public void createOrder(Order order, Token federatedToken, Token localToken) throws OrdersServiceException, UnauthorizedException {
        // TODO para os headers
        if (order == null) {
            // TODO create msg
            String msg = "";
            throw new OrdersServiceException(msg);
        }
        setOrderTokens(order, localToken, federatedToken);
        order.setOrderState(OrderState.OPEN);
        addOrderInActiveOrdersMap(order);
    }

    private void setOrderTokens(Order order, Token federatedToken, Token localToken) {
        order.setFederationToken(federatedToken);
        order.setLocalToken(localToken);
    }

    public void addOrderInActiveOrdersMap(Order order) throws OrdersServiceException {
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
