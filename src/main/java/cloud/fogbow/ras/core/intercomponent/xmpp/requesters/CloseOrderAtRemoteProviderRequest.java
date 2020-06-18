package cloud.fogbow.ras.core.intercomponent.xmpp.requesters;

import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.intercomponent.xmpp.*;
import cloud.fogbow.ras.core.models.orders.Order;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.xmpp.packet.IQ;

public class CloseOrderAtRemoteProviderRequest implements RemoteRequest<Void> {
    private static final Logger LOGGER = Logger.getLogger(CloseOrderAtRemoteProviderRequest.class);

    private Order order;

    public CloseOrderAtRemoteProviderRequest(Order order) {
        this.order = order;
    }

    @Override
    public Void send() throws Exception {
        IQ iq = CloseOrderAtRemoteProviderRequest.marshall(this.order);
        LOGGER.debug(String.format(Messages.Log.SENDING_MSG_S, iq.getID()));
        IQ response = (IQ) PacketSenderHolder.getPacketSender().syncSendPacket(iq);

        XmppErrorConditionToExceptionTranslator.handleError(response, this.order.getRequester());
        LOGGER.debug(Messages.Log.SUCCESS);
        return null;
    }

    public static IQ marshall(Order order) {
        IQ iq = new IQ(IQ.Type.set);
        iq.setTo(SystemConstants.JID_SERVICE_NAME + SystemConstants.JID_CONNECTOR + SystemConstants.XMPP_SERVER_NAME_PREFIX + order.getRequester());
        iq.setID(order.getId());

        //marshall order parcel
        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_NOTIFY_EVENT.toString());

        Element orderIdElement = queryElement.addElement(IqElement.ORDER_ID.toString());
        orderIdElement.setText(order.getId());

        return iq;
    }
}
