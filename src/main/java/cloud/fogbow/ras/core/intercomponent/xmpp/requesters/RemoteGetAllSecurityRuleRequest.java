package cloud.fogbow.ras.core.intercomponent.xmpp.requesters;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.intercomponent.xmpp.IqElement;
import cloud.fogbow.ras.core.intercomponent.xmpp.PacketSenderHolder;
import cloud.fogbow.ras.core.intercomponent.xmpp.RemoteMethod;
import cloud.fogbow.ras.core.intercomponent.xmpp.XmppErrorConditionToExceptionTranslator;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.xmpp.packet.IQ;

import java.util.List;

public class RemoteGetAllSecurityRuleRequest implements RemoteRequest<List<SecurityRuleInstance>> {
    private static final Logger LOGGER = Logger.getLogger(RemoteGetAllSecurityRuleRequest.class);

    private String provider;
    private String orderId;
    private SystemUser systemUser;

    public RemoteGetAllSecurityRuleRequest(String provider, String orderId, SystemUser systemUser) {
        this.provider = provider;
        this.orderId = orderId;
        this.systemUser = systemUser;
    }

    @Override
    public List<SecurityRuleInstance> send() throws Exception {
        IQ iq = marshal();
        LOGGER.debug(String.format(Messages.Info.SENDING_MSG, iq.getID()));
        IQ response = (IQ) PacketSenderHolder.getPacketSender().syncSendPacket(iq);

        XmppErrorConditionToExceptionTranslator.handleError(response, provider);

        LOGGER.debug(Messages.Info.SUCCESS);
        return unmarshalSecurityRules(response);
    }

    private IQ marshal() {
        IQ iq = new IQ(IQ.Type.get);
        iq.setTo(SystemConstants.JID_SERVICE_NAME + SystemConstants.JID_CONNECTOR + SystemConstants.XMPP_SERVER_NAME_PREFIX + provider);

        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_GET_ALL_SECURITY_RULES.toString());

        Element userElement = queryElement.addElement(IqElement.SYSTEM_USER.toString());
        userElement.setText(new Gson().toJson(systemUser));

        Element orderIdElement = queryElement.addElement(IqElement.ORDER_ID.toString());
        orderIdElement.setText(orderId);

        return iq;
    }

    private List<SecurityRuleInstance> unmarshalSecurityRules(IQ response) throws UnexpectedException {
        Element queryElement = response.getElement().element(IqElement.QUERY.toString());
        String listStr = queryElement.element(IqElement.SECURITY_RULE_LIST.toString()).getText();

        String instanceClassName = queryElement.element(IqElement.SECURITY_RULE_LIST_CLASS_NAME.toString()).getText();

        List<SecurityRuleInstance> rulesList;
        try {
            rulesList = (List<SecurityRuleInstance>) new Gson().fromJson(listStr, Class.forName(instanceClassName));
        } catch (Exception e) {
            throw new UnexpectedException(e.getMessage());
        }

        return rulesList;
    }
}
