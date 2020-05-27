package cloud.fogbow.ras.core.intercomponent.xmpp.requesters;

import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.intercomponent.xmpp.*;
import cloud.fogbow.ras.core.models.orders.Order;
import com.google.gson.Gson;
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
        LOGGER.debug(String.format(Messages.Info.SENDING_MSG, iq.getID()));
        IQ response = (IQ) PacketSenderHolder.getPacketSender().syncSendPacket(iq);

        XmppErrorConditionToExceptionTranslator.handleError(response, this.order.getRequester());
        LOGGER.debug(Messages.Info.SUCCESS);
        return null;
    }

    public static IQ marshall(Order order) {
        IQ iq = new IQ(IQ.Type.set);
        iq.setTo(SystemConstants.JID_SERVICE_NAME + SystemConstants.JID_CONNECTOR + SystemConstants.XMPP_SERVER_NAME_PREFIX + order.getRequester());
        iq.setID(order.getId());

        //marshall order parcel
        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_NOTIFY_EVENT.toString());

        Element orderElement = queryElement.addElement(IqElement.ORDER.toString());
        orderElement.setText(new Gson().toJson(order));

        Element orderClassNameElement = queryElement.addElement(IqElement.ORDER_CLASS_NAME.toString());
        orderClassNameElement.setText(order.getClass().getName());

        return iq;
    }
}
