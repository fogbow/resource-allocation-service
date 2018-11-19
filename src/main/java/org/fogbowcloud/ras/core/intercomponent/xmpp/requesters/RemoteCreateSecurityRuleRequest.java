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
import org.fogbowcloud.ras.core.models.securitygroups.Direction;
import org.fogbowcloud.ras.core.models.securitygroups.EtherType;
import org.fogbowcloud.ras.core.models.securitygroups.Protocol;
import org.fogbowcloud.ras.core.models.securitygroups.SecurityGroupRule;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.util.GsonHolder;
import org.xmpp.packet.IQ;

public class RemoteCreateSecurityRuleRequest implements RemoteRequest<Void> {
    private static final Logger LOGGER = Logger.getLogger(RemoteCreateSecurityRuleRequest.class);

    private SecurityGroupRule securityGroupRule;
    private FederationUserToken federationUserToken;
    private String provider;
    private Order majorOrder;

    public RemoteCreateSecurityRuleRequest(SecurityGroupRule securityGroupRule, FederationUserToken federationUserToken,
                                           String provider, Order majorOrder) {
        this.securityGroupRule = securityGroupRule;
        this.federationUserToken = federationUserToken;
        this.provider = provider;
        this.majorOrder = majorOrder;
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
                RemoteMethod.REMOTE_CREATE_SECURITY_RULE.toString());

        Element orderIdElement = queryElement.addElement(IqElement.ORDER_ID.toString());
        orderIdElement.setText(majorOrder.getId());

        Element userElement = queryElement.addElement(IqElement.FEDERATION_USER.toString());
        userElement.setText(new Gson().toJson(federationUserToken));

        Element securityRuleElement = queryElement.addElement(IqElement.SECURITY_RULE.toString());
        securityRuleElement.setText(GsonHolder.getInstance().toJson(securityGroupRule));
        return iq;
    }

    public static void main(String[] args) {
        SecurityGroupRule s = new SecurityGroupRule();
        s.setCidr("asdasd");
        s.setDirection(Direction.IN);
        s.setEtherType(EtherType.IPv4);
        s.setPortFrom(1000);
        s.setPortTo(1000);
        s.setProtocol(Protocol.ICMP);

        Order o = new NetworkOrder();
        o.setProvider("ufrgs");

//        System.out.println(new RemoteCreateSecurityRuleRequest(s, o, new FederationUserToken()).marshal());
    }
}
