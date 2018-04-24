package org.fogbowcloud.manager.core.models.orders;

public interface OrderRegistry {
	
	public Order getNextOrderByState(OrderState orderState);
	
	public void updateOrder(Order order);
}
