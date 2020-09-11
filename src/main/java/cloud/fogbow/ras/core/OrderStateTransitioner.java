package cloud.fogbow.ras.core;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.models.linkedlists.SynchronizedDoublyLinkedList;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;

public class OrderStateTransitioner {

    /**
     * It will move the order to the RemoteProviderList with a specific state.
     * @param order - Order to be moved;
     * @param newStateOrder - New state to this order in the RemoteProviderList.
     */
    public static void transitionToRemoteList(Order order, OrderState newStateOrder) throws InternalServerErrorException {
        transition(order, OrderState.PENDING, newStateOrder);
    }

    /**
     * It will move the order to the newState List and with same newState.
     * @param order - Order to be moved;
     * @param newState - New state related to the List and the order state.
     * @throws InternalServerErrorException
     */
    public static void transition(Order order, OrderState newState) throws InternalServerErrorException {
        transition(order, newState, newState);
    }

    private static void transition(Order order, OrderState newStateList, OrderState newStateOrder) throws InternalServerErrorException {
        synchronized (order) {
            OrderState currentState = order.getOrderState();

            if (currentState == newStateList) {
                // The order may have already been moved to the new state by another thread
                // In this case, there is nothing else to be done
                return;
            }

            SharedOrderHolders ordersHolder = SharedOrderHolders.getInstance();
            SynchronizedDoublyLinkedList<Order> origin = ordersHolder.getOrdersList(currentState);
            SynchronizedDoublyLinkedList<Order> destination = ordersHolder.getOrdersList(newStateList);

            if (origin == null) {
                String message = String.format(Messages.Exception.UNABLE_TO_FIND_LIST_FOR_REQUESTS_S, currentState);
                throw new InternalServerErrorException(message);
            } else if (destination == null) {
                String message = String.format(Messages.Exception.UNABLE_TO_FIND_LIST_FOR_REQUESTS_S, newStateList);
                throw new InternalServerErrorException(message);
            } else {
                // The order may have already been removed from the origin list by another thread
                // In this case, there is nothing else to be done
                if (origin.removeItem(order)) {
                    order.setOrderState(newStateOrder);
                    destination.addItem(order);
                }
            }
        }
    }
    
}
