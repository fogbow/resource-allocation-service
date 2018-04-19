package org.fogbowcloud.manager.core.models.orders;

public interface OrderRegistry {
	public Order getNextOpenOrder();
	
	public void updateOrder(Order order);
}
