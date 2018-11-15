package org.fogbowcloud.ras.core.intercomponent.xmpp.handlers;

import com.google.gson.Gson;
import org.dom4j.Element;
import org.fogbowcloud.ras.core.intercomponent.RemoteFacade;
import org.fogbowcloud.ras.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.ras.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.ras.core.intercomponent.xmpp.XmppExceptionToErrorConditionTranslator;
import org.fogbowcloud.ras.core.models.securitygroups.SecurityGroupRule;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
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
        FederationUserToken federationUserToken = unmarshalFederationUserToken(iq);

        IQ response = IQ.createResultIQ(iq);
        try {
            List<SecurityGroupRule> securityGroupRuleList =  RemoteFacade.getInstance().
                    getAllSecurityRules(iq.getFrom().toBareJID(), orderId, federationUserToken);
            updateResponse(response, securityGroupRuleList);
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

    private void updateResponse(IQ response, List<SecurityGroupRule> securityGroupRuleList) {
        Element queryEl = response.getElement().addElement(IqElement.QUERY.toString(), REMOTE_GET_ALL_SECURITY_RULES);
        Element securityGroupRuleListElement = queryEl.addElement(IqElement.SECURITY_RULE_LIST.toString());

        Element imagesMapClassNameElement = queryEl.addElement(IqElement.SECURITY_RULE_LIST_CLASS_NAME.toString());
        imagesMapClassNameElement.setText(securityGroupRuleList.getClass().getName());

        securityGroupRuleListElement.setText(new Gson().toJson(securityGroupRuleList));
    }

    private FederationUserToken unmarshalFederationUserToken(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element federationUserTokenElement = queryElement.element(IqElement.FEDERATION_USER.toString());
        FederationUserToken federationUserToken = new Gson().fromJson(federationUserTokenElement.getText(),
                FederationUserToken.class);
        return federationUserToken;
    }
}
