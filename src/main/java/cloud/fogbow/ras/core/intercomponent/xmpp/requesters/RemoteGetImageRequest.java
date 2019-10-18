package cloud.fogbow.ras.core.intercomponent.xmpp.requesters;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.intercomponent.xmpp.IqElement;
import cloud.fogbow.ras.core.intercomponent.xmpp.PacketSenderHolder;
import cloud.fogbow.ras.core.intercomponent.xmpp.RemoteMethod;
import cloud.fogbow.ras.core.intercomponent.xmpp.XmppErrorConditionToExceptionTranslator;
import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.xmpp.packet.IQ;

public class RemoteGetImageRequest implements RemoteRequest<ImageInstance> {
    private static final Logger LOGGER = Logger.getLogger(RemoteGetImageRequest.class);

    private String provider;
    private String cloudName;
    private String imageId;
    private SystemUser systemUser;

    public RemoteGetImageRequest(String provider, String cloudName, String imageId, SystemUser systemUser) {
        this.provider = provider;
        this.cloudName = cloudName;
        this.imageId = imageId;
        this.systemUser = systemUser;
    }

    @Override
    public ImageInstance send() throws Exception {
        IQ iq = marshal(this.provider, this.cloudName, this.imageId, this.systemUser);
        LOGGER.debug(String.format(Messages.Info.SENDING_MSG, iq.getID()));
        IQ response = (IQ) PacketSenderHolder.getPacketSender().syncSendPacket(iq);
        XmppErrorConditionToExceptionTranslator.handleError(response, this.provider);
        LOGGER.debug(Messages.Info.SUCCESS);
        return unmarshalImage(response);
    }

    public static IQ marshal(String provider, String cloudName, String imageId, SystemUser systemUser) {
        IQ iq = new IQ(IQ.Type.get);
        iq.setTo(SystemConstants.JID_SERVICE_NAME + SystemConstants.JID_CONNECTOR + SystemConstants.XMPP_SERVER_NAME_PREFIX + provider);

        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_GET_IMAGE.toString());

        Element cloudNameElement = queryElement.addElement(IqElement.CLOUD_NAME.toString());
        cloudNameElement.setText(cloudName);

        Element imageIdElement = queryElement.addElement(IqElement.IMAGE_ID.toString());
        imageIdElement.setText(imageId);

        Element userElement = queryElement.addElement(IqElement.SYSTEM_USER.toString());
        userElement.setText(new Gson().toJson(systemUser));

        return iq;
    }

    private ImageInstance unmarshalImage(IQ response) throws UnexpectedException {
        Element queryElement = response.getElement().element(IqElement.QUERY.toString());
        String imageStr = queryElement.element(IqElement.IMAGE.toString()).getText();

        String instanceClassName = queryElement.element(IqElement.IMAGE_CLASS_NAME.toString()).getText();

        try {
            return (ImageInstance) new Gson().fromJson(imageStr, Class.forName(instanceClassName));
        } catch (Exception e) {
            throw new UnexpectedException(e.getMessage());
        }
    }
}