package cloud.fogbow.ras.core.intercomponent.xmpp.handlers;

import cloud.fogbow.common.constants.Messages;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.intercomponent.RemoteFacade;
import cloud.fogbow.ras.core.intercomponent.xmpp.PacketSenderHolder;
import cloud.fogbow.ras.core.intercomponent.xmpp.requesters.RemoteDeleteOrderRequest;
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
public class RemoteDeleteOrderRequestHandlerTest {

    private static final String REQUESTING_MEMBER = "requestingmember";

    public static final String IQ_RESULT_FORMAT = "\n<iq type=\"result\" id=\"%s\" from=\"%s\" to=\"%s\"/>";

    public static final String IQ_ERROR_RESULT_FORMAT =
            "\n<iq type=\"error\" id=\"%s\" from=\"%s\" to=\"%s\">\n" +
                    "  <error code=\"500\" type=\"wait\">\n" +
                    "    <undefined-condition xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\"/>\n" +
                    "    <text xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\">" + Messages.Exception.FOGBOW + "</text>\n" +
                    "  </error>\n" +
                    "</iq>";

    private RemoteDeleteOrderRequestHandler remoteDeleteOrderRequestHandler;
    private PacketSender packetSender;
    private Order order;
    private RemoteFacade remoteFacade;

    @Before
    public void setUp() {
        this.remoteDeleteOrderRequestHandler = new RemoteDeleteOrderRequestHandler();

        this.packetSender = Mockito.mock(PacketSender.class);

        PowerMockito.mockStatic(PacketSenderHolder.class);
        BDDMockito.given(PacketSenderHolder.getPacketSender()).willReturn(this.packetSender);

        this.remoteFacade = Mockito.mock(RemoteFacade.class);

        PowerMockito.mockStatic(RemoteFacade.class);
        BDDMockito.given(RemoteFacade.getInstance()).willReturn(this.remoteFacade);
    }

    // test case: When calling the method handle passing a valid IQ object, it must create an OK
    // result IQ and return it.
    @Test
    public void testHandleWithValidIQ() throws FogbowException {
        //set up
        SystemUser systemUser = new SystemUser("fake-user-id", "fake-user-name", REQUESTING_MEMBER
        );

        this.order = new ComputeOrder(systemUser, REQUESTING_MEMBER, "providingmember",
                "default", "fake-instance-name", 1, 2, 3, "imageId", null, "publicKey", new ArrayList<>());

        IQ iq = RemoteDeleteOrderRequest.marshal(this.order);
        iq.setFrom(REQUESTING_MEMBER);

        // exercise
        IQ result = this.remoteDeleteOrderRequestHandler.handle(iq);

        //verify
        String orderId = order.getId();
        String orderProvidingMember = SystemConstants.JID_SERVICE_NAME + "@" + SystemConstants.XMPP_SERVER_NAME_PREFIX + order.getProvider();
        String expected = String.format(IQ_RESULT_FORMAT, orderId, orderProvidingMember, REQUESTING_MEMBER);
        Assert.assertEquals(expected, result.toString());
    }

    // test case: When an exception occurs while deleting, the method handle should return a response error
    @Test
    public void testHandleWhenExceptionIsThrown() throws Exception {
        //set up
        SystemUser systemUser = new SystemUser("userId", "userName", "tokenProvider"
        );
        this.order = new ComputeOrder(systemUser, REQUESTING_MEMBER, "providingmember",
                "default", "hostName", 1, 2, 3, "imageId", null, "publicKey", new ArrayList<>());

        Mockito.doThrow(new FogbowException()).when(this.remoteFacade).deleteOrder(this.order.getRequester(),
                this.order.getId(), this.order.getSystemUser(), this.order.getType());

        IQ iq = RemoteDeleteOrderRequest.marshal(this.order);
        iq.setFrom(REQUESTING_MEMBER);

        // exercise
        IQ result = this.remoteDeleteOrderRequestHandler.handle(iq);

        //verify
        Mockito.verify(this.remoteFacade, Mockito.times(1)).
                deleteOrder(this.order.getRequester(), this.order.getId(),
                this.order.getSystemUser(), this.order.getType());

        String orderId = order.getId();
        String orderProvidingMember = SystemConstants.JID_SERVICE_NAME + "@" + SystemConstants.XMPP_SERVER_NAME_PREFIX + order.getProvider();
        String expected = String.format(IQ_ERROR_RESULT_FORMAT, orderId, orderProvidingMember, REQUESTING_MEMBER);
        Assert.assertEquals(expected, result.toString());
    }

}
