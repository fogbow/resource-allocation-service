package org.fogbowcloud.manager.core.intercomponent.xmpp.requesters;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.intercomponent.xmpp.XmppErrorConditionToExceptionTranslator;
import org.fogbowcloud.manager.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.manager.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.xmpp.packet.IQ;
import com.google.gson.Gson;
import java.util.HashMap;

public class RemoteGetAllImagesRequest implements RemoteRequest<HashMap<String, String>> {

    private static final Logger LOGGER = Logger.getLogger(RemoteGetAllImagesRequest.class);

    private String provider;
    private FederationUser federationUser;

    public RemoteGetAllImagesRequest(String provider, FederationUser federationUser) {
        this.provider = provider;
        this.federationUser = federationUser;
    }

    @Override
    public HashMap<String, String> send() throws Exception {
        IQ iq = RemoteGetAllImagesRequest.marshal(this.provider, this.federationUser);
        IQ response = (IQ) PacketSenderHolder.getPacketSender().syncSendPacket(iq);

        XmppErrorConditionToExceptionTranslator.handleError(response, this.provider);

        return unmarshalImages(response);
    }

    public static IQ marshal(String provider, FederationUser federationUser) {
        LOGGER.debug("Marshalling provider <" + provider + "> and federationUser <" + federationUser + ">");

        IQ iq = new IQ(IQ.Type.get);
        iq.setTo(provider);

        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_GET_ALL_IMAGES.toString());

        Element memberIdElement = queryElement.addElement(IqElement.MEMBER_ID.toString());
        memberIdElement.setText(provider);

        Element userElement = queryElement.addElement(IqElement.FEDERATION_USER.toString());
        userElement.setText(new Gson().toJson(federationUser));

        return iq;
    }

    private HashMap<String, String> unmarshalImages(IQ response) throws UnexpectedException {
        Element queryElement = response.getElement().element(IqElement.QUERY.toString());
        String hashMapStr = queryElement.element(IqElement.IMAGES_MAP.toString()).getText();

        String instanceClassName = queryElement.element(IqElement.IMAGES_MAP_CLASS_NAME.toString()).getText();

        HashMap<String, String> imagesMap;

        try {
            imagesMap = (HashMap<String, String>) new Gson().fromJson(hashMapStr, Class.forName(instanceClassName));
        } catch (Exception e) {
            throw new UnexpectedException(e.getMessage());
        }

        return imagesMap;
    }

}
