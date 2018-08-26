package org.fogbowcloud.ras.core.intercomponent.xmpp.requesters;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.ras.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.ras.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.ras.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.ras.core.intercomponent.xmpp.XmppErrorConditionToExceptionTranslator;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.xmpp.packet.IQ;

public class RemoteDeleteOrderRequest implements RemoteRequest<Void> {
    private static final Logger LOGGER = Logger.getLogger(RemoteDeleteOrderRequest.class);

    private Order order;

    public RemoteDeleteOrderRequest(Order order) {
        this.order = order;
    }

    @Override
    public Void send() throws Exception {
        IQ iq = RemoteDeleteOrderRequest.marshal(this.order);
        IQ response = (IQ) PacketSenderHolder.getPacketSender().syncSendPacket(iq);

        XmppErrorConditionToExceptionTranslator.handleError(response, this.order.getProvidingMember());
        return null;
    }

    public static IQ marshal(Order order) {
        IQ iq = new IQ(IQ.Type.set);
        iq.setTo(order.getProvidingMember());
        iq.setID(order.getId());

        // marshalling the order parcel of the IQ. It seems ok to not have another method to do so
        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_DELETE_ORDER.toString());
        Element orderIdElement = queryElement.addElement(IqElement.ORDER_ID.toString());
        orderIdElement.setText(order.getId());

        Element orderTypeElement = queryElement.addElement(IqElement.INSTANCE_TYPE.toString());
        orderTypeElement.setText(order.getType().toString());

        Element userElement = iq.getElement().addElement(IqElement.FEDERATION_USER.toString());
        userElement.setText(new Gson().toJson(order.getFederationUserToken()));

        return iq;
    }
}
