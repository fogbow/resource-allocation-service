package org.fogbowcloud.manager.core.intercomponent.xmpp.requesters;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.manager.core.intercomponent.xmpp.XmppErrorConditionToExceptionTranslator;
import org.fogbowcloud.manager.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.manager.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.xmpp.packet.IQ;
import com.google.gson.Gson;

public class RemoteDeleteOrderRequest implements RemoteRequest<Void> {

    private static final Logger LOGGER = Logger.getLogger(RemoteDeleteOrderRequest.class);

    private Order order;

    public RemoteDeleteOrderRequest(Order order) {
        this.order = order;
    }

    @Override
    public Void send() throws Exception {
        IQ iq = createIq();
        IQ response = (IQ) PacketSenderHolder.getPacketSender().syncSendPacket(iq);

        XmppErrorConditionToExceptionTranslator.handleError(response, this.order.getProvidingMember());
        return null;
    }

    private IQ createIq() {
        LOGGER.debug("Creating IQ for order: " + this.order.getId());

        IQ iq = new IQ(IQ.Type.set);
        iq.setTo(this.order.getProvidingMember());
        iq.setID(this.order.getId());

        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_DELETE_ORDER.toString());
        Element orderIdElement = queryElement.addElement(IqElement.ORDER_ID.toString());
        orderIdElement.setText(this.order.getId());
        
        Element orderTypeElement = queryElement.addElement(IqElement.INSTANCE_TYPE.toString());
        orderTypeElement.setText(this.order.getType().toString());

        LOGGER.debug("Jsonifying federationidentity user.");
        Element userElement = iq.getElement().addElement(IqElement.FEDERATION_USER.toString());
        userElement.setText(new Gson().toJson(this.order.getFederationUser()));
        
        return iq;
    }
}
