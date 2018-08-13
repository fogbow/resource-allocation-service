package org.fogbowcloud.manager.core.intercomponent.xmpp.requesters;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnavailableProviderException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.intercomponent.xmpp.XmppErrorConditionToExceptionTranslator;
import org.fogbowcloud.manager.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.manager.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.quotas.Quota;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.xmpp.packet.IQ;
import com.google.gson.Gson;

public class RemoteGetUserQuotaRequest implements RemoteRequest<Quota> {

    private static final Logger LOGGER = Logger.getLogger(RemoteGetUserQuotaRequest.class);

    private String provider;
    private FederationUser federationUser;
    private ResourceType resourceType;

    public RemoteGetUserQuotaRequest(String provider, FederationUser federationUser, ResourceType resourceType) {
        this.provider = provider;
        this.federationUser = federationUser;
        this.resourceType = resourceType;
    }

    @Override
    public Quota send() throws Exception {
        IQ iq = createIq();
        IQ response = (IQ) PacketSenderHolder.getPacketSender().syncSendPacket(iq);

        XmppErrorConditionToExceptionTranslator.handleError(response, this.provider);
        Quota quota = getUserQuotaFromResponse(response);
        return quota;
    }

    public IQ createIq() {
        IQ iq = new IQ(IQ.Type.get);
        iq.setTo(this.provider);

        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_GET_USER_QUOTA.toString());

        Element memberIdElement = queryElement.addElement(IqElement.MEMBER_ID.toString());
        memberIdElement.setText(new Gson().toJson(this.provider));

        Element userElement = queryElement.addElement(IqElement.FEDERATION_USER.toString());
        userElement.setText(new Gson().toJson(this.federationUser));

        Element orderTypeElement = queryElement.addElement(IqElement.INSTANCE_TYPE.toString());
        orderTypeElement.setText(this.resourceType.toString());

        return iq;
    }

    private Quota getUserQuotaFromResponse(IQ response) throws UnexpectedException {
        Element queryElement = response.getElement().element(IqElement.QUERY.toString());
        String quotaStr = queryElement.element(IqElement.USER_QUOTA.toString()).getText();

        String instanceClassName = queryElement.element(IqElement.USER_QUOTA_CLASS_NAME.toString()).getText();

        Quota quota = null;
        try {
            quota = (Quota) new Gson().fromJson(quotaStr, Class.forName(instanceClassName));
        } catch (Exception e) {
            throw new UnexpectedException(e.getMessage());
        }

        return quota;
    }
}