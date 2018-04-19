package org.fogbowcloud.manager.core;

import org.fogbowcloud.manager.core.models.orders.Order;

public interface QueueOrder {
	
	/**
	 * Inserts the Order into the queue.
	 * @param order
	 */
	public void offerOrder(Order order);
	
	/**
	 * Retrieves and remove Order into the queue by id.
	 * @param id
	 * @return Order
	 */
	public Order pollOrderById(Long id);

}
