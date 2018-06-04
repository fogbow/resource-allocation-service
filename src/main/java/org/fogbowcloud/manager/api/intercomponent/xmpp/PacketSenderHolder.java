package org.fogbowcloud.manager.api.intercomponent.xmpp;

import org.fogbowcloud.manager.core.OrderController;
import org.jamppa.component.PacketSender;

public class PacketSenderHolder {

    private static PacketSender packetSender;

    public static void init(String jid, String password, String xmppServerIp,
                     int xmppServerPort, long timeout, OrderController orderController) {
        packetSender = new XmppComponentManager(jid, password, xmppServerIp, xmppServerPort, timeout, orderController);
    }

    public static synchronized PacketSender getPacketSender() {
        if (packetSender == null) {
            throw new IllegalStateException("PacketSender was not initialized");
        }

        return packetSender;
    }
}

