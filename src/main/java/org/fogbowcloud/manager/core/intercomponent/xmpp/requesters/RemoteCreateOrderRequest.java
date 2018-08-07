package org.fogbowcloud.manager.core.intercomponent.xmpp.requesters;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.manager.core.intercomponent.xmpp.XmppErrorConditionToExceptionTranslator;
import org.fogbowcloud.manager.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.manager.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.xmpp.packet.IQ;

public class RemoteCreateOrderRequest implements RemoteRequest<Void> {

    private static final Logger LOGGER = Logger.getLogger(RemoteCreateOrderRequest.class);

    private Order order;

    public RemoteCreateOrderRequest(Order order) {
        this.order = order;
    }

    @Override
    public Void send() throws Exception {
        IQ iq = createIq();
        IQ response = (IQ) PacketSenderHolder.getPacketSender().syncSendPacket(iq);

        XmppErrorConditionToExceptionTranslator.handleError(response, this.order.getProvidingMember());
        LOGGER.debug("Request for order: " + this.order.getId() + " has been sent to " + order.getProvidingMember());
        return null;
    }

    public IQ createIq() {
        LOGGER.debug("Creating IQ for order: " + order.getId());

        IQ iq = new IQ(IQ.Type.set);
        iq.setTo(this.order.getProvidingMember());
        iq.setID(this.order.getId());

        marshalOrder(iq);

        return iq;
    }

    private void marshalOrder(IQ iq) {
        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_CREATE_ORDER.toString());

        Element orderElement = queryElement.addElement(IqElement.ORDER.toString());

        Element orderClassNameElement =
                queryElement.addElement(IqElement.ORDER_CLASS_NAME.toString());
        
        orderClassNameElement.setText(this.order.getClass().getName());

        String orderJson = new Gson().toJson(this.order);
        orderElement.setText(orderJson);
    }

}
