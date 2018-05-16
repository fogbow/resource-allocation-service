package org.fogbowcloud.manager.core.instanceprovider;

import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;

public interface InstanceProvider {

	/**
	 * Request an Instance for an Order.
	 * 
	 * @param order
	 * @return An OrderInstance with at least an nonempty Id.
	 * @throws Exception
	 *             If the Instance creation fail
	 */
	public OrderInstance requestInstance(Order order) throws Exception;

	public OrderInstance getInstance(Order order);

	public void deleteInstance(Token localToken, OrderInstance orderInstance) throws Exception;

}
