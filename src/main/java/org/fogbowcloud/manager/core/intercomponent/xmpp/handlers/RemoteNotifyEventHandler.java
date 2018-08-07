package org.fogbowcloud.manager.core.intercomponent.xmpp.handlers;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.manager.core.intercomponent.RemoteFacade;
import org.fogbowcloud.manager.core.intercomponent.xmpp.Event;
import org.fogbowcloud.manager.core.intercomponent.xmpp.XmppExceptionToErrorConditionTranslator;
import org.fogbowcloud.manager.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;

public class RemoteNotifyEventHandler extends AbstractQueryHandler {

    private static final Logger LOGGER = Logger.getLogger(RemoteNotifyEventHandler.class);

    public static final String REMOTE_NOTIFY_EVENT = RemoteMethod.REMOTE_NOTIFY_EVENT.toString();

    public RemoteNotifyEventHandler() {
        super(REMOTE_NOTIFY_EVENT);
    }

    @Override
    public IQ handle(IQ iq) {
        LOGGER.info("Received request for order: " + iq.getID());
        String orderJsonStr = unMarshallOrder(iq);
        String className = unMarshallClassName(iq);

        IQ response = IQ.createResultIQ(iq);

        Gson gson = new Gson();
        Order order = null;
        try {
            order = (Order) gson.fromJson(orderJsonStr, Class.forName(className));
            Element eventElement = marshallEvent(iq);
            Event event = gson.fromJson(eventElement.getText(), Event.class);
            RemoteFacade.getInstance().handleRemoteEvent(event, order);
        } catch (Exception e) {
            XmppExceptionToErrorConditionTranslator.updateErrorCondition(response, e);
        }
        return response;
    }

	private Element marshallEvent(IQ iq) {
		Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
		return queryElement.element(IqElement.EVENT.toString());
	}

	private String unMarshallClassName(IQ iq) {
		Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
		Element orderClassNameElement = queryElement.element(IqElement.ORDER_CLASS_NAME.toString());
        String className = orderClassNameElement.getText();
		return className;
	}

	private String unMarshallOrder(IQ iq) {
		Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
		Element orderElement = queryElement.element(IqElement.ORDER.toString());
        String orderJsonStr = orderElement.getText();
		return orderJsonStr;
	}
}
