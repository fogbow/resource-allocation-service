package org.fogbowcloud.manager.core.intercomponent.xmpp.requesters;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.manager.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

public class RemoteCreateOrderRequest implements RemoteRequest<Void> {

    private static final Logger LOGGER = Logger.getLogger(RemoteCreateOrderRequest.class);

    private Order order;

    public RemoteCreateOrderRequest(Order order) {
        this.order = order;
    }

    @Override
    public Void send() throws FogbowManagerException {
        IQ iq = createIq();
        IQ response = (IQ) PacketSenderHolder.getPacketSender().syncSendPacket(iq);

        if (response == null) {
            String message = "Unable to retrieve the response from providing member: " + order.getProvidingMember();
            throw new UnavailableProviderException(message);
        }
        if (response.getError() != null) {
            if (response.getError().getCondition() == PacketError.Condition.forbidden){
                String message = "The order was not authorized for: " + order.getId();
                throw new UnauthorizedRequestException(message);
            } else if (response.getError().getCondition() == PacketError.Condition.bad_request){
                String message = "The order was duplicated on providing member: " + order.getProvidingMember();
                throw new InvalidParameterException(message);
            }
        }
        LOGGER.debug("Request for order: " + this.order.getId() + " has been sent to " + order.getProvidingMember());
        return null;
    }

    private IQ createIq() {
        LOGGER.debug("Creating IQ for order: " + this.order.getId());

        IQ iq = new IQ(IQ.Type.set);
        iq.setTo(this.order.getProvidingMember());
        iq.setID(this.order.getId());

        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_CREATE_ORDER.toString());
        Element orderElement = queryElement.addElement(IqElement.ORDER.toString());

        Element orderClassNameElement = queryElement.addElement(IqElement.ORDER_CLASS_NAME.toString());
        orderClassNameElement.setText(this.order.getClass().getName());
        
        LOGGER.debug("Jsonifying Order");
        String orderJson = new Gson().toJson(this.order);
        orderElement.setText(orderJson);
        return iq;
    }
}
