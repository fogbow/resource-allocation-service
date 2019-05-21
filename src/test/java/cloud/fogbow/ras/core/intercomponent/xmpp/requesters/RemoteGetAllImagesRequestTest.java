package cloud.fogbow.ras.core.intercomponent.xmpp.requesters;

import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.exceptions.UnavailableProviderException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.api.http.response.ImageSummary;
import cloud.fogbow.ras.core.intercomponent.xmpp.IQMatcher;
import cloud.fogbow.ras.core.intercomponent.xmpp.IqElement;
import cloud.fogbow.ras.core.intercomponent.xmpp.PacketSenderHolder;
import cloud.fogbow.ras.core.intercomponent.xmpp.RemoteMethod;
import com.google.gson.Gson;
import org.dom4j.Element;
import org.jamppa.component.PacketSender;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RemoteGetAllImagesRequestTest {

    private final String PROVIDER = "provider";

    private PacketSender packetSender;
    private RemoteGetAllImagesRequest remoteGetAllImagesRequest;

    private List<ImageSummary> imageSummaryList;
    private SystemUser systemUser;

    private ArgumentCaptor<IQ> argIQ = ArgumentCaptor.forClass(IQ.class);

    @Before
    public void setUp() throws InvalidParameterException {
        this.systemUser = new SystemUser("fake-user-id", "fake-user-name", "fake-token-provider"
        );

        this.remoteGetAllImagesRequest = new RemoteGetAllImagesRequest(PROVIDER, "default", systemUser);
        this.packetSender = Mockito.mock(PacketSender.class);
        PacketSenderHolder.setPacketSender(this.packetSender);

        this.imageSummaryList = new ArrayList<>();
        this.imageSummaryList.add(new ImageSummary("key-1", "value-1"));
        this.imageSummaryList.add(new ImageSummary("key-2", "value-2"));
        this.imageSummaryList.add(new ImageSummary("key-3", "value-3"));
    }

    // test case: checks if IQ attributes is according to both RemoteGetAllImagesRequestTest constructor parameters
    // and remote get all images request rules. In addition, it checks if the image list from a possible response is
    // properly created and returned by the "send" method
    @Test
    public void testSend() throws Exception {
        // set up
        IQ response = getImagesResponse(this.imageSummaryList, this.imageSummaryList.getClass().getName());
        Mockito.doReturn(response).when(this.packetSender).syncSendPacket(Mockito.any(IQ.class));

        // exercise
        this.remoteGetAllImagesRequest.send();

        // verify
        // as IQ does not implement equals we need a matcher
        IQ expectedIQ = RemoteGetAllImagesRequest.marshal(PROVIDER, "default", systemUser);
        IQMatcher matcher = new IQMatcher(expectedIQ);
        Mockito.verify(this.packetSender).syncSendPacket(Mockito.argThat(matcher));
    }

    // test case: checks if "send" is properly forwading UnavailableProviderException thrown by
    // "XmppErrorConditionToExceptionTranslator.handleError" when the IQ response is null
    @Test(expected = UnavailableProviderException.class)
    public void testSendWhenResponseIsNull() throws Exception {
        // set up
        Mockito.doReturn(null).when(this.packetSender).syncSendPacket(Mockito.any(IQ.class));

        // exercise/verify
        this.remoteGetAllImagesRequest.send();
    }

    // test case: checks if "send" is properly forwading UnauthorizedRequestException thrown by
    // "XmppErrorConditionToExceptionTranslator.handleError" when the IQ response status is forbidden
    @Test(expected = UnauthorizedRequestException.class)
    public void testSendWhenResponseReturnsForbidden() throws Exception {
        // set up
        IQ iqResponse = new IQ();
        Mockito.doReturn(iqResponse).when(this.packetSender).syncSendPacket(Mockito.any(IQ.class));
        iqResponse.setError(new PacketError(PacketError.Condition.forbidden));

        // exercise/verify
        this.remoteGetAllImagesRequest.send();
    }

    // test case: checks if "send" is properly forwading UnexpectedException thrown by
    // "getImageFromResponse" when the images getCloudUser class name from the IQ response is undefined (wrong or not found)
    @Test(expected = UnexpectedException.class)
    public void testSendWhenImageClassIsUndefined() throws Exception {
        // set up
        IQ iqResponse = getImagesMapIQResponseWithWrongClass(this.imageSummaryList);
        Mockito.doReturn(iqResponse).when(this.packetSender).syncSendPacket(Mockito.any(IQ.class));

        // exercise/verify
        this.remoteGetAllImagesRequest.send();
    }

    private IQ getImagesResponse(List<ImageSummary> imageSummaryList, String className) {
        IQ iqResponse = new IQ();
        Element queryEl = iqResponse.getElement()
                .addElement(IqElement.QUERY.toString(), RemoteMethod.REMOTE_GET_ALL_IMAGES.toString());
        Element imagesMapElement = queryEl.addElement(IqElement.IMAGE_SUMMARY_LIST.toString());

        Element imagesMapClassNameElement = queryEl
                .addElement(IqElement.IMAGE_SUMMARY_LIST_CLASS_NAME.toString());
        imagesMapClassNameElement.setText(className);

        imagesMapElement.setText(new Gson().toJson(imageSummaryList));
        return iqResponse;
    }

    private IQ getImagesMapIQResponseWithWrongClass(List<ImageSummary> imageSummaryList) {
        return getImagesResponse(imageSummaryList, "wrongClassName");
    }
}
