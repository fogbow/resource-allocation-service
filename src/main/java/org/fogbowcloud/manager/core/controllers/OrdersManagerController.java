package org.fogbowcloud.manager.core.controllers;

import org.fogbowcloud.manager.core.datastructures.SharedOrderHolders;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.models.linkedList.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.token.Token;

import java.util.Map;

public class OrdersManagerController {

    private SharedOrderHolders ordersHolder;

    public OrdersManagerController() {
        this.ordersHolder = SharedOrderHolders.getInstance();
    }

    public void newOrderRequest(Order order, Token federationToken, Token localToken) throws OrderManagementException {
        setOrderTokens(order, localToken, federationToken);
        addOrderInActiveOrdersMap(order);
    }

    public void addOrderInActiveOrdersMap(Order order) throws OrderManagementException {
        String orderId = order.getId();
        Map<String, Order> activeOrdersMap = this.ordersHolder.getActiveOrdersMap();
        SynchronizedDoublyLinkedList openOrdersList = this.ordersHolder.getOpenOrdersList();

        if (activeOrdersMap.containsKey(orderId)) {
            String message = String.format("Order with id %s is already in active orders map.", orderId);
            throw new OrderManagementException(message);
        }
        synchronized (order) {
            activeOrdersMap.put(orderId, order);
            openOrdersList.addItem(order);
        }
    }

    private void setOrderTokens(Order order, Token federationToken, Token localToken) {
        order.setFederationToken(federationToken);
        order.setLocalToken(localToken);
    }
}
