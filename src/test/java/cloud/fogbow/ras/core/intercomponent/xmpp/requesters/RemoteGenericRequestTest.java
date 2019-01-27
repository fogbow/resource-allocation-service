package cloud.fogbow.ras.core.intercomponent.xmpp.requesters;

import cloud.fogbow.ras.core.intercomponent.xmpp.IQMatcher;
import cloud.fogbow.ras.core.intercomponent.xmpp.IqElement;
import cloud.fogbow.ras.core.intercomponent.xmpp.PacketSenderHolder;
import cloud.fogbow.ras.core.intercomponent.xmpp.RemoteMethod;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequest;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequestResponse;
import org.dom4j.Element;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.jamppa.component.PacketSender;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.xmpp.packet.IQ;

public class RemoteGenericRequestTest {

    private final String provider = "provider";
    private final String cloudName = "cloudName";

    private GenericRequest genericRequest;
    private RemoteGenericRequest remoteGenericRequest;
    private PacketSender packetSender;
    private FederationUserToken federationUserToken;

    @Before
    public void setUp() {
        this.genericRequest = new GenericRequest("GET", "https://www.foo.bar", null, null);
        this.remoteGenericRequest = new RemoteGenericRequest(provider, cloudName, genericRequest, federationUserToken);
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
        IQ expectedIq = RemoteGenericRequest.marshal(this.provider, this.cloudName, this.genericRequest, this.federationUserToken);
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
