package cloud.fogbow.ras.core.intercomponent.xmpp.requesters;

import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.intercomponent.xmpp.IqElement;
import cloud.fogbow.ras.core.intercomponent.xmpp.PacketSenderHolder;
import cloud.fogbow.ras.core.intercomponent.xmpp.RemoteMethod;
import cloud.fogbow.ras.core.intercomponent.xmpp.XmppErrorConditionToExceptionTranslator;
import cloud.fogbow.ras.core.models.orders.Order;
import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.xmpp.packet.IQ;

public class RemoteCreateSecurityRuleRequest implements RemoteRequest<Void> {
    private static final Logger LOGGER = Logger.getLogger(RemoteCreateSecurityRuleRequest.class);

    private SecurityRule securityRule;
    private SystemUser systemUser;
    private String provider;
    private Order majorOrder;

    public RemoteCreateSecurityRuleRequest(SecurityRule securityRule, SystemUser systemUser,
                                           String provider, Order majorOrder) {
        this.securityRule = securityRule;
        this.systemUser = systemUser;
        this.provider = provider;
        this.majorOrder = majorOrder;
    }

    @Override
    public Void send() throws Exception {
        IQ iq = marshal();

        LOGGER.debug(String.format(Messages.Info.SENDING_MSG, iq.getID()));
        IQ response = (IQ) PacketSenderHolder.getPacketSender().syncSendPacket(iq);
        XmppErrorConditionToExceptionTranslator.handleError(response, this.provider);
        LOGGER.debug(Messages.Info.SUCCESS);
        return null;
    }

    private IQ marshal() {
        IQ iq = new IQ(IQ.Type.set);
        iq.setTo(provider);

        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_CREATE_SECURITY_RULE.toString());

        Element orderIdElement = queryElement.addElement(IqElement.ORDER_ID.toString());
        orderIdElement.setText(majorOrder.getId());

        Element userElement = queryElement.addElement(IqElement.SYSTEM_USER.toString());
        userElement.setText(new Gson().toJson(systemUser));

        Element securityRuleElement = queryElement.addElement(IqElement.SECURITY_RULE.toString());
        securityRuleElement.setText(GsonHolder.getInstance().toJson(securityRule));
        return iq;
    }
}
