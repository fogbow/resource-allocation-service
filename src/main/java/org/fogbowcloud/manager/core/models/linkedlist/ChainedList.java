package org.fogbowcloud.manager.core.models.linkedlist;

import org.fogbowcloud.manager.core.models.orders.Order;

public interface ChainedList {

	public void addItem(Order order);
	
	public void resetPointer() throws Exception;
	
	public Order getNext() throws Exception;
	
	public void removeItem(Order order) throws Exception;
	
}
