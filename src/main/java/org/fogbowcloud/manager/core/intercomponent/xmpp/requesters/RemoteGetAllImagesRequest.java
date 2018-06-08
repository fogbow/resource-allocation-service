package org.fogbowcloud.manager.core.intercomponent.xmpp.requesters;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.manager.core.intercomponent.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.core.intercomponent.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.manager.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.xmpp.packet.IQ;

import com.google.gson.Gson;

import java.util.HashMap;

public class RemoteGetAllImagesRequest implements RemoteRequest<HashMap<String, String>> {

    private static final Logger LOGGER = Logger.getLogger(RemoteGetAllImagesRequest.class);

    private String federationMemberId;
    private FederationUser federationUser;

    public RemoteGetAllImagesRequest(String federationMemberId, FederationUser federationUser) {
        this.federationMemberId = federationMemberId;
        this.federationUser = federationUser;
    }

    @Override
    public HashMap<String, String> send() throws RemoteRequestException {
        IQ iq = createIq();
        IQ response = (IQ) PacketSenderHolder.getPacketSender().syncSendPacket(iq);

        if (response == null) {
            String message = "Unable to retrieve images";
            throw new UnexpectedException(message);
        } else if (response.getError() != null) {
            LOGGER.error(response.getError().toString());
            // TODO: Add errors treatment.
            throw new UnexpectedException(response.getError().toString());
        }
        HashMap<String, String> imagesMap = getImageFromResponse(response);
        return imagesMap;
    }

    private IQ createIq() {
        IQ iq = new IQ(IQ.Type.get);
        iq.setTo(this.federationMemberId);

        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_GET_ALL_IMAGES.toString());

        Element memberIdElement = iq.getElement().addElement(IqElement.MEMBER_ID.toString());
        memberIdElement.setText(new Gson().toJson(this.federationMemberId));

        Element userElement = iq.getElement().addElement(IqElement.FEDERATION_USER.toString());
        userElement.setText(new Gson().toJson(this.federationUser));

        return iq;
    }

    private HashMap<String, String> getImageFromResponse(IQ response) throws RemoteRequestException {
        Element queryElement = response.getElement().element(IqElement.QUERY.toString());
        String hashMapStr = queryElement.element(IqElement.IMAGES_MAP.toString()).getText();

        String instanceClassName = queryElement.element(IqElement.IMAGES_MAP_CLASS_NAME.toString()).getText();

        HashMap<String, String> imagesMap = null;
        try {
            imagesMap = (HashMap<String, String>) new Gson().fromJson(hashMapStr, Class.forName(instanceClassName));
        } catch (Exception e) {
            throw new RemoteRequestException(e.getMessage());
        }

        return imagesMap;
    }
}