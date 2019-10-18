package cloud.fogbow.ras.core.intercomponent.xmpp.handlers;

import cloud.fogbow.common.constants.Messages;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.api.http.response.ImageSummary;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.intercomponent.RemoteFacade;
import cloud.fogbow.ras.core.intercomponent.xmpp.PacketSenderHolder;
import cloud.fogbow.ras.core.intercomponent.xmpp.requesters.RemoteGetAllImagesRequest;
import org.jamppa.component.PacketSender;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.xmpp.packet.IQ;

import java.util.ArrayList;
import java.util.List;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RemoteFacade.class, PacketSenderHolder.class})
public class RemoteGetAllImagesRequestHandlerTest {

    private static final String REQUESTING_MEMBER = "requester";

    private static final String IQ_RESULT_FORMAT = "\n<iq type=\"result\" id=\"%s\" from=\"%s\" to=\"%s\">\n" +
            "  <query xmlns=\"remoteGetAllImages\">\n" +
            "    <imageSummaryList>[{\"id\":\"image-id1\",\"name\":\"%s\"},{\"id\":\"image-id2\",\"name\":\"%s\"}]</imageSummaryList>\n" +
            "    <imageSummaryListClassName>java.util.ArrayList</imageSummaryListClassName>\n" +
            "  </query>\n" +
            "</iq>";

    private static final String IQ_ERROR_RESULT_FORMAT =
            "\n<iq type=\"error\" id=\"%s\" from=\"%s\" to=\"%s\">\n" +
                    "  <error code=\"500\" type=\"wait\">\n" +
                    "    <undefined-condition xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\"/>\n" +
                    "    <text xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\">" + Messages.Exception.FOGBOW + "</text>\n" +
                    "  </error>\n" +
                    "</iq>";

    private String IMAGE_NAME = "image-name";

    private RemoteGetAllImagesRequestHandler remoteGetAllImagesRequestHandler;

    private String provider;
    private SystemUser systemUser;
    private RemoteFacade remoteFacade;

    @Before
    public void setUp() {
        this.remoteGetAllImagesRequestHandler = new RemoteGetAllImagesRequestHandler();

        PacketSender packetSender = Mockito.mock(PacketSender.class);
        PowerMockito.mockStatic(PacketSenderHolder.class);
        BDDMockito.given(PacketSenderHolder.getPacketSender()).willReturn(packetSender);

        this.remoteFacade = Mockito.mock(RemoteFacade.class);
        PowerMockito.mockStatic(RemoteFacade.class);
        BDDMockito.given(RemoteFacade.getInstance()).willReturn(this.remoteFacade);

        this.provider = "provider";
        this.systemUser = new SystemUser("fake-user-id", "fake-user-name", this.provider
        );
    }

    // test case: when the handle method is called passing a valid IQ object,
    // it must create an OK result IQ and return it.
    @Test
    public void testHandleWithValidIQ() throws Exception {
        // set up
        List<ImageSummary> images = new ArrayList<>();
        images.add(new ImageSummary("image-id1", IMAGE_NAME.concat("1")));
        images.add(new ImageSummary("image-id2", IMAGE_NAME.concat("2")));

        Mockito.doReturn(images).when(this.remoteFacade).getAllImages(Mockito.anyString(), Mockito.anyString(),
                Mockito.any(SystemUser.class));

        IQ iq = RemoteGetAllImagesRequest.marshal(this.provider, "default", this.systemUser);
        iq.setFrom(REQUESTING_MEMBER);

        // exercise
        IQ result = this.remoteGetAllImagesRequestHandler.handle(iq);

        // verify
        String iqId = iq.getID();
        String expected = String.format(IQ_RESULT_FORMAT, iqId, SystemConstants.JID_SERVICE_NAME + "@" + SystemConstants.XMPP_SERVER_NAME_PREFIX + this.provider, REQUESTING_MEMBER,
                IMAGE_NAME.concat("1"), IMAGE_NAME.concat("2"));
        Assert.assertEquals(expected, result.toString());
    }

    // test case: When an exception occurs while getting the images, the method handle should
    // return a response error.
    @Test
    public void testHandleWhenThrowsException() throws Exception {
        // set up
        Mockito.doThrow(new FogbowException()).when(this.remoteFacade).getAllImages(Mockito.anyString(),
                Mockito.anyString(), Mockito.any(SystemUser.class));

        IQ iq = RemoteGetAllImagesRequest.marshal(this.provider, "default", this.systemUser);
        iq.setFrom(REQUESTING_MEMBER);

        // exercise
        IQ result = this.remoteGetAllImagesRequestHandler.handle(iq);

        // verify
        Mockito.verify(this.remoteFacade, Mockito.times(1)).
                getAllImages(Mockito.anyString(), Mockito.anyString(), Mockito.any(SystemUser.class));

        String iqId = iq.getID();
        String expected = String.format(IQ_ERROR_RESULT_FORMAT, iqId, SystemConstants.JID_SERVICE_NAME + "@" + SystemConstants.XMPP_SERVER_NAME_PREFIX + this.provider, REQUESTING_MEMBER);
        Assert.assertEquals(expected, result.toString());
    }
}
