package org.fogbowcloud.manager.api.intercomponent.xmpp.requesters;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.manager.api.intercomponent.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.api.intercomponent.exceptions.UnexpectedException;
import org.fogbowcloud.manager.api.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.api.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.models.quotas.allocation.Allocation;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.jamppa.component.PacketSender;
import org.xmpp.packet.IQ;

import com.google.gson.Gson;

public class RemoteGetUserAllocationRequest implements RemoteRequest<Allocation> {

    private static final Logger LOGGER = Logger.getLogger(RemoteGetUserAllocationRequest.class);

    private PacketSender packetSender;
    private String federationMemberId;
    private FederationUser federationUser;

    public RemoteGetUserAllocationRequest(PacketSender packetSender, String federationMemberId,
                                          FederationUser federationUser) {
        this.packetSender = packetSender;
        this.federationMemberId = federationMemberId;
        this.federationUser = federationUser;
    }

    @Override
    public Allocation send() throws RemoteRequestException {
        if (this.packetSender == null) {
            throw new IllegalArgumentException("Packet sender not set.");
        }

        IQ iq = createIq();
        IQ response = (IQ) this.packetSender.syncSendPacket(iq);

        if (response == null) {
            String message = "Unable to retrieve the quota for: " + this.federationUser;
            throw new UnexpectedException(message);
        } else if (response.getError() != null) {
            LOGGER.error(response.getError().toString());
            // TODO: Add errors treatment.
            throw new UnexpectedException(response.getError().toString());
        }
        Allocation allocation = getUserAllocationFromResponse(response);
        return allocation;
    }

    private IQ createIq() {
        IQ iq = new IQ(IQ.Type.get);
        iq.setTo(this.federationMemberId);

        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_GET_USER_ALLOCATION.toString());

        Element userElement = iq.getElement().addElement(IqElement.FEDERATION_USER.toString());
        userElement.setText(new Gson().toJson(this.federationUser));

        return iq;
    }

    private Allocation getUserAllocationFromResponse(IQ response) throws RemoteRequestException {
        Element queryElement = response.getElement().element(IqElement.QUERY.toString());
        String quotaStr = queryElement.element(IqElement.USER_ALLOCATION.toString()).getText();

        String instanceClassName = queryElement.element(IqElement.USER_ALLOCATION_CLASS_NAME.toString()).getText();

        Allocation allocation = null;
        try {
            allocation = (Allocation) new Gson().fromJson(quotaStr, Class.forName(instanceClassName));
        } catch (Exception e) {
            throw new RemoteRequestException(e.getMessage());
        }

        return allocation;
    }
}