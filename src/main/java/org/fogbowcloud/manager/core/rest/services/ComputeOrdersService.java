package org.fogbowcloud.manager.core.rest.services;

import org.fogbowcloud.manager.core.controllers.ApplicationController;
import org.fogbowcloud.manager.core.datastructures.SharedOrderHolders;
import org.fogbowcloud.manager.core.exceptions.ComputeOrdersServiceException;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.token.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;

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

        if (computeOrder == null) {
            return new ResponseEntity<Order>(HttpStatus.BAD_REQUEST);
        }
        try {
            Token federatedToken = applicationController.authenticate(accessId);
            Token localToken = createLocalToken(localTokenId);
            setOrderTokens(computeOrder, localToken, federatedToken);
            computeOrder.setOrderState(OrderState.OPEN);
            addOrderInActiveOrdersMap(computeOrder);
        } catch (Exception exception) { // change to catch exception for failedAuthentication
            String message = "It was not possible to create new ComputeOrder. " + exception.getMessage();
            LOGGER.error(message);
            return new ResponseEntity<Order>(HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<Order>(HttpStatus.CREATED);
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

    private void addOrderInActiveOrdersMap(ComputeOrder computeOrder) throws ComputeOrdersServiceException {
        Map<String, Order> activeOrdersMap = ordersHolder.getActiveOrdersMap();

        if (activeOrdersMap.containsKey(computeOrder.getId())) {
            String message = String.format("Order with id %s is already in Map with active orders.", computeOrder.getId());
            throw new ComputeOrdersServiceException(message);
        }

        synchronized (computeOrder) {
            activeOrdersMap.put(computeOrder.getId(), computeOrder);
            ordersHolder.getOrdersList(OrderState.OPEN).addItem(computeOrder);
        }
    }

}
