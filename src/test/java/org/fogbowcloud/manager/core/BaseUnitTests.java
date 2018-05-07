package org.fogbowcloud.manager.core;

import org.fogbowcloud.manager.core.datastructures.SharedOrderHolders;
import org.fogbowcloud.manager.core.models.linkedList.ChainedList;
import org.fogbowcloud.manager.core.models.linkedList.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.junit.After;

public class BaseUnitTests {
	
	@After
	public void tearDown() {
		SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
		for (OrderState state : OrderState.values()) {
            SynchronizedDoublyLinkedList ordersList = sharedOrderHolders.getOrdersList(state);
            this.cleanList(ordersList);
        }
	}

	private void cleanList(ChainedList list) {
		list.resetPointer();
		Order order = null;
		do {
			order = list.getNext();
			if (order != null) {
				list.removeItem(order);
			}
		} while (order != null);
		list.resetPointer();
	}
}
