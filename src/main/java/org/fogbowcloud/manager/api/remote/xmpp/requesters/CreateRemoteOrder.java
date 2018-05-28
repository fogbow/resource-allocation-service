package org.fogbowcloud.manager.api.remote.xmpp.requesters;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.manager.api.remote.xmpp.IqElement;
import org.fogbowcloud.manager.api.remote.xmpp.RemoteMethod;
import org.fogbowcloud.manager.api.remote.xmpp.XmppComponentManager;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.jamppa.component.PacketSender;
import org.xmpp.component.ComponentException;
import org.xmpp.packet.IQ;

public class CreateRemoteOrder {

    private static final Logger LOGGER = Logger.getLogger(CreateRemoteOrder.class);

    public static void sendRequest(Order order, PacketSender packetSender) {
        if (packetSender == null) {
            LOGGER.warn("Packet sender not set.");
            throw new IllegalArgumentException("Packet sender not set.");
        }
        IQ iq = new IQ(IQ.Type.set);
        iq.setTo("localtest.component");
        iq.setID("12345");
        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.CREATE_REMOTE_ORDER.toString());
        Element orderElement = queryElement.addElement(IqElement.ORDER.toString());
        orderElement.addElement("newElement").setText("test");


        IQ response = (IQ) packetSender.syncSendPacket(iq);

        if (response == null) {
            System.out.println("null response.");
        }
        if (response.getError() != null) {
            System.out.println(response.getError());
        }

        System.out.println(response.getElement().element("response").getText());
    }
    
    public static void main(String[] args) {
        Order order = null;
        XmppComponentManager xmppComponent = new XmppComponentManager("testfogbow.ufcg.edu.br", "password", "localhost", 5222, 20L);
        try {
            xmppComponent.connect();
        } catch (ComponentException e) {
            System.err.println("Conflict in the initialization of xmpp component.");
            System.exit(128);
        }
        sendRequest(order, xmppComponent);
    }
}
