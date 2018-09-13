package org.fogbowcloud.ras.core;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.intercomponent.xmpp.Event;
import org.fogbowcloud.ras.core.intercomponent.xmpp.requesters.RemoteNotifyEventRequest;
import org.fogbowcloud.ras.core.models.linkedlists.ChainedList;
import org.fogbowcloud.ras.core.models.linkedlists.SynchronizedDoublyLinkedList;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.orders.OrderState;

import java.util.Map;

public class OrderStateTransitioner {
    private static final Logger LOGGER = Logger.getLogger(OrderStateTransitioner.class);

    public static void activateOrder(Order order) throws UnexpectedException {
        LOGGER.info("Activating new order request received");

        if (order == null) {
            String message = "Cannot process new order request. Order reference is null.";
            throw new UnexpectedException(message);
        }

        synchronized (order) {
            SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
            Map<String, Order> activeOrdersMap = sharedOrderHolders.getActiveOrdersMap();
            ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();

            String orderId = order.getId();

            if (activeOrdersMap.containsKey(orderId)) {
                String message = String.format("Order with id %s is already in active orders map.", orderId);
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
                    String message = "Could not notify requesting member [" + order.getRequestingMember() +
                            " for order " + order.getId();
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
                String message = String.format(
                        "Tried to remove order %s from the active orders but it was not active", order.getId());
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
            String message = String.format("Could not find list for state %s", currentState);
            throw new UnexpectedException(message);
        } else if (destination == null) {
            String message = String.format("Could not find destination list for state %s", newState);
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
