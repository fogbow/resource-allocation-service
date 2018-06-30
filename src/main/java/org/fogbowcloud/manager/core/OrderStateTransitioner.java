package org.fogbowcloud.manager.core;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.intercomponent.xmpp.Event;
import org.fogbowcloud.manager.core.intercomponent.xmpp.requesters.RemoteNotifyEventRequest;
import org.fogbowcloud.manager.core.models.linkedlists.ChainedList;
import org.fogbowcloud.manager.core.models.linkedlists.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;

import java.util.Map;

public class OrderStateTransitioner {

    private static final Logger LOGGER = Logger.getLogger(OrderStateTransitioner.class);

    public static void activateOrder(Order order) throws UnexpectedException {
        LOGGER.info("Activating new compute order request received");

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
                    String message = "Could not notify requesting member ["+ order.getRequestingMember() +
                            " for order " + order.getId();
                    LOGGER.warn(message);
                    // FIXME: What should we do when the notification is not received?
                    // Currently, it will keep trying to notify forever. Shall we give up
                    // after a few attempts?
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
        }
    }

    private static void doTransition(Order order, OrderState newState) throws UnexpectedException {
        OrderState currentState = order.getOrderState();

        if (currentState == newState) {
            String message = String.format("Order with id %s is already %s", order.getId(), currentState);
            throw new UnexpectedException(message);
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
            if (origin.removeItem(order)) {
                order.setOrderState(newState);
                destination.addItem(order);
            } else {
                String message = String.format(
                        "Could not remove order id %s from list of %s orders", order.getId(), currentState.toString());
                throw new UnexpectedException(message);
            }
        }
    }

    private static void notifyRequester(Order order, Event instanceFailed) throws Exception {
        RemoteNotifyEventRequest remoteNotifyEventRequest = new RemoteNotifyEventRequest(order, instanceFailed);
        remoteNotifyEventRequest.send();
    }
}
