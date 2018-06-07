package org.fogbowcloud.manager.api.intercomponent.xmpp.requesters;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.manager.api.intercomponent.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.api.intercomponent.exceptions.UnexpectedException;
import org.fogbowcloud.manager.api.intercomponent.xmpp.Event;
import org.fogbowcloud.manager.api.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.api.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.manager.api.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.plugins.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.xmpp.packet.IQ;

public class RemoteNotifyEventRequest implements RemoteRequest<Void> {

    private static final Logger LOGGER = Logger.getLogger(RemoteNotifyEventRequest.class);

    private Order order;
    private Event event;

    public RemoteNotifyEventRequest(Order order, Event event) {
        this.order = order;
        this.event = event;
    }

    @Override
    public Void send() throws RemoteRequestException, OrderManagementException, UnauthorizedException {
        IQ iq = createIq();
        IQ response = (IQ) PacketSenderHolder.getPacketSender().syncSendPacket(iq);

        if (response == null) {
            String message = "Unable to retrieve the response from providing member: " + order.getProvidingMember();
            throw new UnexpectedException(message);
        } else if (response.getError() != null) {
            throw new UnexpectedException(response.getError().toString());
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
                RemoteMethod.REMOTE_NOTIFY_EVENT.toString());
        Element orderElement = queryElement.addElement(IqElement.ORDER.toString());
        orderElement.setText(new Gson().toJson(this.order));

        Element orderClassNameElement = queryElement.addElement(IqElement.ORDER_CLASS_NAME.toString());
        orderClassNameElement.setText(this.order.getClass().getName());

        Element eventElement = iq.getElement().addElement(IqElement.EVENT.toString());
        eventElement.setText(new Gson().toJson(this.event));

        return iq;
    }
}
