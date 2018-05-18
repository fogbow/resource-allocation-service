package org.fogbowcloud.manager.core;

import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.exceptions.OrderStateTransitionException;
import org.fogbowcloud.manager.core.models.linkedlist.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.models.token.Token.User;
import org.fogbowcloud.manager.core.manager.plugins.compute.ComputePlugin;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.requests.api.local.http.ComputeOrdersController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OrderController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ComputeOrdersController.class);

    private SharedOrderHolders orderHolders;
    private ComputePlugin computePlugin;

    public OrderController() {
        this.orderHolders = SharedOrderHolders.getInstance();
    }

    public List<Order> getAllOrders(User user, OrderType type) throws UnauthorizedException {
        Collection<Order> orders = this.orderHolders.getActiveOrdersMap().values();

        List<Order> requestedOrders = orders.stream()
                .filter(order -> order.getType().equals(type))
                .filter(order -> order.getUser().equals(user))
                .collect(Collectors.toList());

        return requestedOrders;
    }

    public Order getOrder(String id, User user, OrderType orderType) {
        Order requestedOrder = this.orderHolders.getActiveOrdersMap().get(id);

        // TODO Do we need to perform this check?
        // We might want to move this to the AuthorizationPlugin
        User orderOwner = requestedOrder.getUser();
        if (!orderOwner.equals(user)) {
            return null;
        }

        if (!requestedOrder.getType().equals(orderType)) {
            return null;
        }

        return requestedOrder;
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
 					LOGGER.error("This should never happen. Error to try change status" + order.getOrderState()
 							+ " to closed for order [" + order.getId() + "]", e);
 				}
 			} else {
                String message = "Order [" + order.getId() + "] is already in the closed state";
 				throw new OrderManagementException(message);
 			}
 		}
 	}

    public void activateOrder(Order order, Token federationToken) throws OrderManagementException {
        if (order == null) {
            String message = "Can't process new order request. Order reference is null.";
            throw new OrderManagementException(message);
        }

        order.setFederationToken(federationToken);
        addOrderInActiveOrdersMap(order);
    }

    private void addOrderInActiveOrdersMap(Order order) throws OrderManagementException {
        String orderId = order.getId();
        Map<String, Order> activeOrdersMap = this.orderHolders.getActiveOrdersMap();
        SynchronizedDoublyLinkedList openOrdersList = this.orderHolders.getOpenOrdersList();

        synchronized (order) {
            if (activeOrdersMap.containsKey(orderId)) {
                String message = String.format("Order with id %s is already in active orders map.", orderId);
                throw new OrderManagementException(message);
            }

            order.setOrderState(OrderState.OPEN);
            activeOrdersMap.put(orderId, order);
            openOrdersList.addItem(order);
        }
    }

}
