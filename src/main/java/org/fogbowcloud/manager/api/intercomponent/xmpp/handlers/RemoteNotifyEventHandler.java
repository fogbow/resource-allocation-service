package org.fogbowcloud.manager.api.intercomponent.xmpp.handlers;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.manager.api.intercomponent.RemoteFacade;
import org.fogbowcloud.manager.api.intercomponent.xmpp.Event;
import org.fogbowcloud.manager.api.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.api.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.OrderController;
import org.fogbowcloud.manager.core.OrderStateTransitioner;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.exceptions.OrderStateTransitionException;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.plugins.exceptions.UnauthorizedException;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

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
        LOGGER.info("Received request for order: " + iq.getID());
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element orderElement = queryElement.element(IqElement.ORDER.toString());
        String orderJsonStr = orderElement.getText();

        Element orderClassNameElement = queryElement.element(IqElement.ORDER_CLASS_NAME.toString());
        String className = orderClassNameElement.getText();

        IQ response = IQ.createResultIQ(iq);

        Gson gson = new Gson();
        Order order = null;
        try {
            order = (Order) gson.fromJson(orderJsonStr, Class.forName(className));
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            response.setError(PacketError.Condition.bad_request);
            return response;
        }

        Element eventElement = iq.getElement();
        Event event = gson.fromJson(eventElement.getText(), Event.class);

        // order is a java object that represents the order passed in the message
        // actualOrder is the java object that represents this order inside the current manager
        Order actualOrder = orderController.getOrder(order.getId(), order.getFederationUser(), order.getType());
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
