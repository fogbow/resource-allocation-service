package cloud.fogbow.ras.core.intercomponent.xmpp.requesters;

import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.connectivity.GenericRequestResponse;
import cloud.fogbow.ras.core.intercomponent.xmpp.IQMatcher;
import cloud.fogbow.ras.core.intercomponent.xmpp.IqElement;
import cloud.fogbow.ras.core.intercomponent.xmpp.PacketSenderHolder;
import cloud.fogbow.ras.core.intercomponent.xmpp.RemoteMethod;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.FogbowGenericRequest;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.HttpFogbowGenericRequest;
import org.dom4j.Element;
import org.jamppa.component.PacketSender;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.xmpp.packet.IQ;

import java.util.HashMap;

public class RemoteFogbowGenericRequestTest {

    private final String provider = "provider";
    private final String cloudName = "cloudName";

    private FogbowGenericRequest fogbowGenericRequest;
    private RemoteGenericRequest remoteGenericRequest;
    private PacketSender packetSender;
    private SystemUser systemUser;

    @Before
    public void setUp() {
        this.fogbowGenericRequest = new HttpFogbowGenericRequest(HttpMethod.GET, "https://www.foo.bar", new HashMap<>(), new HashMap<>());
        this.remoteGenericRequest = new RemoteGenericRequest(provider, cloudName, fogbowGenericRequest, systemUser);
        this.packetSender = Mockito.mock(PacketSender.class);
        PacketSenderHolder.setPacketSender(this.packetSender);
    }

    // test case: send a generic request and assure that the marshalling occurs properly
    @Test
    public void testSend() throws Exception {
        // set up
        String expectedResponseContent = "fakeContent";
        GenericRequestResponse expectedGenericRequestResponse = new GenericRequestResponse(expectedResponseContent);
        IQ expectedResponse = createIq(expectedGenericRequestResponse);

        Mockito.doReturn(expectedResponse).when(this.packetSender).syncSendPacket(Mockito.any(IQ.class));

        // exercise
        this.remoteGenericRequest.send();

        // verify
        IQ expectedIq = RemoteGenericRequest.marshal(this.provider, this.cloudName, this.fogbowGenericRequest, this.systemUser);
        IQMatcher matcher = new IQMatcher(expectedIq);
        Mockito.verify(this.packetSender).syncSendPacket(Mockito.argThat(matcher));
    }

    private IQ createIq(GenericRequestResponse genericRequestResponse) {
        IQ response = new IQ();
        Element queryEl = response.getElement().addElement(IqElement.QUERY.toString(), RemoteMethod.REMOTE_GENERIC_REQUEST.toString());
        Element genericRequestElement = queryEl.addElement(IqElement.GENERIC_REQUEST_RESPONSE.toString());
        Element genericRequestElementClassname = queryEl.addElement(IqElement.GENERIC_REQUEST_RESPONSE_CLASS_NAME.toString());

        genericRequestElement.setText(GsonHolder.getInstance().toJson(genericRequestResponse));
        genericRequestElementClassname.setText(genericRequestResponse.getClass().getName());
        return response;
    }

}
