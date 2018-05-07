package org.fogbowcloud.manager.core.rest.services;

import org.fogbowcloud.manager.core.controllers.ApplicationController;
import org.fogbowcloud.manager.core.datastructures.SharedOrderHolders;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.token.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class ComputeOrdersService {

    private ApplicationController applicationController = ApplicationController.getInstance();
    private static final Logger LOGGER = LoggerFactory.getLogger(ComputeOrdersService.class);

    public ComputeOrdersService() { }

    public ResponseEntity<Order> createCompute(ComputeOrder computeOrder, String accessId, String localTokenId) {
        try {
            Token federatedToken = applicationController.authenticate(accessId);
            Token localToken = createLocalToken(localTokenId);
            setOrderTokens(computeOrder, localToken, federatedToken);
            computeOrder.setOrderState(OrderState.OPEN);
            addOrderInActiveOrdersMap(computeOrder);
        } catch (Exception exception) { // tratar para pegar exatamente a exceptio de failedAuthentication
            String message = "It was not possible to create new ComputeOrder. " + exception.getMessage();
            LOGGER.error(message);
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        return new ResponseEntity<Order>(HttpStatus.OK);
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

    private void addOrderInActiveOrdersMap(ComputeOrder computeOrder) {
        SharedOrderHolders.getInstance().getOpenOrdersList().addItem(computeOrder); // change to use OrderStateTransitioner
    }

}

