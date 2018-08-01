package org.fogbowcloud.manager.core.intercomponent.xmpp.handlers;

import com.google.gson.Gson;
import org.dom4j.Element;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.intercomponent.RemoteFacade;
import org.fogbowcloud.manager.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.manager.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
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
import java.util.HashMap;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RemoteFacade.class, PacketSenderHolder.class})
public class RemoteDeleteOrderRequestHandlerTest {

    public static final String TAG_RESULT_IQ = "\n<iq type=\"result\" id=\"%s\" from=\"%s\"/>";

    public static final String TAG_RESULT_ERRO =
            "\n<iq type=\"error\" id=\"%s\" from=\"%s\">\n" +
                    "  <error code=\"500\" type=\"wait\">\n" +
                    "    <undefined-condition xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\"/>\n" +
                    "    <text xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\">Fogbow Manager exception</text>\n" +
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

    // test case: When call the handle method passing a valid IQ object, it must create an OK result IQ and return it.
    @Test
    public void testHandleWithValidIQ() throws FogbowManagerException, UnexpectedException {
        //set up
        Map<String, String> attributes = new HashMap<>();
        attributes.put("user-name", "fogbow");

        FederationUser federationUser = new FederationUser("fake-id", attributes);

        this.order = new ComputeOrder(federationUser, "requestingMember", "providingmember",
                1, 2, 3, "imageId", null, "publicKey", new ArrayList<>());

        Mockito.doNothing().when(this.remoteFacade).activateOrder(Mockito.eq(this.order));

        IQ iq = createIq();

        // exercise
        IQ result = this.remoteDeleteOrderRequestHandler.handle(iq);

        //verify
        String orderId = order.getId();
        String orderProvidingMember = order.getProvidingMember();
        String expected = String.format(TAG_RESULT_IQ, orderId, orderProvidingMember);
        Assert.assertEquals(expected, result.toString());
    }

    // test case: When an Exception occurs, the handle method must return a response error.
    @Test
    public void testHandleWhenThrowsException() throws FogbowManagerException, UnexpectedException {
        //set up
        this.order = new ComputeOrder(null, "requestingMember", "providingmember",
                1, 2, 3, "imageId", null, "publicKey", new ArrayList<>());

        Mockito.doThrow(new FogbowManagerException()).when(this.remoteFacade).deleteOrder(Mockito.anyString(),
                Mockito.any(FederationUser.class), Mockito.any(ResourceType.class));

        IQ iq = createIq();

        // exercise
        IQ result = this.remoteDeleteOrderRequestHandler.handle(iq);

        //verify
        Mockito.verify(this.remoteFacade, Mockito.times(1)).deleteOrder(Mockito.anyString(),
                Mockito.any(FederationUser.class), Mockito.any(ResourceType.class));

        String orderId = order.getId();
        String orderProvidingMember = order.getProvidingMember();
        String expected = String.format(TAG_RESULT_ERRO, orderId, orderProvidingMember);
        Assert.assertEquals(expected, result.toString());
    }

    private IQ createIq() {
        IQ iq = new IQ(IQ.Type.set);
        iq.setTo(this.order.getProvidingMember());
        iq.setID(this.order.getId());

        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_DELETE_ORDER.toString());
        Element orderIdElement = queryElement.addElement(IqElement.ORDER_ID.toString());
        orderIdElement.setText(this.order.getId());

        Element orderTypeElement = queryElement.addElement(IqElement.INSTANCE_TYPE.toString());
        orderTypeElement.setText(this.order.getType().toString());

        Element userElement = iq.getElement().addElement(IqElement.FEDERATION_USER.toString());
        userElement.setText(new Gson().toJson(this.order.getFederationUser()));

        return iq;
    }
}
