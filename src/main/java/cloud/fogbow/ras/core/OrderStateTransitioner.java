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
        String localProviderId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_PROVIDER_ID_KEY);
        synchronized (order) {
            if (order.isRequesterRemote(localProviderId) && !newState.equals(OrderState.CLOSED)) {
                try {
                    // The delete request issued by the remote requesting provider that sent the local order to CLOSE
                    // is a synchronous event, thus it does not require an asynchronous notification to be sent to
                    // the remote requesting provider.
                    notifyRequester(order, newState);
                } catch (Exception e) {
                    String message = String.format(Messages.Warn.UNABLE_TO_NOTIFY_REQUESTING_PROVIDER, order.getRequester(), order.getId());
                    LOGGER.warn(message, e);
                    // Do not transition order to keep trying to notify until the site is up again.
                    // The site admin might want to monitor the warn log in case a site never
                    // recovers. In this case the site admin may delete the order using an
                    // appropriate tool.
                    return;
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

    protected static void notifyRequester(Order order, OrderState newState) throws RemoteCommunicationException {
        try {
            RemoteNotifyEventRequest remoteNotifyEventRequest = new RemoteNotifyEventRequest(order, newState);
            remoteNotifyEventRequest.send();
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new RemoteCommunicationException(e.getMessage(), e);
        }
    }
}
