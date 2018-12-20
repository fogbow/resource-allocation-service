package org.fogbowcloud.ras.core.intercomponent.xmpp.handlers;

import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.intercomponent.RemoteFacade;
import org.fogbowcloud.ras.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.ras.core.intercomponent.xmpp.requesters.RemoteGenericRequest;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.plugins.interoperability.genericrequest.GenericRequest;
import org.fogbowcloud.ras.core.plugins.interoperability.genericrequest.GenericRequestResponse;
import org.fogbowcloud.ras.util.GsonHolder;
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

@RunWith(PowerMockRunner.class)
@PrepareForTest({RemoteFacade.class, PacketSenderHolder.class})
public class RemoteGenericRequestHandlerTest {
    private static final String REQUESTING_MEMBER = "requestingmember";

    private static final String IQ_RESULT_FORMAT = "\n<iq type=\"result\" id=\"%s\" from=\"%s\" to=\"%s\">\n" +
            "  <query xmlns=\"remoteGenericRequest\">\n" +
            "    <genericRequestResponse>%s</genericRequestResponse>\n" +
            "    <genericRequestResponseClassName>org.fogbowcloud.ras.core.plugins.interoperability.genericrequest.GenericRequestResponse</genericRequestResponseClassName>\n" +
            "  </query>\n" +
            "</iq>";

    private String IMAGE_NAME = "image-name";

    private RemoteGenericRequestHandler remoteGenericRequestHandler;

    private String provider = "fake-provider";
    private String cloudName = "fake-cloud-name";
    private FederationUserToken federationUserToken;
    private GenericRequest genericRequest =  new GenericRequest("GET", "https://www.foo.bar", null, null);
    private RemoteFacade remoteFacade;

    @Before
    public void setUp() throws InvalidParameterException {
        this.remoteGenericRequestHandler = new RemoteGenericRequestHandler();

        PacketSender packetSender = Mockito.mock(PacketSender.class);
        PowerMockito.mockStatic(PacketSenderHolder.class);
        BDDMockito.given(PacketSenderHolder.getPacketSender()).willReturn(packetSender);

        this.remoteFacade = Mockito.mock(RemoteFacade.class);
        PowerMockito.mockStatic(RemoteFacade.class);
        BDDMockito.given(RemoteFacade.getInstance()).willReturn(this.remoteFacade);

        this.federationUserToken = new FederationUserToken(this.provider,
                "fake-federation-token-value", "fake-user-id", "fake-user-name");
    }

    // test case: when the handle method is called passing a valid IQ object,
    // it must create an OK result IQ and return it.
    @Test
    public void testHandleWithValidIQ() throws Exception {
        // set up
        String fakeContent = "fake-content";
        GenericRequestResponse genericRequestResponse = new GenericRequestResponse(fakeContent);
        Mockito.doReturn(genericRequestResponse).when(this.remoteFacade).genericRequest(
                REQUESTING_MEMBER, this.cloudName, this.provider, this.genericRequest, this.federationUserToken);

        Mockito.when(this.remoteFacade
                .genericRequest(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any()))
                .thenReturn(genericRequestResponse);

        IQ iq = RemoteGenericRequest.marshal(this.provider, this.cloudName, genericRequest, this.federationUserToken);
        iq.setFrom(REQUESTING_MEMBER);

        // exercise
        IQ result = this.remoteGenericRequestHandler.handle(iq);

        // verify
        String iqId = iq.getID();
        String genericRequestResponseAsJson = GsonHolder.getInstance().toJson(genericRequestResponse);
        String expected = String.format(IQ_RESULT_FORMAT, iqId, this.provider, REQUESTING_MEMBER, genericRequestResponseAsJson);
        Assert.assertEquals(expected, result.toString());
    }
}
