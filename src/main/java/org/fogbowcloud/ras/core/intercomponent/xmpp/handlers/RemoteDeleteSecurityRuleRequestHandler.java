package org.fogbowcloud.ras.core.intercomponent.xmpp.handlers;

import com.google.gson.Gson;
import org.dom4j.Element;
import org.fogbowcloud.ras.core.intercomponent.RemoteFacade;
import org.fogbowcloud.ras.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.ras.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.ras.core.intercomponent.xmpp.XmppExceptionToErrorConditionTranslator;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;

public class RemoteDeleteSecurityRuleRequestHandler extends AbstractQueryHandler {

    public RemoteDeleteSecurityRuleRequestHandler() {
        super(RemoteMethod.REMOTE_DELETE_SECURITY_RULE.toString());
    }

    @Override
    public IQ handle(IQ iq) {
        String providerId = iq.getTo().toBareJID();
        String ruleId = unmarshalRuleId(iq);
        FederationUserToken federationUserToken = unmarshalFederationUserToken(iq);

        IQ response = IQ.createResultIQ(iq);
        try {
            RemoteFacade.getInstance().deleteSecurityRule(iq.getFrom().toBareJID(), providerId, ruleId, federationUserToken);
        } catch (Throwable e) {
            XmppExceptionToErrorConditionTranslator.updateErrorCondition(response, e);
        }

        return response;
    }

    private String unmarshalRuleId(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element ruleIdElement = queryElement.element(IqElement.RULE_ID.toString());
        return ruleIdElement.getText();
    }

    private FederationUserToken unmarshalFederationUserToken(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element federationUserTokenElement = queryElement.element(IqElement.FEDERATION_USER.toString());
        FederationUserToken federationUserToken = new Gson().fromJson(federationUserTokenElement.getText(),
                FederationUserToken.class);
        return federationUserToken;
    }
}
