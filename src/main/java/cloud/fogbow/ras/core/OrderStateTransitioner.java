package cloud.fogbow.ras.core;

import cloud.fogbow.common.exceptions.RemoteCommunicationException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.linkedlists.SynchronizedDoublyLinkedList;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.intercomponent.xmpp.requesters.RemoteNotifyEventRequest;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import org.apache.log4j.Logger;

public class OrderStateTransitioner {
    private static final Logger LOGGER = Logger.getLogger(OrderStateTransitioner.class);

    public static void transition(Order order, OrderState newState) throws UnexpectedException {
        String localProviderId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.PROVIDER_ID_KEY);
        synchronized (order) {
            if (order.isRequesterRemote(localProviderId) && !newState.equals(OrderState.SELECTED)) {
                // We need to inform the remote requesting provider about changes on the local order states.
                // However, the transition to the SELECTED state is only used to implement an at-most-once
                // semantic at the local provider that makes the requestInstance() call, thus, does not need
                // to be notified to the remote requesting provider.
                try {
                    notifyRequester(order, newState);
                } catch (Exception e) {
                    // ToDO: Add orders that failed to be notified to a list of orders missing notification.
                    //  A new thread (MissedNotificationProcessor) will periodically retry these notifications.
                    // This list does not need to be in stable storage. Upon recovery (see the constructor of
                    // SharedOrdersHolder), all active orders (those not deactivated) whose requesters are remote
                    // should be added in the list of orders missing notification and be notified again (just in case).
                    // ClosedProcessor should inspect this list before deactivating an order whose requester is
                    // remote. Only orders that are not present in the list should be deactivated. Those that are
                    // present in the list should be kept in the CLOSED state, until they are successfully notified.
                    // Eventual garbage that remains due to a remote requester that never recovers (and cannot be
                    // notified) will be dealt with by the admin tool to be developed.
                    String message = String.format(Messages.Warn.UNABLE_TO_NOTIFY_REQUESTING_PROVIDER, order.getRequester(), order.getId());
                    LOGGER.warn(message, e);
                }
            }
            doTransition(order, newState);
        }
    }

    protected static void doTransition(Order order, OrderState newState) throws UnexpectedException {
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

    public static void notifyRequester(Order order, OrderState newState) throws RemoteCommunicationException {
        try {
            RemoteNotifyEventRequest remoteNotifyEventRequest = new RemoteNotifyEventRequest(order, newState);
            remoteNotifyEventRequest.send();
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new RemoteCommunicationException(e.getMessage(), e);
        }
    }
}
