package org.fogbowcloud.ras.core.intercomponent.xmpp;

import org.fogbowcloud.ras.core.constants.Messages;
import org.jamppa.component.PacketSender;

public class PacketSenderHolder {

    private static PacketSender packetSender;

    public static void init(PacketSender packetSender) {
        PacketSenderHolder.packetSender = packetSender;
    }

    public static synchronized PacketSender getPacketSender() {
        if (packetSender == null) {
            throw new IllegalStateException(Messages.Exception.NO_PACKET_SENDER);
        }

        return packetSender;
    }
}

