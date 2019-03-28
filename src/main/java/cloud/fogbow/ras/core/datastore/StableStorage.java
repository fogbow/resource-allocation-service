package cloud.fogbow.ras.core.datastore;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.linkedlists.SynchronizedDoublyLinkedList;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;

public interface StableStorage {
    /**
     * Add the order into database, so we can recovery it when necessary.
     *
     * @param order {@link Order}
     */
    void add(Order order) throws UnexpectedException;

    /**
     * Update the order in the stable storage
     *
     * @param order {@link Order}
     * @param stateChange this should be true if the order state was changed.
     */
    void update(Order order, boolean stateChange) throws UnexpectedException;

    /**
     * Retrive orders from the database based on its state.
     *
     * @param orderState {@link OrderState}
     * @return {@link SynchronizedDoublyLinkedList}
     */
    SynchronizedDoublyLinkedList<Order> readActiveOrders(OrderState orderState) throws UnexpectedException;
}
