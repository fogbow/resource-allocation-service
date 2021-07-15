package cloud.fogbow.ras.core.intercomponent.xmpp.requesters;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.xmpp.packet.IQ;

import com.google.gson.Gson;

import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.intercomponent.xmpp.IqElement;
import cloud.fogbow.ras.core.intercomponent.xmpp.PacketSenderHolder;
import cloud.fogbow.ras.core.intercomponent.xmpp.RemoteMethod;
import cloud.fogbow.ras.core.intercomponent.xmpp.XmppErrorConditionToExceptionTranslator;
import cloud.fogbow.ras.core.models.orders.Order;

public class RemoteStopOrderRequest implements RemoteRequest<Void> {
    private static final Logger LOGGER = Logger.getLogger(RemoteStopOrderRequest.class);

    private Order order;

    public RemoteStopOrderRequest(Order order) {
        this.order = order;
    }

    @Override
    public Void send() throws Exception {
        IQ iq = RemoteStopOrderRequest.marshal(this.order);
        LOGGER.debug(String.format(Messages.Log.SENDING_MSG_S, iq.getID()));
        IQ response = (IQ) PacketSenderHolder.getPacketSender().syncSendPacket(iq);

        XmppErrorConditionToExceptionTranslator.handleError(response, this.order.getProvider());
        LOGGER.debug(Messages.Log.SUCCESS);
        return null;
    }

    public static IQ marshal(Order order) {
        IQ iq = new IQ(IQ.Type.set);
        iq.setTo(SystemConstants.JID_SERVICE_NAME + SystemConstants.JID_CONNECTOR + SystemConstants.XMPP_SERVER_NAME_PREFIX + order.getProvider());
        iq.setID(order.getId());

        // marshalling the order parcel of the IQ. It seems ok to not have another method to do so
        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_STOP_ORDER.toString());
        Element orderIdElement = queryElement.addElement(IqElement.ORDER_ID.toString());
        orderIdElement.setText(order.getId());

        Element orderTypeElement = queryElement.addElement(IqElement.INSTANCE_TYPE.toString());
        orderTypeElement.setText(order.getType().toString());

        Element userElement = iq.getElement().addElement(IqElement.SYSTEM_USER.toString());
        userElement.setText(new Gson().toJson(order.getSystemUser()));

        return iq;
    }
}
