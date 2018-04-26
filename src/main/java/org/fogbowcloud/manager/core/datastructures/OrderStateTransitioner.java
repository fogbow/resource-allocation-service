package org.fogbowcloud.manager.core.datastructures;

import org.fogbowcloud.manager.core.exceptions.OrderStateTransitionException;
import org.fogbowcloud.manager.core.models.linkedList.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;

public class OrderStateTransitioner {

    /**
     * Updates the state of the given order. This method is not
     * thread-safe, the caller must assure that only one
     * thread can call it at the same time. This can be done by creating
     * a synchronized block on the order object that is passed as parameter
     * to transition(). The reason the synchronized block is created in the
     * code calling this method is because, usually the order will be inspected
     * before the call to transition() is made, and this needs to be done within
     * a synchronized block, since several threads may attempt to process the
     * same order at the same time.
     *
     * @param order, the order that will transition through states
     * @param newState, the new state
     */
    public static void transition(Order order, OrderState newState) throws OrderStateTransitionException {
        OrderState currentState = order.getOrderState();

        SynchronizedDoublyLinkedList list = getOrdersList(currentState);
        SynchronizedDoublyLinkedList destination = getOrdersList(newState);

        if (list == null) {
            String message = String.format("Could not find list list for state %s", currentState);
            throw new OrderStateTransitionException(message);
        } else if (destination == null) {
            String message = String.format("Could not find destination list for state %s", newState);
            throw new OrderStateTransitionException(message);
        } else {
            if (list.removeItem(order)) {
                order.setOrderState(newState);
                destination.addItem(order);
                return;
            } else {
                String template = "Could not remove order id %s from list of %s orders";
                String message = String.format(template, order.getId(), currentState.toString());
                throw new OrderStateTransitionException(message);
            }
        }
    }

    protected static SynchronizedDoublyLinkedList getOrdersList(OrderState orderState) {
        SynchronizedDoublyLinkedList origin = null;
        switch (orderState) {
            case OPEN:
                origin = SharedOrderHolders.getInstance().getOpenOrdersList();
                break;
            case SPAWNING:
                origin = SharedOrderHolders.getInstance().getSpawningOrdersList();
                break;
            case PENDING:
                origin = SharedOrderHolders.getInstance().getPendingOrdersList();
                break;
            case FULFILLED:
                origin = SharedOrderHolders.getInstance().getFulfilledOrdersList();
                break;
            case CLOSED:
                origin = SharedOrderHolders.getInstance().getClosedOrdersList();
                break;
            case FAILED:
                origin = SharedOrderHolders.getInstance().getFailedOrdersList();
                break;
            default:
                break;
        }

        return origin;
    }

}
