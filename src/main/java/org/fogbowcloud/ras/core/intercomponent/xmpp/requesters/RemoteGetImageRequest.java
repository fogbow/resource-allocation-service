package org.fogbowcloud.ras.core.intercomponent.xmpp.requesters;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.ras.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.ras.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.ras.core.intercomponent.xmpp.XmppErrorConditionToExceptionTranslator;
import org.fogbowcloud.ras.core.models.images.Image;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.xmpp.packet.IQ;

public class RemoteGetImageRequest implements RemoteRequest<Image> {
    private static final Logger LOGGER = Logger.getLogger(RemoteGetImageRequest.class);

    private String provider;
    private String imageId;
    private FederationUserToken federationUserToken;

    public RemoteGetImageRequest(String provider, String imageId, FederationUserToken federationUserToken) {
        this.provider = provider;
        this.imageId = imageId;
        this.federationUserToken = federationUserToken;
    }

    @Override
    public Image send() throws Exception {
        IQ request = marshal(this.provider, this.imageId, this.federationUserToken);
        IQ response = (IQ) PacketSenderHolder.getPacketSender().syncSendPacket(request);
        XmppErrorConditionToExceptionTranslator.handleError(response, this.provider);
        return unmarshalImage(response);
    }

    public static IQ marshal(String provider, String imageId, FederationUserToken federationUserToken) {
        IQ iq = new IQ(IQ.Type.get);
        iq.setTo(provider);

        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_GET_IMAGE.toString());

        Element memberIdElement = queryElement.addElement(IqElement.MEMBER_ID.toString());
        memberIdElement.setText(provider);

        Element imageIdElement = queryElement.addElement(IqElement.IMAGE_ID.toString());
        imageIdElement.setText(imageId);

        Element userElement = queryElement.addElement(IqElement.FEDERATION_USER.toString());
        userElement.setText(new Gson().toJson(federationUserToken));

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