package org.fogbowcloud.ras.core.intercomponent.xmpp.requesters;

import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.UnauthorizedRequestException;
import org.fogbowcloud.ras.core.exceptions.UnavailableProviderException;
import org.fogbowcloud.ras.core.intercomponent.xmpp.IQMatcher;
import org.fogbowcloud.ras.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.ras.core.models.orders.ComputeOrder;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.jamppa.component.PacketSender;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

public class RemoteCreateOrderRequestTest {

    private final String providingMember = "providing-member";

    private RemoteCreateOrderRequest remoteCreateOrderRequest;
    private Order order;
    private PacketSender packetSender;
    private IQ iqResponse;

    @Before
    public void setUp() throws InvalidParameterException {
        this.order = new ComputeOrder(null, "requesting-member", this.providingMember, "hostName",10, 20, 30,
                "imageid", null,
                "publicKey", null);
        this.remoteCreateOrderRequest = new RemoteCreateOrderRequest(this.order);
        this.packetSender = Mockito.mock(PacketSender.class);
        PacketSenderHolder.init(packetSender);
        this.iqResponse = new IQ();
    }

    //test case: check if IQ attributes is according to both Order parameters and remote create order request rules
    @Test
    public void testSend() throws Exception {
        // set up
        IQ expectedIQ = RemoteCreateOrderRequest.marshal(this.order);

        Mockito.doReturn(this.iqResponse).when(this.packetSender)
                .syncSendPacket(Mockito.any(IQ.class));

        // exercise
        this.remoteCreateOrderRequest.send();

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
        this.remoteCreateOrderRequest.send();
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
        this.remoteCreateOrderRequest.send();
    }
}
