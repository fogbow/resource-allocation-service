package org.fogbowcloud.manager.api.remote.xmpp.requesters;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.jamppa.component.PacketSender;
import org.xmpp.packet.IQ;

public class CreateRemoteOrder {

    private static final Logger LOGGER = Logger.getLogger(CreateRemoteOrder.class);

    public int makeRequest(String orderId, String providingMemeber, FederationUser federationUser,
                           PacketSender packetSender){
        if (packetSender == null) {
            LOGGER.warn("Packet sender not set.");
            throw new IllegalArgumentException("Packet sender not set.");
        }
        IQ iq = new IQ(IQ.Type.set);
        iq.setTo(providingMemeber);
        return 404;
    }
}
