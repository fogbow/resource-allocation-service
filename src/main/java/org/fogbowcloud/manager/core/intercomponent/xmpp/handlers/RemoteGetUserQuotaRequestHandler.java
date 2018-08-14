package org.fogbowcloud.manager.core.intercomponent.xmpp.handlers;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.manager.core.intercomponent.RemoteFacade;
import org.fogbowcloud.manager.core.intercomponent.xmpp.XmppExceptionToErrorConditionTranslator;
import org.fogbowcloud.manager.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.quotas.Quota;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
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
    	LOGGER.info("Received request for order: " + iq.getID());
        String memberId = unmarshalMemberId(iq);
        FederationUser federationUser = unmarshalFederatedUser(iq);
        ResourceType resourceType = unmarshalInstanceType(iq);

        IQ response = IQ.createResultIQ(iq);

        try {
            Quota userQuota = RemoteFacade.getInstance().getUserQuota(memberId, federationUser, resourceType);
            updateResponse(response, userQuota);
        } catch (Exception e) {
            XmppExceptionToErrorConditionTranslator.updateErrorCondition(response, e);
        }
        return response;
    }
    
    private String unmarshalMemberId(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());

        Element memberIdElement = queryElement.element(IqElement.MEMBER_ID.toString());
        String memberId = new Gson().fromJson(memberIdElement.getText(), String.class);
        return memberId;
    }
    
    private FederationUser unmarshalFederatedUser(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());

        Element federationUserElement = queryElement.element(IqElement.FEDERATION_USER.toString());
        FederationUser federationUser = new Gson().fromJson(federationUserElement.getText(), FederationUser.class);
        return federationUser;
    }
    
    private ResourceType unmarshalInstanceType(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());

        Element instanceTypeElementRequest = queryElement.element(IqElement.INSTANCE_TYPE.toString());
        ResourceType resourceType = new Gson().fromJson(instanceTypeElementRequest.getText(), ResourceType.class);
        return resourceType;
    }
    
    private void updateResponse(IQ iq, Quota quota) {
        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(), REMOTE_GET_USER_QUOTA);
        Element instanceElement = queryElement.addElement(IqElement.USER_QUOTA.toString());
        
        Element instanceClassNameElement = queryElement.addElement(IqElement.USER_QUOTA_CLASS_NAME.toString());
        instanceClassNameElement.setText(quota.getClass().getName());
        
        instanceElement.setText(new Gson().toJson(quota));
    }
}