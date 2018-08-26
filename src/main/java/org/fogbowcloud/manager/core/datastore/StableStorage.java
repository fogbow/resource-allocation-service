package org.fogbowcloud.manager.core.datastore;

import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.linkedlists.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;

public interface StableStorage {
    /**
     * Add the order into database, so we can recovery it when necessary.
     *
     * @param order {@link Order}
     */
    public void add(Order order) throws UnexpectedException;

    /**
     * Update the order when trasitions occur.
     *
     * @param order {@link Order}
     */
    public void update(Order order) throws UnexpectedException;

    /**
     * Retrive orders from the database based on its state.
     *
     * @param orderState {@link OrderState}
     * @return {@link SynchronizedDoublyLinkedList}
     */
    public SynchronizedDoublyLinkedList readActiveOrders(OrderState orderState) throws UnexpectedException;
}
