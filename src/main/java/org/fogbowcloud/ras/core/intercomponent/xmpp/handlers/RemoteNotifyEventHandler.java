package org.fogbowcloud.ras.core.intercomponent.xmpp.handlers;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.ras.core.intercomponent.RemoteFacade;
import org.fogbowcloud.ras.core.intercomponent.xmpp.Event;
import org.fogbowcloud.ras.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.ras.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.ras.core.intercomponent.xmpp.XmppExceptionToErrorConditionTranslator;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;

public class RemoteNotifyEventHandler extends AbstractQueryHandler {
    private static final Logger LOGGER = Logger.getLogger(RemoteNotifyEventHandler.class);

    private static final String REMOTE_NOTIFY_EVENT = RemoteMethod.REMOTE_NOTIFY_EVENT.toString();

    public RemoteNotifyEventHandler() {
        super(REMOTE_NOTIFY_EVENT);
    }

    @Override
    public IQ handle(IQ iq) {
        LOGGER.info("Received request for order: " + iq.getID());

        IQ response = IQ.createResultIQ(iq);

        Gson gson = new Gson();
        Order order = null;
        Event event = null;
        try {
            order = unmarshalOrder(iq, gson);
            event = unmarshalEvent(iq, gson);

            RemoteFacade.getInstance().handleRemoteEvent(event, order);
        } catch (Exception e) {
            XmppExceptionToErrorConditionTranslator.updateErrorCondition(response, e);
        }
        return response;
    }

    private Order unmarshalOrder(IQ iq, Gson gson) throws ClassNotFoundException {

        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element orderClassNameElement = queryElement.element(IqElement.ORDER_CLASS_NAME.toString());
        String className = orderClassNameElement.getText();

        Element orderElement = queryElement.element(IqElement.ORDER.toString());
        String orderJsonStr = orderElement.getText();

        return (Order) gson.fromJson(orderJsonStr, Class.forName(className));
    }

    private Event unmarshalEvent(IQ iq, Gson gson) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element eventElement = queryElement.element(IqElement.EVENT.toString());

        Event event = gson.fromJson(eventElement.getText(), Event.class);
        return event;
    }
}
