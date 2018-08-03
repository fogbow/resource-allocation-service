package org.fogbowcloud.manager.core.intercomponent.xmpp.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.dom4j.Element;
import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.intercomponent.RemoteFacade;
import org.fogbowcloud.manager.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.manager.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.instances.Instance;
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
import com.google.gson.Gson;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RemoteFacade.class, PacketSenderHolder.class})
public class RemoteGetOrderRequestHandlerTest {

    private static final String TAG_RESULT_IQ = "\n<iq type=\"result\" id=\"%s\" from=\"%s\">\n"
            + "  <query xmlns=\"remoteGetOrder\">\n"
            + "    <instance>{\"id\":\"fake-instance-id\"}</instance>\n"
            + "    <instanceClassName>org.fogbowcloud.manager.core.models.instances.Instance</instanceClassName>\n"
            + "  </query>\n" + "</iq>";

    private static final String TAG_RESULT_ERRO = "\n<iq type=\"error\" id=\"%s\" from=\"%s\">\n"
            + "  <error code=\"500\" type=\"wait\">\n"
            + "    <undefined-condition xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\"/>\n"
            + "    <text xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\">Unexpected exceptionjava.lang.Exception</text>\n"
            + "  </error>\n" + "</iq>";

    private static final String FAKE_INSTANCE_ID = "fake-instance-id";

    private RemoteGetOrderRequestHandler remoteGetOrderRequestHandler;
    private RemoteFacade remoteFacade;
    private PacketSender packetSender;
    private Order order;

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

    // test case: When call the handle method passing an IQ request, it must return the Order from
    // that.
    @Test
    public void testHandleWithValidIQ() throws Exception {
        // set up
        FederationUser federationUser = createFederationUser();
        String orderId = createOrder(federationUser);
        Instance instance = new Instance(FAKE_INSTANCE_ID);

        Mockito.when(this.remoteFacade.getResourceInstance(Mockito.eq(orderId),
                Mockito.eq(federationUser), Mockito.eq(ResourceType.COMPUTE))).thenReturn(instance);

        IQ iq = createIq();

        // exercise
        IQ result = this.remoteGetOrderRequestHandler.handle(iq);

        // verify
        Mockito.verify(this.remoteFacade, Mockito.times(1)).getResourceInstance(Mockito.eq(orderId),
                Mockito.eq(federationUser), Mockito.eq(ResourceType.COMPUTE));

        String iqId = iq.getID();
        String providingMember = this.order.getProvidingMember();
        String expected = String.format(TAG_RESULT_IQ, iqId, providingMember);

        Assert.assertEquals(expected, result.toString());

    }

    // test case: When an Exception occurs, the handle method must return a response error.
    @Test
    public void testHandleWhenThrowsException() throws Exception {
        // set up
        FederationUser federationUser = null;
        String orderId = createOrder(federationUser);

        Mockito.when(this.remoteFacade.getResourceInstance(Mockito.eq(orderId),
                Mockito.eq(federationUser), Mockito.eq(ResourceType.COMPUTE)))
                .thenThrow(new Exception());

        IQ iq = createIq();

        // exercise
        IQ result = this.remoteGetOrderRequestHandler.handle(iq);

        // verify
        Mockito.verify(this.remoteFacade, Mockito.times(1)).getResourceInstance(Mockito.eq(orderId),
                Mockito.eq(federationUser), Mockito.eq(ResourceType.COMPUTE));

        String iqId = iq.getID();
        String providingMember = order.getProvidingMember();
        String expected = String.format(TAG_RESULT_ERRO, iqId, providingMember);

        Assert.assertEquals(expected, result.toString());
    }

    private IQ createIq() {
        IQ iq = new IQ(IQ.Type.get);
        iq.setTo(this.order.getProvidingMember());

        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_GET_ORDER.toString());
        Element orderIdElement = queryElement.addElement(IqElement.ORDER_ID.toString());
        orderIdElement.setText(this.order.getId());

        Element orderTypeElement = queryElement.addElement(IqElement.INSTANCE_TYPE.toString());
        orderTypeElement.setText(this.order.getType().toString());

        Element userElement = iq.getElement().addElement(IqElement.FEDERATION_USER.toString());
        userElement.setText(new Gson().toJson(this.order.getFederationUser()));

        return iq;
    }

    private String createOrder(FederationUser federationUser) throws InvalidParameterException {
        this.order = new ComputeOrder(federationUser, "requestingMember", "providingmember", 1, 2,
                3, "imageId", null, "publicKey", new ArrayList<>());
        return this.order.getId();
    }

    private FederationUser createFederationUser() throws InvalidParameterException {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("user-name", "fogbow");
        return new FederationUser("fake-id", attributes);
    }

}
