package org.fogbowcloud.manager.core.services;

import org.fogbowcloud.manager.core.datastructures.OrderStateTransitioner;
import org.fogbowcloud.manager.core.exceptions.OrderStateTransitionException;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComputeOrdersService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ComputeOrdersService.class);

	public void deleteOrder(Order order) {
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
