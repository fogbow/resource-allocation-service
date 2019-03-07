package cloud.fogbow.ras.core.intercomponent.xmpp.handlers;

import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.core.intercomponent.RemoteFacade;
import cloud.fogbow.ras.core.intercomponent.xmpp.IqElement;
import cloud.fogbow.ras.core.intercomponent.xmpp.RemoteMethod;
import cloud.fogbow.ras.core.intercomponent.xmpp.XmppExceptionToErrorConditionTranslator;
import cloud.fogbow.ras.api.http.response.securityrules.SecurityRule;
import com.google.gson.Gson;
import org.dom4j.Element;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;

import java.util.List;

public class RemoteGetAllSecurityRuleHandler extends AbstractQueryHandler {

    private static final String REMOTE_GET_ALL_SECURITY_RULES = RemoteMethod.REMOTE_GET_ALL_SECURITY_RULES.toString();

    public RemoteGetAllSecurityRuleHandler() {
        super(RemoteMethod.REMOTE_GET_ALL_SECURITY_RULES.toString());
    }

    @Override
    public IQ handle(IQ iq) {
        String orderId = unmarshalOrderId(iq);
        SystemUser systemUser = unmarshalFederationUserToken(iq);

        IQ response = IQ.createResultIQ(iq);
        try {
            List<SecurityRule> securityRuleList =  RemoteFacade.getInstance().
                    getAllSecurityRules(iq.getFrom().toBareJID(), orderId, systemUser);
            updateResponse(response, securityRuleList);
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

    private void updateResponse(IQ response, List<SecurityRule> securityRuleList) {
        Element queryEl = response.getElement().addElement(IqElement.QUERY.toString(), REMOTE_GET_ALL_SECURITY_RULES);
        Element securityRuleListElement = queryEl.addElement(IqElement.SECURITY_RULE_LIST.toString());

        Element imagesMapClassNameElement = queryEl.addElement(IqElement.SECURITY_RULE_LIST_CLASS_NAME.toString());
        imagesMapClassNameElement.setText(securityRuleList.getClass().getName());

        securityRuleListElement.setText(new Gson().toJson(securityRuleList));
    }

    private SystemUser unmarshalFederationUserToken(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element federationUserElement = queryElement.element(IqElement.FEDERATION_USER.toString());
        SystemUser systemUser = new Gson().fromJson(federationUserElement.getText(), SystemUser.class);
        return systemUser;
    }
}
