package org.fogbowcloud.manager.core.models.linkedlist;

import org.fogbowcloud.manager.core.models.orders.Order;

public interface ChainedList {

	public void addItem(Order order);
	
	public void resetPointer();
	
	public Order getNext();
	
	public boolean removeItem(Order order);
}
