package cloud.fogbow.ras.core.intercomponent.xmpp.handlers;

import cloud.fogbow.common.exceptions.RemoteCommunicationException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.api.http.response.OrderInstance;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.intercomponent.RemoteFacade;
import cloud.fogbow.ras.core.intercomponent.xmpp.PacketSenderHolder;
import cloud.fogbow.ras.core.intercomponent.xmpp.requesters.RemoteGetInstanceRequest;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.Order;
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
public class RemoteGetInstanceRequestHandlerTest {

    private static final String REQUESTING_MEMBER = "requestingmember";

    private static final String IQ_RESULT = "\n<iq type=\"result\" id=\"%s\" from=\"%s\" to=\"%s\">\n"
            + "  <query xmlns=\"remoteGetOrder\">\n"
            + "    <instance>{\"isReady\":false,\"hasFailed\":false,\"id\":\"fake-instance-id\"}</instance>\n"
            + "    <instanceClassName>cloud.fogbow.ras.api.http.response.OrderInstance</instanceClassName>\n"
            + "  </query>\n" + "</iq>";

    private static final String IQ_ERROR_RESULT = "\n<iq type=\"error\" id=\"%s\" from=\"%s\" to=\"%s\">\n"
            + "  <error code=\"500\" type=\"wait\">\n"
            + "    <undefined-condition xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\"/>\n"
            + "    <text xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\">Error while sending message to remote RAS.</text>\n"
            + "  </error>\n" + "</iq>";

    private static final String FAKE_INSTANCE_ID = "fake-instance-id";

    private RemoteGetInstanceRequestHandler remoteGetInstanceRequestHandler;
    private RemoteFacade remoteFacade;

    private PacketSender packetSender;

    @Before
    public void setUp() {
        this.remoteGetInstanceRequestHandler = new RemoteGetInstanceRequestHandler();

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
    public void testHandleWithValidIQ() throws Exception {
        // set up
        SystemUser systemUser = createFederationUser();
        Order order = createOrder(systemUser);
        String orderId = order.getId();
        OrderInstance instance = new OrderInstance(FAKE_INSTANCE_ID);

        Mockito.when(
                this.remoteFacade.getResourceInstance(Mockito.eq(REQUESTING_MEMBER), Mockito.eq(orderId),
                        Mockito.eq(systemUser), Mockito.eq(ResourceType.COMPUTE))).thenReturn(instance);

        IQ iq = RemoteGetInstanceRequest.marshal(order);
        iq.setFrom(REQUESTING_MEMBER);

        // exercise
        IQ result = this.remoteGetInstanceRequestHandler.handle(iq);

        // verify
        Mockito.verify(this.remoteFacade, Mockito.times(1)).
                getResourceInstance(Mockito.eq(REQUESTING_MEMBER), Mockito.eq(orderId),
                Mockito.eq(systemUser), Mockito.eq(ResourceType.COMPUTE));

        String iqId = iq.getID();
        String providingMember = order.getProvider();
        String expected = String.format(IQ_RESULT, iqId, SystemConstants.JID_SERVICE_NAME + SystemConstants.JID_CONNECTOR + SystemConstants.XMPP_SERVER_NAME_PREFIX + providingMember, REQUESTING_MEMBER);

        Assert.assertEquals(expected, result.toString());

    }

    // test case: When an Exception occurs, the handle method must return a response
    // error.
    @Test
    public void testHandleWhenThrowsException() throws Exception {
        // set up
        SystemUser systemUser = new SystemUser("fake-user-id", "fake-user-name", "fake-token-provider"
        );
        Order order = createOrder(systemUser);
        String orderId = order.getId();

        Mockito.when(this.remoteFacade.getResourceInstance(Mockito.eq(REQUESTING_MEMBER), Mockito.eq(orderId),
                Mockito.eq(systemUser), Mockito.eq(ResourceType.COMPUTE))).thenThrow(new RemoteCommunicationException());

        IQ iq = RemoteGetInstanceRequest.marshal(order);
        iq.setFrom(REQUESTING_MEMBER);

        // exercise
        IQ result = this.remoteGetInstanceRequestHandler.handle(iq);

        // verify
        Mockito.verify(this.remoteFacade, Mockito.times(1)).
                getResourceInstance(Mockito.eq(REQUESTING_MEMBER), Mockito.eq(orderId),
                Mockito.eq(systemUser), Mockito.eq(ResourceType.COMPUTE));

        String iqId = iq.getID();
        String providingMember = SystemConstants.JID_SERVICE_NAME + SystemConstants.JID_CONNECTOR + SystemConstants.XMPP_SERVER_NAME_PREFIX + order.getProvider();
        String expected = String.format(IQ_ERROR_RESULT, iqId, providingMember, REQUESTING_MEMBER);

        Assert.assertEquals(expected, result.toString());
    }

    private Order createOrder(SystemUser systemUser) {
        return new ComputeOrder(systemUser, REQUESTING_MEMBER,
                "providingmember", "default", "hostName", 1, 2, 3,
                "imageId", null,
                "publicKey", new ArrayList<>());
    }

    private SystemUser createFederationUser() {
        return new SystemUser("fake-user-id", "fake-user-name", "fake-token-provider"
        );
    }

}
