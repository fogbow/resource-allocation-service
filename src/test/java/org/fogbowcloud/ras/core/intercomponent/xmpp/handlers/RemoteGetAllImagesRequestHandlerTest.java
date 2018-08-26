package org.fogbowcloud.ras.core.intercomponent.xmpp.handlers;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.intercomponent.RemoteFacade;
import org.fogbowcloud.ras.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.ras.core.intercomponent.xmpp.requesters.RemoteGetAllImagesRequest;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
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

import java.util.HashMap;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RemoteFacade.class, PacketSenderHolder.class})
public class RemoteGetAllImagesRequestHandlerTest {

    private static final String IQ_RESULT_FORMAT = "\n<iq type=\"result\" id=\"%s\" from=\"%s\">\n" +
            "  <query xmlns=\"remoteGetAllImages\">\n" +
            "    <imagesMap>{\"image-id1\":\"%s\",\"image-id2\":\"%s\"}</imagesMap>\n" +
            "    <imagesMapClassName>java.util.HashMap</imagesMapClassName>\n" +
            "  </query>\n" +
            "</iq>";

    private static final String IQ_ERROR_RESULT_FORMAT =
            "\n<iq type=\"error\" id=\"%s\" from=\"%s\">\n" +
                    "  <error code=\"500\" type=\"wait\">\n" +
                    "    <undefined-condition xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\"/>\n" +
                    "    <text xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\">Fogbow RAS exception</text>\n" +
                    "  </error>\n" +
                    "</iq>";

    private String IMAGE_NAME = "image-name";

    private RemoteGetAllImagesRequestHandler remoteGetAllImagesRequestHandler;

    private String provider;
    private FederationUserToken federationUserToken;
    private RemoteFacade remoteFacade;

    @Before
    public void setUp() throws InvalidParameterException {
        this.remoteGetAllImagesRequestHandler = new RemoteGetAllImagesRequestHandler();

        PacketSender packetSender = Mockito.mock(PacketSender.class);
        PowerMockito.mockStatic(PacketSenderHolder.class);
        BDDMockito.given(PacketSenderHolder.getPacketSender()).willReturn(packetSender);

        this.remoteFacade = Mockito.mock(RemoteFacade.class);
        PowerMockito.mockStatic(RemoteFacade.class);
        BDDMockito.given(RemoteFacade.getInstance()).willReturn(this.remoteFacade);

        this.provider = "member";
        this.federationUserToken = new FederationUserToken(this.provider,
                "fake-federation-token-value", "fake-user-id", "fake-user-name");

        this.federationUserToken = federationUserToken;
    }

    // test case: when the handle method is called passing a valid IQ object,
    // it must create an OK result IQ and return it.
    @Test
    public void testHandleWithValidIQ() throws Exception {
        // set up
        Map<String, String> images = new HashMap<>();
        images.put("image-id1", IMAGE_NAME.concat("1"));
        images.put("image-id2", IMAGE_NAME.concat("2"));

        Mockito.doReturn(images).when(this.remoteFacade).getAllImages(Mockito.anyString(), Mockito.any(FederationUserToken.class));

        IQ iq = RemoteGetAllImagesRequest.marshal(this.provider, this.federationUserToken);

        // exercise
        IQ result = this.remoteGetAllImagesRequestHandler.handle(iq);

        // verify
        String iqId = iq.getID();
        String expected = String.format(IQ_RESULT_FORMAT, iqId, this.provider,
                IMAGE_NAME.concat("1"), IMAGE_NAME.concat("2"));
        Assert.assertEquals(expected, result.toString());
    }

    // test case: When an exception occurs while getting the images, the method handle should
    // return a response error.
    @Test
    public void testHandleWhenThrowsException() throws Exception {
        // set up
        Mockito.doThrow(new FogbowRasException()).when(this.remoteFacade).getAllImages(
                Mockito.anyString(), Mockito.any(FederationUserToken.class));

        IQ iq = RemoteGetAllImagesRequest.marshal(this.provider, this.federationUserToken);

        // exercise
        IQ result = this.remoteGetAllImagesRequestHandler.handle(iq);

        // verify
        Mockito.verify(this.remoteFacade, Mockito.times(1)).getAllImages(
                Mockito.anyString(), Mockito.any(FederationUserToken.class));

        String iqId = iq.getID();
        String expected = String.format(IQ_ERROR_RESULT_FORMAT, iqId, this.provider);
        Assert.assertEquals(expected, result.toString());
    }
}
