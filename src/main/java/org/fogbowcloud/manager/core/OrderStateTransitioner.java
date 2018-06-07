package org.fogbowcloud.manager.core;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.api.intercomponent.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.api.intercomponent.xmpp.Event;
import org.fogbowcloud.manager.api.intercomponent.xmpp.requesters.RemoteNotifyEventRequest;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.exceptions.OrderStateTransitionException;
import org.fogbowcloud.manager.core.models.linkedlist.ChainedList;
import org.fogbowcloud.manager.core.plugins.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.linkedlist.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.utils.PropertiesUtil;

import java.util.Map;

public class OrderStateTransitioner {

    private static final Logger LOGGER = Logger.getLogger(OrderStateTransitioner.class);

    public static void activateOrder(Order order) throws OrderManagementException {
        synchronized (order) {
            if (order == null) {
                String message = "Can't process new order request. Order reference is null.";
                throw new OrderManagementException(message);
            }

            SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
            Map<String, Order> activeOrdersMap = sharedOrderHolders.getActiveOrdersMap();
            ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();

            String orderId = order.getId();

            if (activeOrdersMap.containsKey(orderId)) {
                String message = String.format("Order with id %s is already in active orders map.", orderId);
                throw new OrderManagementException(message);
            }

            order.setOrderState(OrderState.OPEN);
            activeOrdersMap.put(orderId, order);
            openOrdersList.addItem(order);
        }
    }

    public static void transition(Order order, OrderState newState)
            throws OrderStateTransitionException {

        String localMemberId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);
        synchronized (order) {
            if (order.isRequesterRemote(localMemberId)) {
                switch (newState) {
                    case FAILED:
                        notifyRequester(order, Event.INSTANCE_FAILED);
                        newState = OrderState.CLOSED;
                        break;
                    case FULFILLED:
                        notifyRequester(order, Event.INSTANCE_FULFILLED);
                        break;
                }
            }

            doTransition(order, newState);
        }
    }

    public static void deactivateOrder(Order order) {

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        Map<String, Order> activeOrdersMap = sharedOrderHolders.getActiveOrdersMap();
        ChainedList closedOrders = sharedOrderHolders.getClosedOrdersList();

        synchronized (order) {
            if (activeOrdersMap.containsKey(order.getId())) {
                activeOrdersMap.remove(order.getId());
            } else {
                String message =
                        "Tried to remove order %s from the active orders but it was not active";
                LOGGER.error(String.format(message, order.getId()));
            }
            closedOrders.removeItem(order);
        }
    }

    /**
     * Updates the state of the given order. This method is not thread-safe, the caller must assure
     * that only one thread can call it at the same time. This can be done by creating a
     * synchronized block on the order object that is passed as parameter to transition(). The
     * reason the synchronized block is created in the code calling this method is because, usually
     * the order will be inspected before the call to transition() is made, and this needs to be
     * done within a synchronized block, since several processors may attempt to process the same
     * order at the same time.
     *
     * @param order, the order that will transition through states
     * @param newState, the new state
     */
    private static void doTransition(Order order, OrderState newState)
            throws OrderStateTransitionException {
        OrderState currentState = order.getOrderState();

        if (currentState == newState) {
            String message =
                    String.format("Order with id %s is already %s", order.getId(), currentState);
            throw new OrderStateTransitionException(message);
        }

        SharedOrderHolders ordersHolder = SharedOrderHolders.getInstance();
        SynchronizedDoublyLinkedList origin = ordersHolder.getOrdersList(currentState);
        SynchronizedDoublyLinkedList destination = ordersHolder.getOrdersList(newState);

        if (origin == null) {
            String message = String.format("Could not find list for state %s", currentState);
            throw new RuntimeException(message);
        } else if (destination == null) {
            String message =
                    String.format("Could not find destination list for state %s", newState);
            throw new RuntimeException(message);
        } else {
            if (origin.removeItem(order)) {
                order.setOrderState(newState);
                destination.addItem(order);
            } else {
                String template = "Could not remove order id %s from list of %s orders";
                String message = String.format(template, order.getId(), currentState.toString());
                throw new OrderStateTransitionException(message);
            }
        }
    }

    private static void notifyRequester(Order order, Event instanceFailed) {
        RemoteNotifyEventRequest remoteNotifyEventRequest = new RemoteNotifyEventRequest(order, instanceFailed);
        try {
            remoteNotifyEventRequest.send();
        } catch (RemoteRequestException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (OrderManagementException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (UnauthorizedException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

}
