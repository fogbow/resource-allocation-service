package org.fogbowcloud.ras.core.intercomponent.xmpp.requesters;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.ras.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.ras.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.ras.core.intercomponent.xmpp.XmppErrorConditionToExceptionTranslator;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.xmpp.packet.IQ;

import java.util.HashMap;

public class RemoteGetAllImagesRequest implements RemoteRequest<HashMap<String, String>> {
    private static final Logger LOGGER = Logger.getLogger(RemoteGetAllImagesRequest.class);

    private String provider;
    private FederationUserToken federationUserToken;

    public RemoteGetAllImagesRequest(String provider, FederationUserToken federationUserToken) {
        this.provider = provider;
        this.federationUserToken = federationUserToken;
    }

    @Override
    public HashMap<String, String> send() throws Exception {
        IQ iq = RemoteGetAllImagesRequest.marshal(this.provider, this.federationUserToken);
        IQ response = (IQ) PacketSenderHolder.getPacketSender().syncSendPacket(iq);

        XmppErrorConditionToExceptionTranslator.handleError(response, this.provider);

        return unmarshalImages(response);
    }

    public static IQ marshal(String provider, FederationUserToken federationUserToken) {
        IQ iq = new IQ(IQ.Type.get);
        iq.setTo(provider);

        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_GET_ALL_IMAGES.toString());

        Element memberIdElement = queryElement.addElement(IqElement.MEMBER_ID.toString());
        memberIdElement.setText(provider);

        Element userElement = queryElement.addElement(IqElement.FEDERATION_USER.toString());
        userElement.setText(new Gson().toJson(federationUserToken));

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
