package cloud.fogbow.ras.core.intercomponent.xmpp.handlers;

import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.connectivity.FogbowGenericResponse;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.intercomponent.RemoteFacade;
import cloud.fogbow.ras.core.intercomponent.xmpp.PacketSenderHolder;
import cloud.fogbow.ras.core.intercomponent.xmpp.requesters.RemoteGenericRequest;
import cloud.fogbow.common.util.connectivity.FogbowGenericRequest;
import cloud.fogbow.common.util.connectivity.HttpRequest;
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

@RunWith(PowerMockRunner.class)
@PrepareForTest({RemoteFacade.class, PacketSenderHolder.class})
public class RemoteFogbowGenericRequestHandlerTest {
    private static final String REQUESTING_MEMBER = "requestingmember";

    private static final String IQ_RESULT_FORMAT = "\n<iq type=\"result\" id=\"%s\" from=\"%s\" to=\"%s\">\n" +
            "  <query xmlns=\"remoteGenericRequest\">\n" +
            "    <genericRequestResponse>%s</genericRequestResponse>\n" +
            "    <genericRequestResponseClassName>" +
            FogbowGenericResponse.class.getName() +
            "</genericRequestResponseClassName>\n" +
            "  </query>\n" +
            "</iq>";

    private RemoteGenericRequestHandler remoteGenericRequestHandler;

    private String provider = "fake-provider";
    private String cloudName = "fake-cloud-name";
    private SystemUser systemUser;
    private String fogbowGenericRequest;
    private RemoteFacade remoteFacade;

    @Before
    public void setUp() throws InvalidParameterException {
        this.remoteGenericRequestHandler = new RemoteGenericRequestHandler();
        FogbowGenericRequest fogbowGenericRequest = new HttpRequest(HttpMethod.GET, "https://www.foo.bar", new HashMap<>(), new HashMap<>());
        this.fogbowGenericRequest = GsonHolder.getInstance().toJson(fogbowGenericRequest);

        PacketSender packetSender = Mockito.mock(PacketSender.class);
        PowerMockito.mockStatic(PacketSenderHolder.class);
        BDDMockito.given(PacketSenderHolder.getPacketSender()).willReturn(packetSender);

        this.remoteFacade = Mockito.mock(RemoteFacade.class);
        PowerMockito.mockStatic(RemoteFacade.class);
        BDDMockito.given(RemoteFacade.getInstance()).willReturn(this.remoteFacade);

        this.systemUser = new SystemUser("fake-user-id", "fake-user-name", this.provider
        );
    }

    // test case: when the handle method is called passing a valid IQ object,
    // it must create an OK result IQ and return it.
    @Test
    public void testHandleWithValidIQ() throws Exception {
        // set up
        String fakeContent = "fake-content";
        FogbowGenericResponse fogbowGenericResponse = new FogbowGenericResponse(fakeContent);
        Mockito.doReturn(fogbowGenericResponse).when(this.remoteFacade).genericRequest(
                REQUESTING_MEMBER, this.cloudName, this.fogbowGenericRequest, this.systemUser);

        Mockito.when(this.remoteFacade
                .genericRequest(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any()))
                .thenReturn(fogbowGenericResponse);

        IQ iq = RemoteGenericRequest.marshal(this.provider, this.cloudName, fogbowGenericRequest, this.systemUser);
        iq.setFrom(REQUESTING_MEMBER);

        // exercise
        IQ result = this.remoteGenericRequestHandler.handle(iq);

        // verify
        String iqId = iq.getID();
        String genericRequestResponseAsJson = GsonHolder.getInstance().toJson(fogbowGenericResponse);
        String expected = String.format(IQ_RESULT_FORMAT, iqId, SystemConstants.JID_SERVICE_NAME + "@" + SystemConstants.XMPP_SERVER_NAME_PREFIX + this.provider, REQUESTING_MEMBER, genericRequestResponseAsJson);
        Assert.assertEquals(expected, result.toString());
    }
}
