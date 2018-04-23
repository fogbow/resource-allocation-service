package org.fogbowcloud.manager.core.models.orders;

public interface OrderRegistry {
	
	public Order getNextOrderByState(OrderState orderState);
	
	public boolean removeFromSpwaningOrders(Order order);
	
	public void insertIntoSpwaningOrders(Order order);
	
	public void updateOrder(Order order);
}
