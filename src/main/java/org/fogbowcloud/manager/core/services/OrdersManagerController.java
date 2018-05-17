package org.fogbowcloud.manager.core.services;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.fogbowcloud.manager.core.datastructures.OrderStateTransitioner;
import org.fogbowcloud.manager.core.datastructures.SharedOrderHolders;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.exceptions.OrderStateTransitionException;
import org.fogbowcloud.manager.core.models.exceptions.RequestException;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.models.token.Token.User;
import org.fogbowcloud.manager.core.plugins.compute.ComputePlugin;
import org.fogbowcloud.manager.core.plugins.identity.exceptions.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrdersManagerController {

    @SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(OrdersManagerController.class);

    private SharedOrderHolders ordersHolder;
    private ComputePlugin computePlugin;

    public OrdersManagerController() {
        ordersHolder = SharedOrderHolders.getInstance();
    }
    
    public List<Order> getAllOrdersByType(Token federatedToken, OrderType orderType) throws UnauthorizedException {
    	Collection<Order> orders = this.ordersHolder.getActiveOrdersMap().values();
    	
    	List<Order> requestedOrders = orders.stream()
    			.filter(order -> order.getType().equals(orderType))
    			.filter(order -> order.getFederationToken().getUser().equals(federatedToken.getUser()))
    			.collect(Collectors.toList());
    	
    	return requestedOrders;
    }

    public Order getOrderByIdAndType(String id, Token federatedToken, OrderType orderType) throws UnauthorizedException {
    	Order requestedOrder = this.ordersHolder.getActiveOrdersMap().get(id);
    	
    	User orderWoner = requestedOrder.getFederationToken().getUser();
    	if (!orderWoner.equals(federatedToken.getUser()))
    		return null;
    	
    	if (!requestedOrder.getType().equals(orderType))
    		return null;
    	
    	if (requestedOrder.getOrderState().equals(OrderState.FULFILLED)) {
			try {
				// works only for compute order instance, because the compute plug-in is the only one available
				OrderInstance orderInstance = this.computePlugin.getInstance(requestedOrder.getLocalToken(), requestedOrder.getOrderInstance().getId());
				requestedOrder.setOrderInstance(orderInstance);
			} catch (RequestException e) {
				e.printStackTrace();
			}
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
        Map<String, Order> activeOrdersMap = ordersHolder.getActiveOrdersMap();

        if (activeOrdersMap.containsKey(order.getId())) {
            String message = String.format("Order with id %s is already in Map with active orders.", order.getId());
            throw new OrderManagementException(message);
        }

        synchronized (order) {
            activeOrdersMap.put(order.getId(), order);
            ordersHolder.getOrdersList(OrderState.OPEN).addItem(order);
        }
    }

}
