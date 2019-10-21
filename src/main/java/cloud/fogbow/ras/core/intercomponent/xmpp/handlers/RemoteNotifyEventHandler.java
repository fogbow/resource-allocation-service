package cloud.fogbow.ras.core.intercomponent.xmpp.handlers;

import cloud.fogbow.common.util.IntercomponentUtil;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.intercomponent.RemoteFacade;
import cloud.fogbow.ras.core.intercomponent.xmpp.IqElement;
import cloud.fogbow.ras.core.intercomponent.xmpp.RemoteMethod;
import cloud.fogbow.ras.core.intercomponent.xmpp.XmppExceptionToErrorConditionTranslator;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
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
        LOGGER.debug(String.format(Messages.Info.RECEIVING_REMOTE_REQUEST, iq.getID()));
        IQ response = IQ.createResultIQ(iq);

        Gson gson = new Gson();
        Order order = null;
        OrderState orderState = null;
        try {
            order = unmarshalOrder(iq, gson);
            orderState = unmarshalEvent(iq, gson);

            String senderId = IntercomponentUtil.getSender(iq.getFrom().toBareJID(), SystemConstants.XMPP_SERVER_NAME_PREFIX);
            RemoteFacade.getInstance().handleRemoteEvent(senderId, orderState, order);
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

    private OrderState unmarshalEvent(IQ iq, Gson gson) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element newStateElement = queryElement.element(IqElement.NEW_STATE.toString());

        OrderState newState = gson.fromJson(newStateElement.getText(), OrderState.class);
        return newState;
    }
}
