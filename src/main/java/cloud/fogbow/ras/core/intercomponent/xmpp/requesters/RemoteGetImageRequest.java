package cloud.fogbow.ras.core.intercomponent.xmpp.requesters;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.FederationUser;
import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import cloud.fogbow.ras.core.intercomponent.xmpp.IqElement;
import cloud.fogbow.ras.core.intercomponent.xmpp.PacketSenderHolder;
import cloud.fogbow.ras.core.intercomponent.xmpp.RemoteMethod;
import cloud.fogbow.ras.core.intercomponent.xmpp.XmppErrorConditionToExceptionTranslator;
import cloud.fogbow.ras.core.models.images.Image;
import org.xmpp.packet.IQ;

public class RemoteGetImageRequest implements RemoteRequest<Image> {
    private static final Logger LOGGER = Logger.getLogger(RemoteGetImageRequest.class);

    private String provider;
    private String cloudName;
    private String imageId;
    private FederationUser federationUser;

    public RemoteGetImageRequest(String provider, String cloudName, String imageId, FederationUser federationUser) {
        this.provider = provider;
        this.cloudName = cloudName;
        this.imageId = imageId;
        this.federationUser = federationUser;
    }

    @Override
    public Image send() throws Exception {
        IQ request = marshal(this.provider, this.cloudName, this.imageId, this.federationUser);
        IQ response = (IQ) PacketSenderHolder.getPacketSender().syncSendPacket(request);
        XmppErrorConditionToExceptionTranslator.handleError(response, this.provider);
        return unmarshalImage(response);
    }

    public static IQ marshal(String provider, String cloudName, String imageId, FederationUser federationUser) {
        IQ iq = new IQ(IQ.Type.get);
        iq.setTo(provider);

        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_GET_IMAGE.toString());

        Element cloudNameElement = queryElement.addElement(IqElement.CLOUD_NAME.toString());
        cloudNameElement.setText(cloudName);

        Element imageIdElement = queryElement.addElement(IqElement.IMAGE_ID.toString());
        imageIdElement.setText(imageId);

        Element userElement = queryElement.addElement(IqElement.FEDERATION_USER.toString());
        userElement.setText(new Gson().toJson(federationUser));

        return iq;
    }

    private Image unmarshalImage(IQ response) throws UnexpectedException {
        Element queryElement = response.getElement().element(IqElement.QUERY.toString());
        String imageStr = queryElement.element(IqElement.IMAGE.toString()).getText();

        String instanceClassName = queryElement.element(IqElement.IMAGE_CLASS_NAME.toString()).getText();

        try {
            return (Image) new Gson().fromJson(imageStr, Class.forName(instanceClassName));
        } catch (Exception e) {
            throw new UnexpectedException(e.getMessage());
        }
    }
}