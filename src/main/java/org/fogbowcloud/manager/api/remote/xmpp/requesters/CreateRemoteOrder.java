package org.fogbowcloud.manager.api.remote.xmpp.requesters;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.manager.api.remote.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.jamppa.component.PacketSender;
import org.xmpp.packet.IQ;

public class CreateRemoteOrder {

    private static final Logger LOGGER = Logger.getLogger(CreateRemoteOrder.class);

    public int sendRequest(Order order, FederationUser federationUser, PacketSender packetSender) {
        if (packetSender == null) {
            LOGGER.warn("Packet sender not set.");
            throw new IllegalArgumentException("Packet sender not set.");
        }
        IQ iq = new IQ(IQ.Type.set);
        Element queryElement = iq.getElement().addElement("query", RemoteMethod.CREATE_REMOTE_ORDER.name());
        Element orderElement = queryElement.addElement("order");
        return 404;
    }

}
