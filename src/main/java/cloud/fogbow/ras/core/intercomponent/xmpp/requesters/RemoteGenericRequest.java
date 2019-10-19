package cloud.fogbow.ras.core.intercomponent.xmpp.requesters;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.connectivity.FogbowGenericResponse;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.intercomponent.xmpp.IqElement;
import cloud.fogbow.ras.core.intercomponent.xmpp.PacketSenderHolder;
import cloud.fogbow.ras.core.intercomponent.xmpp.RemoteMethod;
import cloud.fogbow.ras.core.intercomponent.xmpp.XmppErrorConditionToExceptionTranslator;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.xmpp.packet.IQ;

public class RemoteGenericRequest implements RemoteRequest<FogbowGenericResponse> {
    private static final Logger LOGGER = Logger.getLogger(RemoteGenericRequest.class);

    private SystemUser systemUser;
    private String genericRequest;
    private String cloudName;
    private String provider;

    public RemoteGenericRequest(String provider, String cloudName, String genericRequest, SystemUser systemUser) {
        this.systemUser = systemUser;
        this.provider = provider;
        this.cloudName = cloudName;
        this.genericRequest = genericRequest;
    }

    @Override
    public FogbowGenericResponse send() throws Exception {
        IQ iq = marshal(provider, cloudName, genericRequest, systemUser);

        LOGGER.debug(String.format(Messages.Info.SENDING_MSG, iq.getID()));
        IQ response = (IQ) PacketSenderHolder.getPacketSender().syncSendPacket(iq);
        XmppErrorConditionToExceptionTranslator.handleError(response, this.provider);
        LOGGER.debug(Messages.Info.SUCCESS);
        return unmarshal(response);
    }

    private FogbowGenericResponse unmarshal(IQ response) throws UnexpectedException {
        Element queryElement = response.getElement().element(IqElement.QUERY.toString());
        String genericRequestResponseStr = queryElement.element(IqElement.GENERIC_REQUEST_RESPONSE.toString()).getText();
        String instanceClassName = queryElement.element(IqElement.GENERIC_REQUEST_RESPONSE_CLASS_NAME.toString()).getText();

        FogbowGenericResponse fogbowGenericResponse;
        try {
            fogbowGenericResponse = (FogbowGenericResponse) GsonHolder.getInstance().
                    fromJson(genericRequestResponseStr, Class.forName(instanceClassName));
        } catch (Exception e) {
            throw new UnexpectedException(e.getMessage());
        }
        return fogbowGenericResponse;
    }

    public static IQ marshal(String provider, String cloudName, String genericRequest, SystemUser systemUser) {
        IQ iq = new IQ(IQ.Type.set);
        iq.setTo(SystemConstants.JID_SERVICE_NAME + SystemConstants.JID_CONNECTOR + SystemConstants.XMPP_SERVER_NAME_PREFIX + provider);

        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_GENERIC_REQUEST.toString());

        Element cloudNameElement = queryElement.addElement(IqElement.CLOUD_NAME.toString());
        cloudNameElement.setText(cloudName);

        Element userElement = queryElement.addElement(IqElement.SYSTEM_USER.toString());
        userElement.setText(GsonHolder.getInstance().toJson(systemUser));

        Element genericRequestElement = queryElement.addElement(IqElement.GENERIC_REQUEST.toString());
        genericRequestElement.setText(GsonHolder.getInstance().toJson(genericRequest));

        return iq;
    }
}