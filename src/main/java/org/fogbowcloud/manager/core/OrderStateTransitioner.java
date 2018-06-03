package org.fogbowcloud.manager.core;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.api.intercomponent.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.api.intercomponent.xmpp.Event;
import org.fogbowcloud.manager.api.intercomponent.xmpp.requesters.RemoteNotifyEventRequest;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.exceptions.OrderStateTransitionException;
import org.fogbowcloud.manager.core.plugins.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.linkedlist.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.utils.PropertiesUtil;

public class OrderStateTransitioner {

    private static final String LOCAL_MEMBER_ID = PropertiesUtil.getLocalMemberId();
    private static final Logger LOGGER = Logger.getLogger(OrderStateTransitioner.class);

    public static void transition(Order order, OrderState newState)
            throws OrderStateTransitionException {

        if (order.isRequesterRemote(LOCAL_MEMBER_ID)) {
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
