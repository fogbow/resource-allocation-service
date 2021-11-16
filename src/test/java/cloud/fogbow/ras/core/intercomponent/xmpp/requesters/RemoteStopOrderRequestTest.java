package cloud.fogbow.ras.core.intercomponent.xmpp.requesters;

import org.jamppa.component.PacketSender;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.exceptions.UnavailableProviderException;
import cloud.fogbow.ras.core.intercomponent.xmpp.IQMatcher;
import cloud.fogbow.ras.core.intercomponent.xmpp.PacketSenderHolder;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.Order;

public class RemoteStopOrderRequestTest {

    private final String providingMember = "providing-member";

    private RemoteStopOrderRequest remoteStopOrderRequest;
    private Order order;
    private PacketSender packetSender;
    private IQ iqResponse;

    @Before
    public void setUp() throws InvalidParameterException {
        this.order = new ComputeOrder(null, "requesting-member", this.providingMember, "default", "hostName", 10, 20, 30,
                "imageid", null,
                "publicKey", null);
        this.remoteStopOrderRequest = new RemoteStopOrderRequest(this.order);
        this.packetSender = Mockito.mock(PacketSender.class);
        PacketSenderHolder.setPacketSender(this.packetSender);
        this.iqResponse = new IQ();
    }

    //test case: check if IQ attributes is according to both Order parameters and remote create order request rules
    @Test
    public void testSend() throws Exception {
        // set up
        IQ expectedIQ = RemoteStopOrderRequest.marshal(this.order);

        Mockito.doReturn(this.iqResponse).when(this.packetSender)
                .syncSendPacket(Mockito.any(IQ.class));

        // exercise
        this.remoteStopOrderRequest.send();

        // verify
        // as IQ does not implement equals we need a matcher
        IQMatcher matcher = new IQMatcher(expectedIQ);
        Mockito.verify(this.packetSender).syncSendPacket(Mockito.argThat(matcher));
    }

    //test case: Check if "send" is properly forwarding UnavailableProviderException thrown by
    //"XmppErrorConditionToExceptionTranslator.handleError" when the IQ response is null
    @Test(expected = UnavailableProviderException.class)
    public void testSendWhenResponseIsNull() throws Exception {
        // set up
        Mockito.doReturn(null).when(this.packetSender).syncSendPacket(Mockito.any(IQ.class));

        // exercise/verify
        this.remoteStopOrderRequest.send();
    }

    //test case: Check if "send" is properly forwarding UnauthorizedRequestException thrown by
    //"XmppErrorConditionToExceptionTranslator.handleError" when the IQ response status is forbidden
    @Test(expected = UnauthorizedRequestException.class)
    public void testSendWhenResponseReturnsForbidden() throws Exception {
        // set up
        Mockito.doReturn(this.iqResponse).when(this.packetSender)
                .syncSendPacket(Mockito.any(IQ.class));
        this.iqResponse.setError(new PacketError(PacketError.Condition.forbidden));

        // exercise/verify
        this.remoteStopOrderRequest.send();
    }

}
