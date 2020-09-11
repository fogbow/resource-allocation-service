package cloud.fogbow.ras.core.intercomponent.xmpp.requesters;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.exceptions.UnavailableProviderException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.core.intercomponent.xmpp.IQMatcher;
import cloud.fogbow.ras.core.intercomponent.xmpp.IqElement;
import cloud.fogbow.ras.core.intercomponent.xmpp.PacketSenderHolder;
import cloud.fogbow.ras.core.intercomponent.xmpp.RemoteMethod;
import com.google.gson.Gson;
import org.dom4j.Element;
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
    private SystemUser systemUser;

    @Before
    public void setUp() {
        this.systemUser = new SystemUser("fake-user-id", "fake-user-name", "fake-token-provider"
        );

        this.remoteGetImageRequest = new RemoteGetImageRequest(provider, "default", imageId, systemUser);
        this.packetSender = Mockito.mock(PacketSender.class);
        PacketSenderHolder.setPacketSender(this.packetSender);
    }

    // test case: checks if IQ attributes is according to both RemoteGetImageRequest constructor parameters
    // and remote get image request rules. In addition, it checks if the image from a possible response is
    // properly created and returned by the "send" method
    @Test
    public void testSend() throws Exception {
        // set up
        ImageInstance imageInstance = new ImageInstance("imageInstance-id", "imageInstance-name", 10, 20, 30, "status");
        IQ iqResponse = getImageIQResponse(imageInstance);
        Mockito.doReturn(iqResponse).when(this.packetSender).syncSendPacket(Mockito.any(IQ.class));

        // exercise
        this.remoteGetImageRequest.send();

        // verify
        IQ expectedIq = RemoteGetImageRequest.marshal(provider, "default", imageId, systemUser);
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

    // test case: checks if "send" is properly forwading InternalServerErrorException thrown by
    // "getImageFromResponse" when the image class name from the IQ response is undefined (wrong or not found)
    @Test(expected = InternalServerErrorException.class)
    public void testSendWhenImageClassIsUndefined() throws Exception {
        //set up
        ImageInstance imageInstance = new ImageInstance("imageInstance-id", "imageInstance-name", 10, 20, 30, "status");
        IQ iqResponse = getImageIQResponseWithWrongClass(imageInstance);
        Mockito.doReturn(iqResponse).when(this.packetSender).syncSendPacket(Mockito.any(IQ.class));

        // exercise/verify
        this.remoteGetImageRequest.send();
    }

    private IQ getImageIQResponse(ImageInstance imageInstance) {
        IQ iqResponse = new IQ();
        Element queryEl = iqResponse.getElement()
                .addElement(IqElement.QUERY.toString(), RemoteMethod.REMOTE_GET_IMAGE.toString());
        Element imageElement = queryEl.addElement(IqElement.IMAGE.toString());
        Element imageClassNameElement = queryEl.addElement(IqElement.IMAGE_CLASS_NAME.toString());
        imageClassNameElement.setText(imageInstance.getClass().getName());
        imageElement.setText(new Gson().toJson(imageInstance));
        return iqResponse;
    }

    private IQ getImageIQResponseWithWrongClass(ImageInstance imageInstance) {
        IQ iqResponse = new IQ();
        Element queryEl = iqResponse.getElement()
                .addElement(IqElement.QUERY.toString(), RemoteMethod.REMOTE_GET_IMAGE.toString());
        Element imageElement = queryEl.addElement(IqElement.IMAGE.toString());
        Element imageClassNameElement = queryEl.addElement(IqElement.IMAGE_CLASS_NAME.toString());
        imageClassNameElement.setText("wrong-class-name");
        imageElement.setText(new Gson().toJson(imageInstance));
        return iqResponse;
    }
}
