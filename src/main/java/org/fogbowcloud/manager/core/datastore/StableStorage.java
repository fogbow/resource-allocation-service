package org.fogbowcloud.manager.core.datastore;

import org.fogbowcloud.manager.core.models.linkedlist.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;

public interface StableStorage {

    public void add(Order order);

    public void update(Order order);

    public SynchronizedDoublyLinkedList readActiveOrders(OrderState orderState);
}
