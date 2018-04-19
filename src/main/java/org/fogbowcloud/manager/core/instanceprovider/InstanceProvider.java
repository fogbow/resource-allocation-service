package org.fogbowcloud.manager.core.instanceprovider;

import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;

public interface InstanceProvider {
	
	public OrderInstance requestInstance(Order order);
	
	public void deleteInstance(Order order);
	
}
