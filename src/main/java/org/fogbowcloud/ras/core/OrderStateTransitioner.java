package org.fogbowcloud.ras.core;

import java.util.Map;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.intercomponent.xmpp.Event;
import org.fogbowcloud.ras.core.intercomponent.xmpp.requesters.RemoteNotifyEventRequest;
import org.fogbowcloud.ras.core.models.linkedlists.ChainedList;
import org.fogbowcloud.ras.core.models.linkedlists.SynchronizedDoublyLinkedList;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.orders.OrderState;

public class OrderStateTransitioner {
    private static final Logger LOGGER = Logger.getLogger(OrderStateTransitioner.class);

    public static void activateOrder(Order order) throws UnexpectedException {
        LOGGER.info(Messages.Info.ACTIVATING_NEW_ORDER);
        
        if (order == null) {
            throw new UnexpectedException(Messages.Exception.CANNOT_PROCESS_ORDER_REQUEST_NULL);
        }

        synchronized (order) {
            SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
            Map<String, Order> activeOrdersMap = sharedOrderHolders.getActiveOrdersMap();
            ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();

            String orderId = order.getId();

            if (activeOrdersMap.containsKey(orderId)) {
                String message = String.format(Messages.Exception.ORDER_ID_ALREADY_ACTIVATED, orderId);
                throw new UnexpectedException(message);
            }
            order.setOrderState(OrderState.OPEN);
            activeOrdersMap.put(orderId, order);
            openOrdersList.addItem(order);
        }
    }

    public static void transition(Order order, OrderState newState) throws UnexpectedException {
        String localMemberId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);
        synchronized (order) {
            if (order.isRequesterRemote(localMemberId)) {
                try {
                    switch (newState) {
                        case FAILED:
                            notifyRequester(order, Event.INSTANCE_FAILED);
                            newState = OrderState.CLOSED;
                            break;
                        case FULFILLED:
                            notifyRequester(order, Event.INSTANCE_FULFILLED);
                            break;
                    }
                } catch (Exception e) {
                    String message = String.format(Messages.Warn.COULD_NOT_NOTIFY_REQUESTING_MEMBER, order.getRequestingMember(), order.getId());
                	LOGGER.warn(message);
                    // Keep trying to notify until the site is up again
                    // The site admin might want to monitor the warn log in case a site never
                    // recovers. In this case the site admin may delete the order using an
                    // appropriate tool.
                    return;
                }
            }
            doTransition(order, newState);
        }
    }

    public static void deactivateOrder(Order order) throws UnexpectedException {
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        Map<String, Order> activeOrdersMap = sharedOrderHolders.getActiveOrdersMap();
        ChainedList closedOrders = sharedOrderHolders.getClosedOrdersList();

        synchronized (order) {
            if (activeOrdersMap.containsKey(order.getId())) {
                activeOrdersMap.remove(order.getId());
            } else {
                String message = String.format(Messages.Exception.REMOVE_ORDER_NOT_ACTIVE, order.getId());
                throw new UnexpectedException(message);
            }
            closedOrders.removeItem(order);
            order.setInstanceId(null);
            order.setOrderState(OrderState.DEACTIVATED);
        }
    }

    private static void doTransition(Order order, OrderState newState) throws UnexpectedException {
        OrderState currentState = order.getOrderState();

        if (currentState == newState) {
            // The order may have already been moved to the new state by another thread
            // In this case, there is nothing else to be done
            return;
        }

        SharedOrderHolders ordersHolder = SharedOrderHolders.getInstance();
        SynchronizedDoublyLinkedList origin = ordersHolder.getOrdersList(currentState);
        SynchronizedDoublyLinkedList destination = ordersHolder.getOrdersList(newState);

        if (origin == null) {
            String message = String.format(Messages.Exception.COULD_NOT_FIND_LIST_FOR_STATE, currentState);
            throw new UnexpectedException(message);
        } else if (destination == null) {
            String message = String.format(Messages.Exception.COULD_NOT_FIND_DESTINATION_LIST_FOR_STATE, newState);
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

    private static void notifyRequester(Order order, Event instanceFailed) throws Exception {
        RemoteNotifyEventRequest remoteNotifyEventRequest = new RemoteNotifyEventRequest(order, instanceFailed);
        remoteNotifyEventRequest.send();
    }
}
