package org.fogbowcloud.manager.api.intercomponent.xmpp.requesters;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.manager.api.intercomponent.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.api.intercomponent.exceptions.UnexpectedException;
import org.fogbowcloud.manager.api.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.api.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.manager.api.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.plugins.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.jamppa.component.PacketSender;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

import com.google.gson.Gson;

public class RemoteDeleteOrderRequest implements RemoteRequest<Void> {

    private static final Logger LOGGER = Logger.getLogger(RemoteDeleteOrderRequest.class);

    private Order order;

    public RemoteDeleteOrderRequest(Order order) {
        this.order = order;
    }

    @Override
    public Void send() throws RemoteRequestException, OrderManagementException, UnauthorizedException {
        IQ iq = createIq();
        IQ response = (IQ) PacketSenderHolder.getPacketSender().syncSendPacket(iq);

        if (response == null) {
            String message = "Unable to retrieve the response from providing member: " + this.order.getProvidingMember();
            throw new UnexpectedException(message);
        }
        if (response.getError() != null) {
            if (response.getError().getCondition() == PacketError.Condition.forbidden){
                String message = "The order was not authorized for: " + this.order.getId();
                throw new UnauthorizedException(message);
            } else if (response.getError().getCondition() == PacketError.Condition.bad_request){
                String message = "The order was duplicated on providing member: " + this.order.getProvidingMember();
                throw new OrderManagementException(message);
            }
        }
        LOGGER.debug("Request for order: " + this.order.getId() + " has been sent to " + this.order.getProvidingMember());
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
