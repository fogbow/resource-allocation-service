package cloud.fogbow.ras.core.intercomponent.xmpp.requesters;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.core.intercomponent.xmpp.IqElement;
import cloud.fogbow.ras.core.intercomponent.xmpp.PacketSenderHolder;
import cloud.fogbow.ras.core.intercomponent.xmpp.RemoteMethod;
import cloud.fogbow.ras.core.intercomponent.xmpp.XmppErrorConditionToExceptionTranslator;
import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.xmpp.packet.IQ;

import java.util.HashMap;

public class RemoteGetAllImagesRequest implements RemoteRequest<HashMap<String, String>> {
    private static final Logger LOGGER = Logger.getLogger(RemoteGetAllImagesRequest.class);

    private String provider;
    private String cloudName;
    private SystemUser systemUser;

    public RemoteGetAllImagesRequest(String provider, String cloudName, SystemUser systemUser) {
        this.provider = provider;
        this.cloudName = cloudName;
        this.systemUser = systemUser;
    }

    @Override
    public HashMap<String, String> send() throws Exception {
        IQ iq = RemoteGetAllImagesRequest.marshal(this.provider, this.cloudName, this.systemUser);
        IQ response = (IQ) PacketSenderHolder.getPacketSender().syncSendPacket(iq);

        XmppErrorConditionToExceptionTranslator.handleError(response, this.provider);

        return unmarshalImages(response);
    }

    public static IQ marshal(String provider, String cloudName, SystemUser systemUser) {
        IQ iq = new IQ(IQ.Type.get);
        iq.setTo(provider);

        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_GET_ALL_IMAGES.toString());

        Element cloudNameElement = queryElement.addElement(IqElement.CLOUD_NAME.toString());
        cloudNameElement.setText(cloudName);

        Element userElement = queryElement.addElement(IqElement.FEDERATION_USER.toString());
        userElement.setText(new Gson().toJson(systemUser));

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
