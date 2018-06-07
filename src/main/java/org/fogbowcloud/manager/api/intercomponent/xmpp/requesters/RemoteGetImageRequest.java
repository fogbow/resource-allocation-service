package org.fogbowcloud.manager.api.intercomponent.xmpp.requesters;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.manager.api.intercomponent.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.api.intercomponent.exceptions.UnexpectedException;
import org.fogbowcloud.manager.api.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.api.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.manager.api.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.models.images.Image;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.xmpp.packet.IQ;

import com.google.gson.Gson;

public class RemoteGetImageRequest implements RemoteRequest<Image> {

    private static final Logger LOGGER = Logger.getLogger(RemoteGetImageRequest.class);

    private String federationMemberId;
    private String imageId;
    private FederationUser federationUser;

    public RemoteGetImageRequest(String federationMemberId, String imageId, FederationUser federationUser) {
        this.federationMemberId = federationMemberId;
        this.imageId = imageId;
        this.federationUser = federationUser;
    }

    @Override
    public Image send() throws RemoteRequestException {
        IQ iq = createIq();
        IQ response = (IQ) PacketSenderHolder.getPacketSender().syncSendPacket(iq);

        if (response == null) {
            String message = "Unable to retrieve image " + this.imageId;
            throw new UnexpectedException(message);
        } else if (response.getError() != null) {
            LOGGER.error(response.getError().toString());
            // TODO: Add errors treatment.
            throw new UnexpectedException(response.getError().toString());
        }
        Image image = getImageFromResponse(response);
        return image;
    }

    private IQ createIq() {
        IQ iq = new IQ(IQ.Type.get);
        iq.setTo(this.federationMemberId);

        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_GET_IMAGE.toString());

        Element memberIdElement = iq.getElement().addElement(IqElement.MEMBER_ID.toString());
        memberIdElement.setText(new Gson().toJson(this.federationMemberId));

        Element imageIdElement = queryElement.addElement(IqElement.IMAGE_ID.toString());
        imageIdElement.setText(this.imageId);

        Element userElement = iq.getElement().addElement(IqElement.FEDERATION_USER.toString());
        userElement.setText(new Gson().toJson(this.federationUser));

        return iq;
    }

    private Image getImageFromResponse(IQ response) throws RemoteRequestException {
        Element queryElement = response.getElement().element(IqElement.QUERY.toString());
        String imageStr = queryElement.element(IqElement.IMAGE.toString()).getText();

        String instanceClassName = queryElement.element(IqElement.IMAGE_CLASS_NAME.toString()).getText();

        Image image = null;
        try {
            image = (Image) new Gson().fromJson(imageStr, Class.forName(instanceClassName));
        } catch (Exception e) {
            throw new RemoteRequestException(e.getMessage());
        }

        return image;
    }
}