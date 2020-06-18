package cloud.fogbow.ras.core.intercomponent.xmpp.requesters;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.intercomponent.xmpp.IqElement;
import cloud.fogbow.ras.core.intercomponent.xmpp.PacketSenderHolder;
import cloud.fogbow.ras.core.intercomponent.xmpp.RemoteMethod;
import cloud.fogbow.ras.core.intercomponent.xmpp.XmppErrorConditionToExceptionTranslator;
import cloud.fogbow.ras.core.models.orders.Order;
import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.xmpp.packet.IQ;

public class RemoteGetOrderRequest implements RemoteRequest<Order> {
    private static final Logger LOGGER = Logger.getLogger(RemoteGetOrderRequest.class);

    private Order order;

    public RemoteGetOrderRequest(Order order) {
        this.order = order;
    }

    @Override
    public Order send() throws Exception {

        IQ iq = marshal(this.order);
        LOGGER.debug(String.format(Messages.Log.SENDING_MSG_S, iq.getID()));
        IQ response = (IQ) PacketSenderHolder.getPacketSender().syncSendPacket(iq);

        XmppErrorConditionToExceptionTranslator.handleError(response, this.order.getProvider());
        Order order = unmarshalOrder(response);
        LOGGER.debug(Messages.Log.SUCCESS);
        return order;
    }

    public static IQ marshal(Order order) {
        IQ iq = new IQ(IQ.Type.get);
        iq.setTo(SystemConstants.JID_SERVICE_NAME + SystemConstants.JID_CONNECTOR + SystemConstants.XMPP_SERVER_NAME_PREFIX + order.getProvider());

        //order
        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_GET_ORDER.toString());

        Element orderIdElement = queryElement.addElement(IqElement.ORDER_ID.toString());
        orderIdElement.setText(order.getId());

        return iq;
    }

    private Order unmarshalOrder(IQ response) throws InternalServerErrorException {

        Element queryElement = response.getElement().element(IqElement.QUERY.toString());
        String orderStr = queryElement.element(IqElement.ORDER.toString()).getText();

        String orderClassName = queryElement.element(IqElement.ORDER_CLASS_NAME.toString()).getText();

        Order order = null;
        try {
            order = (Order) new Gson().fromJson(orderStr, Class.forName(orderClassName));
        } catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage());
        }

        return order;
    }
}
