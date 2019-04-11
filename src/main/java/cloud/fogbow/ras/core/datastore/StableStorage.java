package cloud.fogbow.ras.core.datastore;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.linkedlists.SynchronizedDoublyLinkedList;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;

public interface StableStorage {
    /**
     * Add the order to the stable storage, so that we can recovery it if necessary.
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
     * Retrive orders from the stable storage based on its state.
     *
     * @param orderState {@link OrderState}
     * @return {@link SynchronizedDoublyLinkedList}
     */
    SynchronizedDoublyLinkedList<Order> readActiveOrders(OrderState orderState) throws UnexpectedException;
}
