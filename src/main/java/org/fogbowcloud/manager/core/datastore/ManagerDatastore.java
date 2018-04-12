package org.fogbowcloud.manager.core.datastore;

import java.util.List;

import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;

public interface ManagerDatastore {
	
	public void addOrder(Order order);
	
	public List<Order> getAllOrders();
	
	public List<Order> getOrderByState(OrderState orderState);
	
	public void updateOrder(Order order);
}
