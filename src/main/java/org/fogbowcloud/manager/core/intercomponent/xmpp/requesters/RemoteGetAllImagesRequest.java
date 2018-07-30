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
        IQ iq = createIq();
        IQ response = (IQ) PacketSenderHolder.getPacketSender().syncSendPacket(iq);

        XmppErrorConditionToExceptionTranslator.handleError(response, this.provider);
        HashMap<String, String> imagesMap = getImageFromResponse(response);
        return imagesMap;
    }

    private IQ createIq() {
        IQ iq = new IQ(IQ.Type.get);
        iq.setTo(this.provider);

        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_GET_ALL_IMAGES.toString());

        Element memberIdElement = queryElement.addElement(IqElement.MEMBER_ID.toString());
        memberIdElement.setText(this.provider);

        Element userElement = queryElement.addElement(IqElement.FEDERATION_USER.toString());
        userElement.setText(new Gson().toJson(this.federationUser));

        return iq;
    }

    private HashMap<String, String> getImageFromResponse(IQ response) throws UnexpectedException {
        Element queryElement = response.getElement().element(IqElement.QUERY.toString());
        String hashMapStr = queryElement.element(IqElement.IMAGES_MAP.toString()).getText();

        String instanceClassName = queryElement.element(IqElement.IMAGES_MAP_CLASS_NAME.toString()).getText();

        HashMap<String, String> imagesMap = null;
        try {
            imagesMap = (HashMap<String, String>) new Gson().fromJson(hashMapStr, Class.forName(instanceClassName));
        } catch (Exception e) {
            throw new UnexpectedException(e.getMessage());
        }

        return imagesMap;
    }
}