package org.fogbowcloud.manager.core.intercomponent.xmpp.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.dom4j.Element;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.intercomponent.RemoteFacade;
import org.fogbowcloud.manager.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.manager.core.intercomponent.xmpp.RemoteMethod;
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
public class RemoteCreateOrderRequestHandlerTest {

    public static final String TAG_RESULT_IQ = "\n<iq type=\"result\" id=\"%s\" from=\"%s\"/>";

    public static final String TAG_RESULT_ERRO = "\n<iq type=\"error\" id=\"%s\" from=\"%s\">\n"
            + "  <error code=\"500\" type=\"wait\">\n"
            + "    <undefined-condition xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\"/>\n"
            + "    <text xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\">Fogbow Manager exception</text>\n"
            + "  </error>\n" + "</iq>";


    private RemoteCreateOrderRequestHandler remoteCreateOrderRequestHandler;
    private PacketSender packetSender;

    private Order order;
    private RemoteFacade remoteFacade;

    @Before
    public void setUp() {
        this.remoteCreateOrderRequestHandler = new RemoteCreateOrderRequestHandler();

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
    public void testHandleWithValidIQ() throws FogbowManagerException, UnexpectedException {
        // set up
        FederationUser federationUser = createFederationUser();
        String orderId = createOrder(federationUser);

        Mockito.doNothing().when(this.remoteFacade).activateOrder(Mockito.eq(this.order));

        IQ iq = createIq();

        // exercise
        IQ result = this.remoteCreateOrderRequestHandler.handle(iq);

        // verify
        String providingMember = order.getProvidingMember();
        String expected = String.format(TAG_RESULT_IQ, orderId, providingMember);
        
        Assert.assertEquals(expected, result.toString());
    }

    // test case: When an Exception occurs, the handle method must return a response error.
    @Test
    public void testHandleWhenThrowsException() throws FogbowManagerException, UnexpectedException {
        // set up
        FederationUser federationUser = null;
        String orderId = createOrder(federationUser);

        Mockito.doThrow(new FogbowManagerException()).when(this.remoteFacade)
                .activateOrder(Mockito.any(Order.class));

        IQ iq = createIq();

        // exercise
        IQ result = this.remoteCreateOrderRequestHandler.handle(iq);

        // verify
        Mockito.verify(this.remoteFacade, Mockito.times(1)).activateOrder(Mockito.eq(order));

        String providingMember = order.getProvidingMember();
        String expected = String.format(TAG_RESULT_ERRO, orderId, providingMember);
        
        Assert.assertEquals(expected, result.toString());
    }

    private IQ createIq() {
        IQ iq = new IQ(IQ.Type.set);
        iq.setTo(this.order.getProvidingMember());
        iq.setID(this.order.getId());

        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_CREATE_ORDER.toString());
        Element orderElement = queryElement.addElement(IqElement.ORDER.toString());

        Element orderClassNameElement =
                queryElement.addElement(IqElement.ORDER_CLASS_NAME.toString());
        orderClassNameElement.setText(this.order.getClass().getName());

        String orderJson = new Gson().toJson(this.order);
        orderElement.setText(orderJson);
        return iq;
    }
    
    private String createOrder(FederationUser federationUser) {
        this.order = new ComputeOrder(federationUser, "requestingMember", "providingmember", 1, 2,
                3, "imageId", null, "publicKey", new ArrayList<>());
        return this.order.getId();
    }

    private FederationUser createFederationUser() throws InvalidParameterException {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("user-name", "fogbow");

        FederationUser federationUser = new FederationUser("fake-id", attributes);
        return federationUser;
    }

}
