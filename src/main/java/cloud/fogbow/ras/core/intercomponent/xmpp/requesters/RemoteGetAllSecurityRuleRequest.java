package cloud.fogbow.ras.core.intercomponent.xmpp.requesters;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.FederationUser;
import cloud.fogbow.ras.core.intercomponent.xmpp.IqElement;
import cloud.fogbow.ras.core.intercomponent.xmpp.PacketSenderHolder;
import cloud.fogbow.ras.core.intercomponent.xmpp.RemoteMethod;
import cloud.fogbow.ras.core.intercomponent.xmpp.XmppErrorConditionToExceptionTranslator;
import cloud.fogbow.ras.core.models.securityrules.SecurityRule;
import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.xmpp.packet.IQ;

import java.util.List;

public class RemoteGetAllSecurityRuleRequest implements RemoteRequest<List<SecurityRule>> {
    private static final Logger LOGGER = Logger.getLogger(RemoteGetAllSecurityRuleRequest.class);

    private String provider;
    private String orderId;
    private FederationUser federationUser;

    public RemoteGetAllSecurityRuleRequest(String provider, String orderId, FederationUser federationUser) {
        this.provider = provider;
        this.orderId = orderId;
        this.federationUser = federationUser;
    }

    @Override
    public List<SecurityRule> send() throws Exception {
        IQ iq = marshal();
        IQ response = (IQ) PacketSenderHolder.getPacketSender().syncSendPacket(iq);

        XmppErrorConditionToExceptionTranslator.handleError(response, provider);

        return unmarshalSecurityRules(response);
    }

    private IQ marshal() {
        IQ iq = new IQ(IQ.Type.get);
        iq.setTo(provider);

        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_GET_ALL_SECURITY_RULES.toString());

        Element userElement = queryElement.addElement(IqElement.FEDERATION_USER.toString());
        userElement.setText(new Gson().toJson(federationUser));

        Element orderIdElement = queryElement.addElement(IqElement.ORDER_ID.toString());
        orderIdElement.setText(orderId);

        return iq;
    }

    private List<SecurityRule> unmarshalSecurityRules(IQ response) throws UnexpectedException {
        Element queryElement = response.getElement().element(IqElement.QUERY.toString());
        String listStr = queryElement.element(IqElement.SECURITY_RULE_LIST.toString()).getText();

        String instanceClassName = queryElement.element(IqElement.SECURITY_RULE_LIST_CLASS_NAME.toString()).getText();

        List<SecurityRule> rulesList;
        try {
            rulesList = (List<SecurityRule>) new Gson().fromJson(listStr, Class.forName(instanceClassName));
        } catch (Exception e) {
            throw new UnexpectedException(e.getMessage());
        }

        return rulesList;
    }
}
