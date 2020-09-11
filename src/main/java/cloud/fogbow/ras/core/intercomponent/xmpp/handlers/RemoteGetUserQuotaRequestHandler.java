package cloud.fogbow.ras.core.intercomponent.xmpp.handlers;

import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.util.IntercomponentUtil;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.intercomponent.RemoteFacade;
import cloud.fogbow.ras.core.intercomponent.xmpp.IqElement;
import cloud.fogbow.ras.core.intercomponent.xmpp.RemoteMethod;
import cloud.fogbow.ras.core.intercomponent.xmpp.XmppExceptionToErrorConditionTranslator;
import cloud.fogbow.ras.api.http.response.quotas.Quota;
import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
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
        LOGGER.debug(String.format(Messages.Log.RECEIVING_REMOTE_REQUEST_S, iq.getID()));
        String cloudName = unmarshalCloudName(iq);
        SystemUser systemUser = unmarshalFederatedUser(iq);

        IQ response = IQ.createResultIQ(iq);

        try {
            String senderId = IntercomponentUtil.getSender(iq.getFrom().toBareJID(), SystemConstants.XMPP_SERVER_NAME_PREFIX);
            Quota userQuota = RemoteFacade.getInstance().getUserQuota(senderId, cloudName, systemUser);
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

    private SystemUser unmarshalFederatedUser(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());

        Element systemUserElement = queryElement.element(IqElement.SYSTEM_USER.toString());
        SystemUser systemUser = new Gson().fromJson(systemUserElement.getText(), SystemUser.class);
        return systemUser;
    }

    private void updateResponse(IQ iq, Quota quota) {
        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(), REMOTE_GET_USER_QUOTA);
        Element instanceElement = queryElement.addElement(IqElement.USER_QUOTA.toString());

        Element instanceClassNameElement = queryElement.addElement(IqElement.USER_QUOTA_CLASS_NAME.toString());
        instanceClassNameElement.setText(quota.getClass().getName());

        instanceElement.setText(new Gson().toJson(quota));
    }
}