package org.fogbowcloud.manager.api.remote.xmpp.requesters;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.manager.api.remote.xmpp.IqElement;
import org.fogbowcloud.manager.api.remote.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.jamppa.component.PacketSender;
import org.xmpp.packet.IQ;

public class CreateRemoteOrder {

    private static final Logger LOGGER = Logger.getLogger(CreateRemoteOrder.class);

    public static void sendRequest(Order order, PacketSender packetSender) {
        if (packetSender == null) {
            LOGGER.warn("Packet sender not set.");
            throw new IllegalArgumentException("Packet sender not set.");
        }

        IQ iq = createIq(order);
        IQ response = (IQ) packetSender.syncSendPacket(iq);

        if (response == null) {
            LOGGER.error("Timed out or got no response.");
            // TODO: Throw exception
        }
        if (response.getError() != null) {
            LOGGER.error("Response returned error: " + response.getError().getText());
            // TODO: Throw exception
        }
        LOGGER.debug("Request for order: " + order.getId() + " has been sent to " + order.getProvidingMember());
    }

    private static IQ createIq(Order order) {
        LOGGER.debug("Creating IQ for order: " + order.getId());

        IQ iq = new IQ(IQ.Type.set);
        iq.setTo(order.getProvidingMember());
        iq.setID(order.getId());

        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.CREATE_REMOTE_ORDER.toString());
        Element orderElement = queryElement.addElement(IqElement.ORDER.toString());

        LOGGER.debug("Jsonifying Order");
        Gson gson = new Gson();
//        order.setId(null); ??
        String orderJson = gson.toJson(order);
        orderElement.setText(orderJson);
        return iq;
    }
}
