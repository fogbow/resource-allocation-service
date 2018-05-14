package org.fogbowcloud.manager.core.services;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.fogbowcloud.manager.core.datastructures.SharedOrderHolders;
import org.fogbowcloud.manager.core.exceptions.OrdersServiceException;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.plugins.identity.exceptions.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    
    public List<Order> getAllComputes(Token federatedToken) throws UnauthorizedException {
    	Collection<Order> orders = this.ordersHolder.getActiveOrdersMap().values();
    	
    	return orders.stream()
    			.filter(order -> order.getType().equals(OrderType.COMPUTE))
    			.filter(order -> order.getFederationToken().getUser().equals(federatedToken.getUser()))
    			.collect(Collectors.toList());
    }

    public Order getOrderById(String id, Token federatedToken) throws UnauthorizedException {
    	Collection<Order> orders = this.ordersHolder.getActiveOrdersMap().values();
    	
    	return orders.stream()
    			.filter(order -> order.getType().equals(OrderType.COMPUTE))
    			.filter(order -> order.getFederationToken().equals(federatedToken))
    			.filter(order -> order.getId().equals(id))
    			.findFirst()
    			.orElse(null);  // returns null if not exists the order.
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
