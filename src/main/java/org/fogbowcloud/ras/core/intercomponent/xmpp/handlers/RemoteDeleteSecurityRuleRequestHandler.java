package org.fogbowcloud.ras.core.intercomponent.xmpp.handlers;

import org.dom4j.Element;
import org.fogbowcloud.ras.core.intercomponent.RemoteFacade;
import org.fogbowcloud.ras.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.ras.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.ras.core.intercomponent.xmpp.XmppExceptionToErrorConditionTranslator;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;

public class RemoteDeleteSecurityRuleRequestHandler extends AbstractQueryHandler {

    public RemoteDeleteSecurityRuleRequestHandler() {
        super(RemoteMethod.REMOTE_DELETE_SECURITY_RULE.toString());
    }

    @Override
    public IQ handle(IQ iq) {
        String orderId = unmarshalOrderId(iq);
        String ruleId = unmarshalRuleId(iq);

        IQ response = IQ.createResultIQ(iq);
        try {
            RemoteFacade.getInstance().deleteSecurityRule(iq.getFrom().toBareJID(), orderId, ruleId);
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

    private String unmarshalRuleId(IQ iq) {
        Element queryElement = iq.getElement().element(IqElement.QUERY.toString());
        Element ruleIdElement = queryElement.element(IqElement.RULE_ID.toString());
        return ruleIdElement.getText();
    }
}
