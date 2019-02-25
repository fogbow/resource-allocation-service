package cloud.fogbow.ras.core.intercomponent.xmpp.requesters;

import cloud.fogbow.common.constants.FogbowConstants;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.exceptions.UnavailableProviderException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.FederationUser;
import cloud.fogbow.ras.core.intercomponent.xmpp.IQMatcher;
import cloud.fogbow.ras.core.intercomponent.xmpp.IqElement;
import cloud.fogbow.ras.core.intercomponent.xmpp.PacketSenderHolder;
import cloud.fogbow.ras.core.intercomponent.xmpp.RemoteMethod;
import cloud.fogbow.ras.api.http.response.Image;
import com.google.gson.Gson;
import org.dom4j.Element;
import org.jamppa.component.PacketSender;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

import java.util.HashMap;
import java.util.Map;

public class RemoteGetImageRequestTest {

    private final String provider = "provider";
    private final String imageId = "imageId";
    private RemoteGetImageRequest remoteGetImageRequest;
    private PacketSender packetSender;
    private FederationUser federationUser;

    @Before
    public void setUp() throws InvalidParameterException {
        Map<String, String> attributes = new HashMap<>();
        attributes.put(FogbowConstants.PROVIDER_ID_KEY, "fake-token-provider");
        attributes.put(FogbowConstants.USER_ID_KEY, "fake-user-id");
        attributes.put(FogbowConstants.USER_NAME_KEY, "fake-user-name");
        attributes.put(FogbowConstants.TOKEN_VALUE_KEY, "fake-federation-token-value");
        this.federationUser = new FederationUser(attributes);

        this.remoteGetImageRequest = new RemoteGetImageRequest(provider, "default", imageId, federationUser);
        this.packetSender = Mockito.mock(PacketSender.class);
        PacketSenderHolder.setPacketSender(this.packetSender);
    }

    // test case: checks if IQ attributes is according to both RemoteGetImageRequest constructor parameters
    // and remote get image request rules. In addition, it checks if the image from a possible response is
    // properly created and returned by the "send" method
    @Test
    public void testSend() throws Exception {
        // set up
        Image image = new Image("image-id", "image-name", 10, 20, 30, "status");
        IQ iqResponse = getImageIQResponse(image);
        Mockito.doReturn(iqResponse).when(this.packetSender).syncSendPacket(Mockito.any(IQ.class));

        // exercise
        this.remoteGetImageRequest.send();

        // verify
        IQ expectedIq = RemoteGetImageRequest.marshal(provider, "default", imageId, federationUser);
        IQMatcher matcher = new IQMatcher(expectedIq);
        Mockito.verify(this.packetSender).syncSendPacket(Mockito.argThat(matcher));
    }

    // test case: checks if "send" is properly forwading UnavailableProviderException thrown by
    // "XmppErrorConditionToExceptionTranslator.handleError" when the IQ response is null
    @Test(expected = UnavailableProviderException.class)
    public void testSendWhenResponseIsNull() throws Exception {
        // set up
        Mockito.doReturn(null).when(this.packetSender).syncSendPacket(Mockito.any(IQ.class));

        // exercise/verify
        this.remoteGetImageRequest.send();
    }

    //test case: checks if "send" is properly forwading UnauthorizedRequestException thrown by
    //"XmppErrorConditionToExceptionTranslator.handleError" when the IQ response status is forbidden
    @Test(expected = UnauthorizedRequestException.class)
    public void testSendWhenResponseReturnsForbidden() throws Exception {
        // set up
        IQ iqResponse = new IQ();
        Mockito.doReturn(iqResponse).when(this.packetSender).syncSendPacket(Mockito.any(IQ.class));
        iqResponse.setError(new PacketError(PacketError.Condition.forbidden));

        // exercise/verify
        this.remoteGetImageRequest.send();
    }

    // test case: checks if "send" is properly forwading UnexpectedException thrown by
    // "getImageFromResponse" when the image class name from the IQ response is undefined (wrong or not found)
    @Test(expected = UnexpectedException.class)
    public void testSendWhenImageClassIsUndefined() throws Exception {
        //set up
        Image image = new Image("image-id", "image-name", 10, 20, 30, "status");
        IQ iqResponse = getImageIQResponseWithWrongClass(image);
        Mockito.doReturn(iqResponse).when(this.packetSender).syncSendPacket(Mockito.any(IQ.class));

        // exercise/verify
        this.remoteGetImageRequest.send();
    }

    private IQ getImageIQResponse(Image image) {
        IQ iqResponse = new IQ();
        Element queryEl = iqResponse.getElement()
                .addElement(IqElement.QUERY.toString(), RemoteMethod.REMOTE_GET_IMAGE.toString());
        Element imageElement = queryEl.addElement(IqElement.IMAGE.toString());
        Element imageClassNameElement = queryEl.addElement(IqElement.IMAGE_CLASS_NAME.toString());
        imageClassNameElement.setText(image.getClass().getName());
        imageElement.setText(new Gson().toJson(image));
        return iqResponse;
    }

    private IQ getImageIQResponseWithWrongClass(Image image) {
        IQ iqResponse = new IQ();
        Element queryEl = iqResponse.getElement()
                .addElement(IqElement.QUERY.toString(), RemoteMethod.REMOTE_GET_IMAGE.toString());
        Element imageElement = queryEl.addElement(IqElement.IMAGE.toString());
        Element imageClassNameElement = queryEl.addElement(IqElement.IMAGE_CLASS_NAME.toString());
        imageClassNameElement.setText("wrong-class-name");
        imageElement.setText(new Gson().toJson(image));
        return iqResponse;
    }
}
