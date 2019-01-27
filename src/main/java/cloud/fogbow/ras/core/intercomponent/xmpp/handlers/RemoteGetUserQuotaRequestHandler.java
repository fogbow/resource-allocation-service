package cloud.fogbow.ras.core.intercomponent.xmpp.handlers;

import cloud.fogbow.common.models.FederationUser;
import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import cloud.fogbow.ras.core.constants.Messages;
import cloud.fogbow.ras.core.intercomponent.RemoteFacade;
import cloud.fogbow.ras.core.intercomponent.xmpp.IqElement;
import cloud.fogbow.ras.core.intercomponent.xmpp.RemoteMethod;
import cloud.fogbow.ras.core.intercomponent.xmpp.XmppExceptionToErrorConditionTranslator;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.quotas.Quota;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;

public class RemoteGetUserQuotaRequestHandler extends AbstractQueryHandler {
    private static final Logger LOGGER = Logger.getLogger(RemoteGetUserQuotaRequestHandler.class);

    private static final String REMOTE_GET_USER_QUOTA = RemoteMethod.REMOTE_GET_USER_QUOTA.toString();

    public RemoteGetUserQuotaRequestHandler() {
        super(REMOTE_GET_USER_QUOTA);
    }

    @Override
    public IQ handle(IQ iq) {
        LOGGER.info(String.format(Messages.Info.RECEIVING_REMOTE_REQUEST, iq.getID()));
        String cloudName = unmarshalCloudName(iq);
        FederationUser federationUser = unmarshalFederatedUser(iq);
        ResourceType resourceType = unmarshalInstanceType(iq);

        IQ response = IQ.createResultIQ(iq);

        try {
            Quota userQuota = RemoteFacade.getInstance().getUserQuota(iq.getFrom().toBareJID(),
                    cloudName, federationUser, resourceType);
            updateResponse(response, userQuota);
        } catch (Exception e) {
            XmppExceptionToErrorConditionTranslator.updateErrorCondition(response, e);
        }
        return response;
    }

    private String unmarshalCloudName(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());

        Element cloudNameElement = queryElement.element(IqElement.CLOUD_NAME.toString());
        String cloudName = new Gson().fromJson(cloudNameElement.getText(), String.class);
        return cloudName;
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