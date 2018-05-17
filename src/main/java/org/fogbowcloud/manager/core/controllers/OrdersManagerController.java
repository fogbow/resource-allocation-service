package org.fogbowcloud.manager.core.controllers;

import org.fogbowcloud.manager.core.datastructures.OrderStateTransitioner;
import org.fogbowcloud.manager.core.datastructures.SharedOrderHolders;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.exceptions.OrderStateTransitionException;
import org.fogbowcloud.manager.core.models.linkedList.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.models.token.Token.User;
import org.fogbowcloud.manager.core.rest.controllers.ComputeOrdersController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class OrdersManagerController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ComputeOrdersController.class);

    private SharedOrderHolders ordersHolder;

    public OrdersManagerController() {
        this.ordersHolder = SharedOrderHolders.getInstance();
    }
    
 // TODO:
 	public Order getOrderByIdAndType(User user, String orderId, OrderType orderType) {
 		throw new UnsupportedOperationException("Not implemented yet");
 	}

 	public void deleteOrder(Order order) throws OrderManagementException {
 		if (order == null) {
 			String message = "Cannot delete a null order";
 			throw new OrderManagementException(message);
 		}
 		synchronized (order) {
 			OrderState orderState = order.getOrderState(); 
 			if (!orderState.equals(OrderState.CLOSED)) {
 				try {
 					OrderStateTransitioner.transition(order, OrderState.CLOSED);
 				} catch (OrderStateTransitionException e) {
 					LOGGER.error("Error to try change status" + order.getOrderState()
 							+ " to closed for order [" + order.getId() + "]", e);
 				}
 			} else {
 				LOGGER.info("Order [" + order.getId() + "] is already in the closed state");
 			}
 		}
 	}

    public void newOrderRequest(Order order, Token localToken, Token federationToken) throws OrderManagementException {
        if (order == null) {
            String message = "Can't process new order request. Order reference is null.";
            throw new OrderManagementException(message);
        }
        order.setFederationToken(federationToken);
        order.setLocalToken(localToken);
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
}
