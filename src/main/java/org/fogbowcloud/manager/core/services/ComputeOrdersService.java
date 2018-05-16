package org.fogbowcloud.manager.core.services;

import org.fogbowcloud.manager.core.datastructures.OrderStateTransitioner;
import org.fogbowcloud.manager.core.exceptions.OrderStateTransitionException;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.models.token.Token.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComputeOrdersService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ComputeOrdersService.class);
    
	public void deleteOrder(Order order, Token token) {
		if (isSameUser(order, token)) {
			synchronized (order) {
				if (!order.getOrderState().equals(OrderState.CLOSED)) {
					try {
						OrderStateTransitioner.transition(order, OrderState.CLOSED);
					} catch (OrderStateTransitionException e) {
						LOGGER.error("Error to try change status" + order.getOrderState() + " to closed for order [" + order.getId() + "]", e);
					}
				} else {
					LOGGER.info("Order [" + order.getId() + "] is not available to change state");
				}
			}
		}		
	}

	private boolean isSameUser(Order order, Token token) {
		Token localToken = order.getLocalToken();
		User orderLocalTokenUser = localToken.getUser();
		User tokenUserAuthenticated = token.getUser();
		if (orderLocalTokenUser.equals(tokenUserAuthenticated)) {
			return true;
		}
		return false;
	}

}
