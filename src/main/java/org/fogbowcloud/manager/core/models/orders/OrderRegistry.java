package org.fogbowcloud.manager.core.models.orders;

public interface OrderRegistry {
	
	public Order getNextOpenOrder();
	
	public Order getNextToBeDeletedOrder();
	
	public void updateOrder(Order order);
}
