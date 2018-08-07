package org.fogbowcloud.manager.core.intercomponent.xmpp.handlers;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.manager.core.intercomponent.RemoteFacade;
import org.fogbowcloud.manager.core.intercomponent.xmpp.XmppExceptionToErrorConditionTranslator;
import org.fogbowcloud.manager.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.quotas.Quota;
import org.fogbowcloud.manager.core.models.tokens.FederationUserAttributes;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;
import com.google.gson.Gson;

public class RemoteGetUserQuotaRequestHandler extends AbstractQueryHandler {

    private static final Logger LOGGER = Logger.getLogger(RemoteGetUserQuotaRequestHandler.class);

    public static final String REMOTE_GET_USER_QUOTA = RemoteMethod.REMOTE_GET_USER_QUOTA.toString();

    public RemoteGetUserQuotaRequestHandler() {
        super(REMOTE_GET_USER_QUOTA);
    }

    @Override
    public IQ handle(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());

        Element memberIdElement = iq.getElement().element(IqElement.MEMBER_ID.toString());
        String memberId = new Gson().fromJson(memberIdElement.getText(), String.class);

        Element federationUserElement = iq.getElement().element(IqElement.FEDERATION_USER.toString());
        FederationUserAttributes federationUserAttributes = new Gson().fromJson(federationUserElement.getText(), FederationUserAttributes.class);

        Element instanceTypeElementRequest = queryElement.element(IqElement.INSTANCE_TYPE.toString());
        ResourceType resourceType = new Gson().fromJson(instanceTypeElementRequest.getText(), ResourceType.class);

        IQ response = IQ.createResultIQ(iq);

        try {
            Quota userQuota = RemoteFacade.getInstance().getUserQuota(memberId, federationUserAttributes, resourceType);

            Element queryEl = response.getElement().addElement(IqElement.QUERY.toString(), REMOTE_GET_USER_QUOTA);
            Element instanceElement = queryEl.addElement(IqElement.USER_QUOTA.toString());

            Element instanceClassNameElement = queryEl.addElement(IqElement.USER_QUOTA_CLASS_NAME.toString());
            instanceClassNameElement.setText(userQuota.getClass().getName());

            instanceElement.setText(new Gson().toJson(userQuota));
        } catch (Exception e) {
            XmppExceptionToErrorConditionTranslator.updateErrorCondition(response, e);
        }
        return response;
    }
}