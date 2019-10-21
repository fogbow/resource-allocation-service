package cloud.fogbow.ras.core.intercomponent.xmpp.handlers;

import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.util.IntercomponentUtil;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.intercomponent.RemoteFacade;
import cloud.fogbow.ras.core.intercomponent.xmpp.IqElement;
import cloud.fogbow.ras.core.intercomponent.xmpp.RemoteMethod;
import cloud.fogbow.ras.core.intercomponent.xmpp.XmppExceptionToErrorConditionTranslator;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;

import java.util.List;

public class RemoteGetAllSecurityRuleHandler extends AbstractQueryHandler {
    private static final Logger LOGGER = Logger.getLogger(RemoteGetAllSecurityRuleHandler.class);

    private static final String REMOTE_GET_ALL_SECURITY_RULES = RemoteMethod.REMOTE_GET_ALL_SECURITY_RULES.toString();

    public RemoteGetAllSecurityRuleHandler() {
        super(RemoteMethod.REMOTE_GET_ALL_SECURITY_RULES.toString());
    }

    @Override
    public IQ handle(IQ iq) {
        LOGGER.debug(String.format(Messages.Info.RECEIVING_REMOTE_REQUEST, iq.getID()));
        String orderId = unmarshalOrderId(iq);
        SystemUser systemUser = unmarshalFederationUserToken(iq);

        IQ response = IQ.createResultIQ(iq);
        try {
            String senderId = IntercomponentUtil.getSender(iq.getFrom().toBareJID(), SystemConstants.XMPP_SERVER_NAME_PREFIX);
            List<SecurityRuleInstance> securityRuleInstanceList =  RemoteFacade.getInstance().
                    getAllSecurityRules(senderId, orderId, systemUser);
            updateResponse(response, securityRuleInstanceList);
        } catch (Throwable e) {
            XmppExceptionToErrorConditionTranslator.updateErrorCondition(response, e);
        }

        return response;
    }

    private String unmarshalOrderId(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element orderIdElement = queryElement.element(IqElement.ORDER_ID.toString());
        return orderIdElement.getText();
    }

    private void updateResponse(IQ response, List<SecurityRuleInstance> securityRuleInstanceList) {
        Element queryEl = response.getElement().addElement(IqElement.QUERY.toString(), REMOTE_GET_ALL_SECURITY_RULES);
        Element securityRuleListElement = queryEl.addElement(IqElement.SECURITY_RULE_LIST.toString());

        Element imagesMapClassNameElement = queryEl.addElement(IqElement.SECURITY_RULE_LIST_CLASS_NAME.toString());
        imagesMapClassNameElement.setText(securityRuleInstanceList.getClass().getName());

        securityRuleListElement.setText(new Gson().toJson(securityRuleInstanceList));
    }

    private SystemUser unmarshalFederationUserToken(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element systemUserElement = queryElement.element(IqElement.SYSTEM_USER.toString());
        SystemUser systemUser = new Gson().fromJson(systemUserElement.getText(), SystemUser.class);
        return systemUser;
    }
}
