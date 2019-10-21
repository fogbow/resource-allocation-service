package cloud.fogbow.ras.core.intercomponent.xmpp.handlers;

import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.IntercomponentUtil;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.intercomponent.RemoteFacade;
import cloud.fogbow.ras.core.intercomponent.xmpp.IqElement;
import cloud.fogbow.ras.core.intercomponent.xmpp.RemoteMethod;
import cloud.fogbow.ras.core.intercomponent.xmpp.XmppExceptionToErrorConditionTranslator;
import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;

public class RemoteCreateSecurityRuleRequestHandler extends AbstractQueryHandler {
    public static final Logger LOGGER = Logger.getLogger(RemoteCreateSecurityRuleRequestHandler.class);

    public RemoteCreateSecurityRuleRequestHandler() {
        super(RemoteMethod.REMOTE_CREATE_SECURITY_RULE.toString());
    }

    @Override
    public IQ handle(IQ iq) {
        LOGGER.debug(String.format(Messages.Info.RECEIVING_REMOTE_REQUEST, iq.getID()));
        String orderId = unmarshalOrderId(iq);
        SystemUser systemUser = unmarshalFederationUserToken(iq);
        SecurityRule securityRule = unmarshalSecurityRule(iq);

        IQ response = IQ.createResultIQ(iq);
        try {
            String senderId = IntercomponentUtil.getSender(iq.getFrom().toBareJID(), SystemConstants.XMPP_SERVER_NAME_PREFIX);
            RemoteFacade.getInstance().createSecurityRule(senderId, orderId, securityRule, systemUser);
        } catch (Throwable e) {
            XmppExceptionToErrorConditionTranslator.updateErrorCondition(response, e);
        }

        return response;
    }

    private SecurityRule unmarshalSecurityRule(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element securityRuleElement = queryElement.element(IqElement.SECURITY_RULE.toString());
        return GsonHolder.getInstance().fromJson(securityRuleElement.getText(), SecurityRule.class);
    }

    private String unmarshalOrderId(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element orderIdElement = queryElement.element(IqElement.ORDER_ID.toString());
        return orderIdElement.getText();
    }

    private SystemUser unmarshalFederationUserToken(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element systemUserElement = queryElement.element(IqElement.SYSTEM_USER.toString());
        SystemUser systemUser = new Gson().fromJson(systemUserElement.getText(), SystemUser.class);
        return systemUser;
    }

}
