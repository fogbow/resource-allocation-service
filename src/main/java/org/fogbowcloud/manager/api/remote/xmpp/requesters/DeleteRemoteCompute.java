package org.fogbowcloud.manager.api.remote.xmpp.requesters;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.manager.api.remote.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.api.remote.exceptions.UnexpectedException;
import org.fogbowcloud.manager.api.remote.xmpp.IqElement;
import org.fogbowcloud.manager.api.remote.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.jamppa.component.PacketSender;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

public class DeleteRemoteCompute implements RemoteRequest<Void> {


    private static final Logger LOGGER = Logger.getLogger(DeleteRemoteCompute.class);

    private PacketSender packetSender;
    private String remoteOrderId;
    private String providingMember;
    private FederationUser federationUser;

    public DeleteRemoteCompute(PacketSender packetSender, String remoteOrderId, FederationUser federationUser, String providingMember) {
        this.packetSender = packetSender;
        this.remoteOrderId = remoteOrderId;
        this.providingMember = providingMember;
        this.federationUser = federationUser;
    }

    @Override
    public Void send() throws RemoteRequestException, OrderManagementException, UnauthorizedException {
        if (packetSender == null) {
            LOGGER.warn("Packet sender not set.");
            throw new IllegalArgumentException("Packet sender not set.");
        }

        IQ iq = createIq();
        IQ response = (IQ) packetSender.syncSendPacket(iq);

        if (response == null) {
            String message = "Unable to retrieve the response from providing member: " + providingMember;
            throw new UnexpectedException(message);
        }
        if (response.getError() != null) {
            if (response.getError().getCondition() == PacketError.Condition.forbidden){
                String message = "The order was not authorized for: " + remoteOrderId;
                throw new UnauthorizedException(message);
            } else if (response.getError().getCondition() == PacketError.Condition.bad_request){
                String message = "The order was duplicated on providing member: " + providingMember;
                throw new OrderManagementException(message);
            }
        }
        LOGGER.debug("Request for order: " + remoteOrderId + " has been sent to " + providingMember);
        return null;
    }

    private IQ createIq() {
        LOGGER.debug("Creating IQ for order: " + remoteOrderId);

        IQ iq = new IQ(IQ.Type.set);
        iq.setTo(providingMember);
        iq.setID(remoteOrderId);

        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.DELETE_REMOTE_ORDER.toString());
        Element orderIdElement = queryElement.addElement(IqElement.REMOTE_ORDER_ID.toString());
        orderIdElement.setText(this.remoteOrderId);

        LOGGER.debug("Jsonifying federation user.");
        Element userElement = iq.getElement().addElement(IqElement.FEDERATION_USER.toString());
        userElement.setText(new Gson().toJson(this.federationUser));
        return iq;
    }
}
