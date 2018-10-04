package org.fogbowcloud.ras.core.intercomponent.xmpp.requesters;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.ras.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.ras.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.ras.core.intercomponent.xmpp.XmppErrorConditionToExceptionTranslator;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.quotas.Quota;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.xmpp.packet.IQ;

public class RemoteGetUserQuotaRequest implements RemoteRequest<Quota> {
    private static final Logger LOGGER = Logger.getLogger(RemoteGetUserQuotaRequest.class);

    private String provider;
    private FederationUserToken federationUserToken;
    private ResourceType resourceType;

    public RemoteGetUserQuotaRequest(String provider, FederationUserToken federationUserToken,
                                     ResourceType resourceType) {
        this.provider = provider;
        this.federationUserToken = federationUserToken;
        this.resourceType = resourceType;
    }

    @Override
    public Quota send() throws Exception {
        IQ iq = marshal(this.provider, this.federationUserToken, this.resourceType);
        IQ response = (IQ) PacketSenderHolder.getPacketSender().syncSendPacket(iq);

        XmppErrorConditionToExceptionTranslator.handleError(response, this.provider);
        Quota quota = unmarshalUserQuota(response);
        return quota;
    }

    public static IQ marshal(String provider, FederationUserToken federationUserToken, ResourceType resourceType) {
        IQ iq = new IQ(IQ.Type.get);
        iq.setTo(provider);

        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_GET_USER_QUOTA.toString());

        Element memberIdElement = queryElement.addElement(IqElement.MEMBER_ID.toString());
        memberIdElement.setText(new Gson().toJson(provider));

        Element userElement = queryElement.addElement(IqElement.FEDERATION_USER.toString());
        userElement.setText(new Gson().toJson(federationUserToken));

        Element orderTypeElement = queryElement.addElement(IqElement.INSTANCE_TYPE.toString());
        orderTypeElement.setText(resourceType.toString());

        return iq;
    }

    private Quota unmarshalUserQuota(IQ response) throws UnexpectedException {
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