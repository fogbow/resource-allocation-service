package org.fogbowcloud.ras.core.intercomponent.xmpp.requesters;

import com.google.gson.Gson;
import org.dom4j.Element;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.UnauthorizedRequestException;
import org.fogbowcloud.ras.core.exceptions.UnavailableProviderException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.intercomponent.xmpp.IQMatcher;
import org.fogbowcloud.ras.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.ras.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.ras.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.ras.core.models.images.Image;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.jamppa.component.PacketSender;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;


public class RemoteGetImageRequestTest {

    private final String provider = "provider";
    private final String imageId = "imageId";
    private RemoteGetImageRequest remoteGetImageRequest;
    private PacketSender packetSender;
    private FederationUserToken federationUserToken;

    @Before
    public void setUp() throws InvalidParameterException {
        this.federationUserToken = new FederationUserToken("fake-token-provider",
                "fake-federation-token-value", "fake-user-id", "fake-user-name");

        this.remoteGetImageRequest = new RemoteGetImageRequest(provider, imageId, federationUserToken);
        this.packetSender = Mockito.mock(PacketSender.class);
        PacketSenderHolder.init(packetSender);
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
        IQ expectedIq = RemoteGetImageRequest.marshal(provider, imageId, federationUserToken);
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
