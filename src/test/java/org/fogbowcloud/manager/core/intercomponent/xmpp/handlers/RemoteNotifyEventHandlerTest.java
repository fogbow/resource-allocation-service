package org.fogbowcloud.manager.core.intercomponent.xmpp.handlers;

import java.util.ArrayList;
import org.dom4j.Element;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.intercomponent.RemoteFacade;
import org.fogbowcloud.manager.core.intercomponent.xmpp.Event;
import org.fogbowcloud.manager.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.manager.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
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
public class RemoteNotifyEventHandlerTest {

    private static final String TAG_RESULT_IQ = "\n<iq type=\"result\" id=\"%s\" from=\"%s\"/>";

    private static final String TAG_RESULT_ERRO = "\n<iq type=\"error\" id=\"%s\" from=\"%s\">\n"
            + "  <error code=\"500\" type=\"wait\">\n"
            + "    <internal-server-error xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\"/>\n"
            + "    <text xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\">Unexpected exception</text>\n"
            + "  </error>\n" + "</iq>";

    private RemoteNotifyEventHandler remoteNotifyEventHandler;
    private RemoteFacade remoteFacade;
    private PacketSender packetSender;
    private Order order;
    private Event event;

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

    // test case: When call the handle method passing an IQ request, it must return the Order from
    // that.
    @Test
    public void testWithValidIQ() throws Exception {
        // set up
        this.event = Event.INSTANCE_FULFILLED;

        String orderId = createOrder();

        Mockito.doNothing().when(this.remoteFacade).handleRemoteEvent(this.event, this.order);

        IQ iq = createIq();

        // exercise
        IQ result = this.remoteNotifyEventHandler.handle(iq);

        // verify
        String requestingMember = this.order.getRequestingMember();
        String expected = String.format(TAG_RESULT_IQ, orderId, requestingMember);

        Assert.assertEquals(expected, result.toString());
    }

    // test case: When an Exception occurs, the handle method must return a response error.
    @Test
    public void testWhenThrowsException() throws FogbowManagerException, UnexpectedException {
        // set up
        String orderId = createOrder();
        Mockito.doThrow(new UnexpectedException()).when(this.remoteFacade)
                .handleRemoteEvent(Mockito.eq(this.event), Mockito.eq(this.order));

        IQ iq = createIq();

        // exercise
        IQ result = this.remoteNotifyEventHandler.handle(iq);

        // verify
        Mockito.verify(this.remoteFacade, Mockito.times(1))
                .handleRemoteEvent(Mockito.eq(this.event), Mockito.eq(this.order));

        String requestingMember = this.order.getRequestingMember();
        String expected = String.format(TAG_RESULT_ERRO, orderId, requestingMember);

        Assert.assertEquals(expected, result.toString());
    }

    private IQ createIq() {
        IQ iq = new IQ(IQ.Type.set);
        iq.setTo(this.order.getRequestingMember());
        iq.setID(this.order.getId());

        Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
                RemoteMethod.REMOTE_NOTIFY_EVENT.toString());
        Element orderElement = queryElement.addElement(IqElement.ORDER.toString());
        orderElement.setText(new Gson().toJson(this.order));

        Element orderClassNameElement =
                queryElement.addElement(IqElement.ORDER_CLASS_NAME.toString());
        orderClassNameElement.setText(this.order.getClass().getName());

        Element eventElement = queryElement.addElement(IqElement.EVENT.toString());
        eventElement.setText(new Gson().toJson(this.event));

        return iq;
    }

    private String createOrder() throws InvalidParameterException {
        this.order = new ComputeOrder(null, "requestingmember", "providingmember", 1, 2, 3,
                "imageId", null, "publicKey", new ArrayList<>());
        return this.order.getId();
    }

}
