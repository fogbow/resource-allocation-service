package org.fogbowcloud.ras.core.intercomponent.xmpp.handlers;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.intercomponent.RemoteFacade;
import org.fogbowcloud.ras.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.ras.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.ras.core.intercomponent.xmpp.XmppExceptionToErrorConditionTranslator;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;

public class RemoteCreateOrderRequestHandler extends AbstractQueryHandler {
    private static final Logger LOGGER = Logger.getLogger(RemoteCreateOrderRequestHandler.class);

    private static final String REMOTE_CREATE_ORDER = RemoteMethod.REMOTE_CREATE_ORDER.toString();

    public RemoteCreateOrderRequestHandler() {
        super(REMOTE_CREATE_ORDER);
    }

    @Override
    public IQ handle(IQ iq) {
        LOGGER.info(String.format(Messages.Info.RECEIVED_REQUEST_FOR_ORDER, iq.getID()));
        String orderJsonStr = unmarshalOrder(iq);
        String className = unmarshalClassName(iq);

        IQ response = IQ.createResultIQ(iq);

        Gson gson = new Gson();
        Order order = null;
        try {
            order = (Order) gson.fromJson(orderJsonStr, Class.forName(className));
            RemoteFacade.getInstance().activateOrder(order);
        } catch (Throwable e) {
            XmppExceptionToErrorConditionTranslator.updateErrorCondition(response, e);
        }
        return response;
    }

    private String unmarshalOrder(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element orderElement = queryElement.element(IqElement.ORDER.toString());
        String orderJsonStr = orderElement.getText();
        return orderJsonStr;
    }

    private String unmarshalClassName(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element orderClassNameElement = queryElement.element(IqElement.ORDER_CLASS_NAME.toString());
        String className = orderClassNameElement.getText();
        return className;
    }
}
