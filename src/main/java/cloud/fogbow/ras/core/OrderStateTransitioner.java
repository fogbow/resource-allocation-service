package cloud.fogbow.ras.core;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.linkedlists.SynchronizedDoublyLinkedList;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;

public class OrderStateTransitioner {

    public static void transition(Order order, OrderState newState) throws UnexpectedException {
        synchronized (order) {
            OrderState currentState = order.getOrderState();

            if (currentState == newState) {
                // The order may have already been moved to the new state by another thread
                // In this case, there is nothing else to be done
                return;
            }

            SharedOrderHolders ordersHolder = SharedOrderHolders.getInstance();
            SynchronizedDoublyLinkedList<Order> origin = ordersHolder.getOrdersList(currentState);
            SynchronizedDoublyLinkedList<Order> destination = ordersHolder.getOrdersList(newState);

            if (origin == null) {
                String message = String.format(Messages.Exception.UNABLE_TO_FIND_LIST_FOR_REQUESTS, currentState);
                throw new UnexpectedException(message);
            } else if (destination == null) {
                String message = String.format(Messages.Exception.UNABLE_TO_FIND_LIST_FOR_REQUESTS, newState);
                throw new UnexpectedException(message);
            } else {
                // The order may have already been removed from the origin list by another thread
                // In this case, there is nothing else to be done
                if (origin.removeItem(order)) {
                    order.setOrderState(newState);
                    destination.addItem(order);
                }
            }
        }
    }
    
}
