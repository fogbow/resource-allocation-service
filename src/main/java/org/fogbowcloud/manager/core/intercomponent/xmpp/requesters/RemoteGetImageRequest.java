package org.fogbowcloud.manager.core.intercomponent.xmpp.requesters;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.intercomponent.xmpp.XmppErrorConditionToExceptionTranslator;
import org.fogbowcloud.manager.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.manager.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.models.images.Image;
import org.fogbowcloud.manager.core.models.tokens.FederationUserAttributes;
import org.xmpp.packet.IQ;
import com.google.gson.Gson;

public class RemoteGetImageRequest implements RemoteRequest<Image> {

    private static final Logger LOGGER = Logger.getLogger(RemoteGetImageRequest.class);

    private String provider;
    private String imageId;
    private FederationUserAttributes federationUserAttributes;

    public RemoteGetImageRequest(String provider, String imageId, FederationUserAttributes federationUserAttributes) {
        this.provider = provider;
        this.imageId = imageId;
        this.federationUserAttributes = federationUserAttributes;
    }

    @Override
    public Image send() throws Exception {
        IQ iq = createIq();
        IQ response = (IQ) PacketSenderHolder.getPacketSender().syncSendPacket(iq);

        XmppErrorConditionToExceptionTranslator.handleError(response, this.provider);
        Image image = getImageFromResponse(response);
        return image;
    }

    private IQ createIq() {
        IQ iq = new IQ(IQ.Type.get);
        iq.setTo(this.provider);

        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_GET_IMAGE.toString());

        Element memberIdElement = queryElement.addElement(IqElement.MEMBER_ID.toString());
        memberIdElement.setText(this.provider);

        Element imageIdElement = queryElement.addElement(IqElement.IMAGE_ID.toString());
        imageIdElement.setText(this.imageId);

        Element userElement = queryElement.addElement(IqElement.FEDERATION_USER.toString());
        userElement.setText(new Gson().toJson(this.federationUserAttributes));

        return iq;
    }

    private Image getImageFromResponse(IQ response) throws UnexpectedException {
        Element queryElement = response.getElement().element(IqElement.QUERY.toString());
        String imageStr = queryElement.element(IqElement.IMAGE.toString()).getText();

        String instanceClassName = queryElement.element(IqElement.IMAGE_CLASS_NAME.toString()).getText();

        Image image = null;
        try {
            image = (Image) new Gson().fromJson(imageStr, Class.forName(instanceClassName));
        } catch (Exception e) {
            throw new UnexpectedException(e.getMessage());
        }

        return image;
    }
}