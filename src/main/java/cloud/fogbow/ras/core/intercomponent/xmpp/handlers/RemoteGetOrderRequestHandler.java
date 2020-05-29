package cloud.fogbow.ras.core.intercomponent.xmpp.handlers;

import cloud.fogbow.common.util.IntercomponentUtil;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.intercomponent.RemoteFacade;
import cloud.fogbow.ras.core.intercomponent.xmpp.IqElement;
import cloud.fogbow.ras.core.intercomponent.xmpp.RemoteMethod;
import cloud.fogbow.ras.core.intercomponent.xmpp.XmppExceptionToErrorConditionTranslator;
import cloud.fogbow.ras.core.models.orders.Order;
import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;

public class RemoteGetOrderRequestHandler extends AbstractQueryHandler {
    private static final Logger LOGGER = Logger.getLogger(RemoteGetOrderRequestHandler.class);

    private static final String REMOTE_GET_ORDER = RemoteMethod.REMOTE_GET_ORDER.toString();

    public RemoteGetOrderRequestHandler() {
        super(REMOTE_GET_ORDER);
    }

    @Override
    public IQ handle(IQ iq) {
        LOGGER.debug(String.format(Messages.Info.RECEIVING_REMOTE_REQUEST, iq.getID()));
        String orderId = unmarshalOrderId(iq);

        IQ response = IQ.createResultIQ(iq);
        try {
            String senderId = IntercomponentUtil.getSender(iq.getFrom().toBareJID(), SystemConstants.XMPP_SERVER_NAME_PREFIX);
            Order order = RemoteFacade.getInstance().getOrder(senderId, orderId);
            //on success, update response with order data
            updateResponse(response, order);
        } catch (Exception e) {
            //on error, update response with exception data
            XmppExceptionToErrorConditionTranslator.updateErrorCondition(response, e);
        }
        return response;
    }

    private void updateResponse(IQ response, Order order) {
        Element queryElement =
                response.getElement().addElement(IqElement.QUERY.toString(), REMOTE_GET_ORDER);

        Element orderElement = queryElement.addElement(IqElement.ORDER.toString());

        Element orderClassNameElement = queryElement.addElement(IqElement.ORDER_CLASS_NAME.toString());

        orderClassNameElement.setText(order.getClass().getName());

        orderElement.setText(new Gson().toJson(order));
    }

    private String unmarshalOrderId(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element orderIdElement = queryElement.element(IqElement.ORDER_ID.toString());
        String orderId = orderIdElement.getText();
        return orderId;
    }
}
