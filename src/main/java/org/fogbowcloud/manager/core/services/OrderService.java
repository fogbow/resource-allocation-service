package org.fogbowcloud.manager.core.services;

import org.fogbowcloud.manager.core.datastructures.OrderStateTransitioner;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.exceptions.OrderStateTransitionException;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.token.Token.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderService {

	private static final Logger LOGGER = LoggerFactory.getLogger(OrderService.class);
	
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

}
