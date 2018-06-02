package org.fogbowcloud.manager.api.remote.xmpp.handlers;

import jdk.management.resource.internal.inst.StaticInstrumentation;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.api.remote.xmpp.Event;
import org.fogbowcloud.manager.api.remote.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.OrderController;
import org.fogbowcloud.manager.core.OrderStateTransitioner;
import org.fogbowcloud.manager.core.exceptions.OrderStateTransitionException;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;

public class RemoteNotifyEventHandler extends AbstractQueryHandler {

    private static final Logger LOGGER = Logger.getLogger(RemoteNotifyEventHandler.class);

    public static final String REMOTE_NOTIFY_EVENT = RemoteMethod.REMOTE_NOTIFY_EVENT.toString();

    private OrderController orderController;

    public RemoteNotifyEventHandler(OrderController orderController) {
        super(REMOTE_NOTIFY_EVENT);
        this.orderController = orderController;
    }

    @Override
    public IQ handle(IQ iq) {
        // TODO: implement XMPP messaging.
        Event event = null;
        Order order = null;
        Order actualOrder = orderController.getOrder(order.getId(),order.getFederationUser(), order.getType());
        switch (event) {
            case INSTANCE_FULFILLED:
                try {
                    OrderStateTransitioner.transition(actualOrder, OrderState.FULFILLED);
                } catch (OrderStateTransitionException e) {
                    LOGGER.error(e.getMessage(), e);
                }
                break;
            case INSTANCE_FAILED:
                try {
                    OrderStateTransitioner.transition(actualOrder, OrderState.FAILED);
                } catch (OrderStateTransitionException e) {
                    LOGGER.error(e.getMessage(), e);
                }
                break;
        }
        return null;
    }
}
