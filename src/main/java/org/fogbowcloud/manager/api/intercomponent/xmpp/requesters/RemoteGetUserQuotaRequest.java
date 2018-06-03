package org.fogbowcloud.manager.api.intercomponent.xmpp.requesters;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.manager.api.intercomponent.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.api.intercomponent.exceptions.UnexpectedException;
import org.fogbowcloud.manager.api.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.api.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.models.quotas.Quota;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.jamppa.component.PacketSender;
import org.xmpp.packet.IQ;

import com.google.gson.Gson;

public class RemoteGetUserQuotaRequest implements RemoteRequest<Quota> {

    private static final Logger LOGGER = Logger.getLogger(RemoteGetUserQuotaRequest.class);

    private PacketSender packetSender;
    private String federationMemberId;
    private FederationUser federationUser;

    public RemoteGetUserQuotaRequest(PacketSender packetSender, String federationMemberId, FederationUser federationUser) {
        this.packetSender = packetSender;
        this.federationMemberId = federationMemberId;
        this.federationUser = federationUser;
    }

    @Override
    public Quota send() throws RemoteRequestException {
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
        Quota quota = getUserQuotaFromResponse(response);
        return quota;
    }

    private IQ createIq() {
        IQ iq = new IQ(IQ.Type.get);
        iq.setTo(this.federationMemberId);

        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_GET_USER_QUOTA.toString());

        Element userElement = iq.getElement().addElement(IqElement.FEDERATION_USER.toString());
        userElement.setText(new Gson().toJson(this.federationUser));

        return iq;
    }

    private Quota getUserQuotaFromResponse(IQ response) throws RemoteRequestException {
        Element queryElement = response.getElement().element(IqElement.QUERY.toString());
        String quotaStr = queryElement.element(IqElement.USER_QUOTA.toString()).getText();

        String instanceClassName = queryElement.element(IqElement.USER_QUOTA_CLASS_NAME.toString()).getText();

        Quota quota = null;
        try {
            quota = (Quota) new Gson().fromJson(quotaStr, Class.forName(instanceClassName));
        } catch (Exception e) {
            throw new RemoteRequestException(e.getMessage());
        }

        return quota;
    }
}