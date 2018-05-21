package org.fogbowcloud.manager.core.instanceprovider;

import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;

public interface InstanceProvider {

	/**
	 * Requests an Instance for a Order.
	 * 
	 * @param order
	 * @return An OrderInstance with at least an nonempty Id.
	 * @throws Exception
	 *             If the Instance creation fail
	 */
	public String requestInstance(Order order) throws Exception;

	/**
	 * Signals the cloud that the provided instance is no longer required.
	 *
	 * @param orderInstance
	 * @throws Exception
	 * 			   if a failure occurred when requesting the deletion of an instance
	 */
	public void deleteInstance(OrderInstance orderInstance) throws Exception;

	/**
	 * Gets the instance currently associated for the provided order.
	 *
	 * @param order
	 * @return
	 */
	public OrderInstance getInstance(Order order) throws Exception;

}
