package org.fogbowcloud.ras.core.intercomponent.xmpp.handlers;

import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.intercomponent.RemoteFacade;
import org.fogbowcloud.ras.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.ras.core.intercomponent.xmpp.requesters.RemoteGetOrderRequest;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.instances.Instance;
import org.fogbowcloud.ras.core.models.orders.ComputeOrder;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
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
public class RemoteGetOrderRequestHandlerTest {

    private static final String REQUESTING_MEMBER = "requestingmember";

    private static final String IQ_RESULT = "\n<iq type=\"result\" id=\"%s\" from=\"%s\" to=\"%s\">\n"
            + "  <query xmlns=\"remoteGetOrder\">\n"
            + "    <instance>{\"id\":\"fake-instance-id\"}</instance>\n"
            + "    <instanceClassName>org.fogbowcloud.ras.core.models.instances.Instance</instanceClassName>\n"
            + "  </query>\n" + "</iq>";

    private static final String IQ_ERROR_RESULT = "\n<iq type=\"error\" id=\"%s\" from=\"%s\" to=\"%s\">\n"
            + "  <error code=\"500\" type=\"wait\">\n"
            + "    <undefined-condition xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\"/>\n"
            + "    <text xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\">Unexpected exception error: java.lang.Exception.</text>\n"
            + "  </error>\n" + "</iq>";

    private static final String FAKE_INSTANCE_ID = "fake-instance-id";

    private RemoteGetOrderRequestHandler remoteGetOrderRequestHandler;
    private RemoteFacade remoteFacade;

    private PacketSender packetSender;

    @Before
    public void setUp() {
        this.remoteGetOrderRequestHandler = new RemoteGetOrderRequestHandler();

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
        FederationUserToken federationUserToken = createFederationUserToken();
        Order order = createOrder(federationUserToken);
        String orderId = order.getId();
        Instance instance = new Instance(FAKE_INSTANCE_ID);

        Mockito.when(
                this.remoteFacade.getResourceInstance(Mockito.eq(REQUESTING_MEMBER), Mockito.eq(orderId),
                        Mockito.eq(federationUserToken), Mockito.eq(ResourceType.COMPUTE))).thenReturn(instance);

        IQ iq = RemoteGetOrderRequest.marshal(order);
        iq.setFrom(REQUESTING_MEMBER);

        // exercise
        IQ result = this.remoteGetOrderRequestHandler.handle(iq);

        // verify
        Mockito.verify(this.remoteFacade, Mockito.times(1)).
                getResourceInstance(Mockito.eq(REQUESTING_MEMBER), Mockito.eq(orderId),
                Mockito.eq(federationUserToken), Mockito.eq(ResourceType.COMPUTE));

        String iqId = iq.getID();
        String providingMember = order.getProvidingMember();
        String expected = String.format(IQ_RESULT, iqId, providingMember, REQUESTING_MEMBER);

        Assert.assertEquals(expected, result.toString());

    }

    // test case: When an Exception occurs, the handle method must return a response
    // error.
    @Test
    public void testHandleWhenThrowsException() throws Exception {
        // set up
        FederationUserToken federationUserToken = null;
        Order order = createOrder(federationUserToken);
        String orderId = order.getId();

        Mockito.when(this.remoteFacade.getResourceInstance(Mockito.eq(REQUESTING_MEMBER), Mockito.eq(orderId),
                Mockito.eq(federationUserToken), Mockito.eq(ResourceType.COMPUTE))).thenThrow(new Exception());

        IQ iq = RemoteGetOrderRequest.marshal(order);
        iq.setFrom(REQUESTING_MEMBER);

        // exercise
        IQ result = this.remoteGetOrderRequestHandler.handle(iq);

        // verify
        Mockito.verify(this.remoteFacade, Mockito.times(1)).
                getResourceInstance(Mockito.eq(REQUESTING_MEMBER), Mockito.eq(orderId),
                Mockito.eq(federationUserToken), Mockito.eq(ResourceType.COMPUTE));

        String iqId = iq.getID();
        String providingMember = order.getProvidingMember();
        String expected = String.format(IQ_ERROR_RESULT, iqId, providingMember, REQUESTING_MEMBER);

        Assert.assertEquals(expected, result.toString());
    }

    private Order createOrder(FederationUserToken federationUserToken) {
        return new ComputeOrder(federationUserToken, REQUESTING_MEMBER,
                "providingmember", "hostName", 1, 2, 3,
                "imageId", null,
                "publicKey", new ArrayList<>());
    }

    private FederationUserToken createFederationUserToken() throws InvalidParameterException {
        FederationUserToken federationUserToken = new FederationUserToken(REQUESTING_MEMBER,
                "fake-federation-token-value", "fake-user-id", "fake-user-name");
        return federationUserToken;
    }

}
