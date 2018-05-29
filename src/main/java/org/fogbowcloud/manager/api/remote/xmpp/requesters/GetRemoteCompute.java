package org.fogbowcloud.manager.api.remote.xmpp.requesters;

import com.google.gson.Gson;
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

public class GetRemoteCompute {

    private static final Logger LOGGER = Logger.getLogger(GetRemoteCompute.class);

    public static void sendRequest(String remoteOrderId, String targetMember, FederationUser user, PacketSender packetSender) {
        if (packetSender == null) {
            throw new IllegalArgumentException("Packet sender not set.");
        }

        IQ iq = new IQ(IQ.Type.get);
        iq.setTo(targetMember);

        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.GET_REMOTE_INSTANCE.toString());
        Element orderIdElement = queryElement.addElement(IqElement.REMOTE_ORDER_ID.toString());
        orderIdElement.setText(remoteOrderId);

        Element userElement = iq.getElement().addElement(IqElement.FEDERATION_USER.toString());
        userElement.setText(new Gson().toJson(user));

        IQ response = (IQ) packetSender.syncSendPacket(iq);
        if (response == null) {
            LOGGER.error("Unable to retrieve the response from " + targetMember + ". IQ is " + iq.toString());
        } else if (response.getError() != null) {
            LOGGER.error(response.getError().toString());
        }
    }

    public static void main(String[] args) throws InterruptedException {
        XmppComponentManager xmppComponentA = new XmppComponentManager("test.fogbow.a", "password", "127.0.0.1", 5347, 5000L);
        try {
            xmppComponentA.connect();
        } catch (ComponentException e) {
            System.err.println("Conflict in the initialization of xmpp component.");
            System.exit(128);
        }

        sendRequest("remoteOrderId", "test.fogbow.a", new FederationUser(131313L, null), xmppComponentA);
    }
}
