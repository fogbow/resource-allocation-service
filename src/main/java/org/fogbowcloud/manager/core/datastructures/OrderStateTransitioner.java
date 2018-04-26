package org.fogbowcloud.manager.core.datastructures;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.models.linkedList.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;

public class OrderStateTransitioner {

    private static final Logger LOGGER = Logger.getLogger(OrderStateTransitioner.class);

    /**
     * Updates the state of the given order. This method is not
     * thread-safe. The caller must assure that only one
     * thread can call it at the same time.
     *
     *  @param order, the order that will transition through states
     * @param newState, the new state
     */
    public static boolean updateState(Order order, OrderState newState) {
        OrderState currentState = order.getOrderState();

        SynchronizedDoublyLinkedList origin = getOrdersList(currentState);
        SynchronizedDoublyLinkedList destination = getOrdersList(newState);

        if (origin == null) {
            LOGGER.error(String.format("Could not find origin list for state %s", currentState));
        } else if (destination == null) {
            LOGGER.error(String.format("Could not find destination list for state %s", newState));
        } else {
            if (origin.removeItem(order)) {
                order.setOrderState(newState);
                origin.addItem(order);
                return true;
            } else {
                String template = "Could not remove order id %s from list of %s orders";
                String message = String.format(template, order.getId(), currentState.toString());
                LOGGER.error(message);
            }
        }

        return false;
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
