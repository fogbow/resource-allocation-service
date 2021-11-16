package cloud.fogbow.ras.core.intercomponent.xmpp.handlers;

import java.util.ArrayList;

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

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.intercomponent.RemoteFacade;
import cloud.fogbow.ras.core.intercomponent.xmpp.PacketSenderHolder;
import cloud.fogbow.ras.core.intercomponent.xmpp.requesters.RemoteStopOrderRequest;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.Order;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RemoteFacade.class, PacketSenderHolder.class})
public class RemoteStopOrderRequestHandlerTest {

    private static final String REQUESTING_MEMBER="requestingmember";

    public static final String IQ_RESULT = "\n<iq type=\"result\" id=\"%s\" from=\"%s\" to=\"%s\"/>";

    public static final String IQ_ERROR_RESULT = "\n<iq type=\"error\" id=\"%s\" from=\"%s\" to=\"%s\">\n"
            + "  <error code=\"500\" type=\"wait\">\n"
            + "    <undefined-condition xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\"/>\n"
            + "    <text xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\"></text>\n"
            + "  </error>\n" + "</iq>";

    private RemoteStopOrderRequestHandler remoteStopOrderRequestHandler;
    private RemoteFacade remoteFacade;
    private PacketSender packetSender;

    @Before
    public void setUp() {
        this.remoteStopOrderRequestHandler = new RemoteStopOrderRequestHandler();

        this.remoteFacade = Mockito.mock(RemoteFacade.class);
        PowerMockito.mockStatic(RemoteFacade.class);
        BDDMockito.given(RemoteFacade.getInstance()).willReturn(this.remoteFacade);

        this.packetSender = Mockito.mock(PacketSender.class);
        PowerMockito.mockStatic(PacketSenderHolder.class);
        BDDMockito.given(PacketSenderHolder.getPacketSender()).willReturn(this.packetSender);
    }

    // test case: When the handle method is called passing an IQ request, it must return the Order from
    // that.
    @Test
    public void testHandleWithValidIQ() throws FogbowException {
        // set up
        SystemUser systemUser = createFederationUser();
        Order order = createOrder(systemUser);

        Mockito.doNothing().when(this.remoteFacade).stopOrder(Mockito.anyString(), 
                Mockito.eq(order.getId()), Mockito.eq(systemUser), 
                Mockito.eq(ResourceType.COMPUTE));

        IQ iq = RemoteStopOrderRequest.marshal(order);
        iq.setFrom(REQUESTING_MEMBER);

        // exercise
        IQ result = this.remoteStopOrderRequestHandler.handle(iq);

        // verify
        Mockito.verify(this.remoteFacade, Mockito.times(1)).
                stopOrder(Mockito.anyString(), Mockito.eq(order.getId()), 
                        Mockito.eq(systemUser), Mockito.eq(ResourceType.COMPUTE));

        String orderId = order.getId();
        String providingMember = SystemConstants.JID_SERVICE_NAME + SystemConstants.JID_CONNECTOR + SystemConstants.XMPP_SERVER_NAME_PREFIX + order.getProvider();
        String expected = String.format(IQ_RESULT, orderId, providingMember, REQUESTING_MEMBER);

        Assert.assertEquals(expected, result.toString());
    }

    // test case: When an Exception occurs, the handle method must return a response error.
    @Test
    public void testHandleWhenThrowsException() throws FogbowException {
        // set up
        SystemUser systemUser = null;
        Order order = createOrder(systemUser);

        Mockito.doThrow(new FogbowException("")).when(this.remoteFacade)
                .stopOrder(Mockito.anyString(), Mockito.eq(order.getId()), 
                        Mockito.eq(systemUser), Mockito.eq(ResourceType.COMPUTE));

        IQ iq = RemoteStopOrderRequest.marshal(order);
        iq.setFrom(REQUESTING_MEMBER);

        // exercise
        IQ result = this.remoteStopOrderRequestHandler.handle(iq);

        // verify
        Mockito.verify(this.remoteFacade, Mockito.times(1)).
                stopOrder(Mockito.anyString(), Mockito.eq(order.getId()), 
                        Mockito.eq(systemUser), Mockito.eq(ResourceType.COMPUTE));

        String orderId = order.getId();
        String providingMember = SystemConstants.JID_SERVICE_NAME + SystemConstants.JID_CONNECTOR + SystemConstants.XMPP_SERVER_NAME_PREFIX + order.getProvider();
        String expected = String.format(IQ_ERROR_RESULT, orderId, providingMember, REQUESTING_MEMBER);

        Assert.assertEquals(expected, result.toString());
    }

    private Order createOrder(SystemUser systemUser) {
        return new ComputeOrder(systemUser, REQUESTING_MEMBER, "providingmember", "default", "hostName", 1, 2,
                3, "imageId", null, "publicKey", new ArrayList<>());
    }

    private SystemUser createFederationUser() throws InvalidParameterException {
        return new SystemUser("fake-user-id", "fake-user-name", REQUESTING_MEMBER
        );
    }


}
