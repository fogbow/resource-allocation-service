package cloud.fogbow.ras.core.intercomponent.xmpp.handlers;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.intercomponent.RemoteFacade;
import cloud.fogbow.ras.core.intercomponent.xmpp.PacketSenderHolder;
import cloud.fogbow.ras.core.intercomponent.xmpp.requesters.RemoteNotifyEventRequest;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
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

@RunWith(PowerMockRunner.class)
@PrepareForTest({RemoteFacade.class, PacketSenderHolder.class})
public class RemoteNotifyEventHandlerTest {

    private static final String REQUESTING_MEMBER = "requestingmember";
    private static final String IQ_RESULT = "\n<iq type=\"result\" id=\"%s\" from=\"%s\" to=\"%s\"/>";

    private static final String IQ_ERROR_RESULT = "\n<iq type=\"error\" id=\"%s\" from=\"%s\" to=\"%s\">\n"
            + "  <error code=\"500\" type=\"wait\">\n"
            + "    <internal-server-error xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\"/>\n"
            + "    <text xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\">" + Messages.Exception.UNEXPECTED_ERROR + "</text>\n" + "  </error>\n"
            + "</iq>";

    private RemoteNotifyEventHandler remoteNotifyEventHandler;
    private RemoteFacade remoteFacade;
    private PacketSender packetSender;
    private Order order;
    private OrderState newState;

    @Before
    public void setUp() {
        this.remoteNotifyEventHandler = new RemoteNotifyEventHandler();

        this.remoteFacade = Mockito.mock(RemoteFacade.class);
        PowerMockito.mockStatic(RemoteFacade.class);
        BDDMockito.given(RemoteFacade.getInstance()).willReturn(this.remoteFacade);

        this.packetSender = Mockito.mock(PacketSender.class);
        PowerMockito.mockStatic(PacketSenderHolder.class);
        BDDMockito.given(PacketSenderHolder.getPacketSender()).willReturn(this.packetSender);
    }

    // test case: When the handle method is called passing an IQ request, it must
    // return the Order from that.
    @Test
    public void testWithValidIQ() throws Exception {
        // set up
        this.newState = OrderState.FULFILLED;

        String orderId = createOrder();

        Mockito.doNothing().when(this.remoteFacade).handleRemoteEvent(REQUESTING_MEMBER, this.newState, this.order);

        IQ iq = RemoteNotifyEventRequest.marshall(this.order, this.newState);
        iq.setFrom(REQUESTING_MEMBER);

        // exercise
        IQ result = this.remoteNotifyEventHandler.handle(iq);

        // verify
        Mockito.verify(this.remoteFacade, Mockito.times(1)).
                handleRemoteEvent(Mockito.eq(REQUESTING_MEMBER), Mockito.eq(this.newState), Mockito.eq(this.order));

        String requestingMember = SystemConstants.JID_SERVICE_NAME + "@" + SystemConstants.XMPP_SERVER_NAME_PREFIX + this.order.getRequester();
        String expected = String.format(IQ_RESULT, orderId, requestingMember, REQUESTING_MEMBER);

        Assert.assertEquals(expected, result.toString());
    }

    // test case: When an Exception occurs, the handle method must return a response
    // error.
    @Test
    public void testWhenThrowsException() throws FogbowException {
        // set up
        String orderId = createOrder();
        Mockito.doThrow(new UnexpectedException()).when(this.remoteFacade).
                handleRemoteEvent(Mockito.eq(REQUESTING_MEMBER), Mockito.eq(this.newState), Mockito.eq(this.order));

        IQ iq = RemoteNotifyEventRequest.marshall(this.order, this.newState);
        iq.setFrom(REQUESTING_MEMBER);

        // exercise
        IQ result = this.remoteNotifyEventHandler.handle(iq);

        // verify
        Mockito.verify(this.remoteFacade, Mockito.times(1)).
                handleRemoteEvent(Mockito.eq(REQUESTING_MEMBER), Mockito.eq(this.newState), Mockito.eq(this.order));

        String requestingMember = SystemConstants.JID_SERVICE_NAME + "@" + SystemConstants.XMPP_SERVER_NAME_PREFIX + this.order.getRequester();
        String expected = String.format(IQ_ERROR_RESULT, orderId, requestingMember, REQUESTING_MEMBER);

        Assert.assertEquals(expected, result.toString());
    }

    private String createOrder() throws InvalidParameterException {
        this.order = new ComputeOrder(null, REQUESTING_MEMBER, "providingmember",
                "default", "hostName", 1, 2, 3, "imageId", null,
                "publicKey", new ArrayList<>());
        return this.order.getId();
    }

}
