package org.fogbowcloud.manager.core.rest.services;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.fogbowcloud.manager.core.controllers.ApplicationController;
import org.fogbowcloud.manager.core.datastructures.SharedOrderHolders;
import org.fogbowcloud.manager.core.exceptions.OrdersServiceException;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.plugins.identity.exceptions.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class ComputeOrdersService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComputeOrdersService.class);

    private ApplicationController applicationController;
    private SharedOrderHolders ordersHolder;

    public ComputeOrdersService() {
        applicationController = ApplicationController.getInstance();
        ordersHolder = SharedOrderHolders.getInstance();
    }

    public ResponseEntity<Order> createCompute(ComputeOrder computeOrder, String accessId, String localTokenId) {
        try {
            Token federatedToken = applicationController.authenticate(accessId);
            Token localToken = createLocalToken(localTokenId);
            setOrderTokens(computeOrder, localToken, federatedToken);
            computeOrder.setOrderState(OrderState.OPEN);
            addOrderInActiveOrdersMap(computeOrder);
        } catch (Exception exception) { // change to catch exception for failedAuthentication
            String message = "It was not possible to create new ComputeOrder. " + exception.getMessage();
            LOGGER.error(message);
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        return new ResponseEntity<Order>(HttpStatus.OK);
    }
    
    public List<Order> getAllComputes(String accessId, String localTokenId) throws UnauthorizedException {
    	Token federatedToken = applicationController.authenticate(accessId);
    	Token localToken = createLocalToken(localTokenId);
    	Collection<Order> orders = this.ordersHolder.getActiveOrdersMap().values();
    	
    	return orders.stream()
    			.filter(order -> order.getType().equals(OrderType.COMPUTE))
    			.filter(order -> order.getFederationToken().equals(federatedToken) && order.getLocalToken().equals(localToken))
    			.collect(Collectors.toList());
    }

    public Order getOrderById(String id, String accessId, String localTokenId) throws UnauthorizedException {
    	Token federatedToken = applicationController.authenticate(accessId);
    	Token localToken = createLocalToken(localTokenId);
    	Collection<Order> orders = this.ordersHolder.getActiveOrdersMap().values();
    	
    	return orders.stream()
    			.filter(order -> order.getType().equals(OrderType.COMPUTE))
    			.filter(order -> order.getFederationToken().equals(federatedToken) && order.getLocalToken().equals(localToken))
    			.filter(order -> order.getId().equals(id))
    			.findFirst()
    			.get();
    }
    
    private Token createLocalToken(String localTokenId) {
        Token localToken = new Token();
        localToken.setAccessId(localTokenId);
        return localToken;
    }

    private void setOrderTokens(ComputeOrder computeOrder, Token localToken, Token federatedToken) {
        computeOrder.setFederationToken(federatedToken);
        computeOrder.setLocalToken(localToken);
    }

    private void addOrderInActiveOrdersMap(ComputeOrder computeOrder) throws OrdersServiceException {
        Map<String, Order> activeOrdersMap = ordersHolder.getActiveOrdersMap();

        if (activeOrdersMap.containsKey(computeOrder.getId())) {
            String message = String.format("Order with id %s is already in Map with active orders.", computeOrder.getId());
            throw new OrdersServiceException(message);
        }

        synchronized (computeOrder) {
            activeOrdersMap.put(computeOrder.getId(), computeOrder);
            ordersHolder.getOrdersList(OrderState.OPEN).addItem(computeOrder);
        }
    }
}
