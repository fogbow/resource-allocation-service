package org.fogbowcloud.ras.core.intercomponent.xmpp.requesters;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.ras.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.ras.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.ras.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.ras.core.intercomponent.xmpp.XmppErrorConditionToExceptionTranslator;
import org.fogbowcloud.ras.core.models.orders.NetworkOrder;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.xmpp.packet.IQ;

public class RemoteDeleteSecurityRuleRequest implements RemoteRequest<Void> {

    private static final Logger LOGGER = Logger.getLogger(RemoteDeleteSecurityRuleRequest.class);

    private String ruleId;
    private String provider;
    private FederationUserToken federationUserToken;

    public RemoteDeleteSecurityRuleRequest(String ruleId, String provider, FederationUserToken federationUserToken) {
        this.ruleId = ruleId;
        this.provider = provider;
        this.federationUserToken = federationUserToken;
    }

    @Override
    public Void send() throws Exception {
        IQ iq = marshal();

        IQ response = (IQ) PacketSenderHolder.getPacketSender().syncSendPacket(iq);
        XmppErrorConditionToExceptionTranslator.handleError(response, this.provider);
        return null;
    }

    private IQ marshal() {
        IQ iq = new IQ(IQ.Type.set);
        iq.setTo(provider);

        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_DELETE_SECURITY_RULE.toString());

        Element userElement = queryElement.addElement(IqElement.FEDERATION_USER.toString());
        userElement.setText(new Gson().toJson(federationUserToken));

        Element ruleIdElement = queryElement.addElement(IqElement.RULE_ID.toString());
        ruleIdElement.setText(ruleId);

        return iq;
    }

    public static void main(String[] args) {
        Order o = new NetworkOrder();
        String ruleId = "fake-ruleId";
//        System.out.println(new RemoteDeleteSecurityRuleRequest(o, ruleId).marshal());

    }
}
