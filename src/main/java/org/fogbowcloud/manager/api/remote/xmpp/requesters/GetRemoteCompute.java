package org.fogbowcloud.manager.api.remote.xmpp.requesters;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.manager.api.remote.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.api.remote.xmpp.IqElement;
import org.fogbowcloud.manager.api.remote.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.jamppa.component.PacketSender;
import org.xmpp.packet.IQ;

public class GetRemoteCompute implements RemoteRequest {

    private static final Logger LOGGER = Logger.getLogger(GetRemoteCompute.class);

    private PacketSender packetSender;
    private String remoteOrderId;
    private String targetMember;
    private FederationUser user;

    public GetRemoteCompute(PacketSender packetSender, String remoteOrderId, String targetMember, FederationUser user) {
        this.packetSender = packetSender;
        this.remoteOrderId = remoteOrderId;
        this.targetMember = targetMember;
        this.user = user;
    }

    @Override
    public void send() throws RemoteRequestException {
        if (this.packetSender == null) {
            throw new IllegalArgumentException("Packet sender not set.");
        }

        IQ iq = createIq();

        IQ response = (IQ) packetSender.syncSendPacket(iq);
        if (response == null) {
            LOGGER.error("Unable to retrieve the response from " + this.targetMember + ". IQ is " + iq.toString());
        } else if (response.getError() != null) {
            LOGGER.error(response.getError().toString());
        }
    }

    private IQ createIq() {
        IQ iq = new IQ(IQ.Type.get);
        iq.setTo(this.targetMember);

        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.GET_REMOTE_INSTANCE.toString());
        Element orderIdElement = queryElement.addElement(IqElement.REMOTE_ORDER_ID.toString());
        orderIdElement.setText(this.remoteOrderId);

        Element userElement = iq.getElement().addElement(IqElement.FEDERATION_USER.toString());
        userElement.setText(new Gson().toJson(this.user));
        return iq;
    }
}
