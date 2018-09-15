package org.fogbowcloud.ras.core.intercomponent.xmpp.requesters;

import com.google.gson.Gson;
import org.dom4j.Element;
import org.fogbowcloud.ras.core.exceptions.UnauthorizedRequestException;
import org.fogbowcloud.ras.core.exceptions.UnavailableProviderException;
import org.fogbowcloud.ras.core.intercomponent.xmpp.Event;
import org.fogbowcloud.ras.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.ras.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.ras.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.ras.core.models.orders.ComputeOrder;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.jamppa.component.PacketSender;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

public class RemoteNotifyEventRequestTest {

    private RemoteNotifyEventRequest remoteNotifyEventRequest;
    private Order order;
    private PacketSender packetSender;
    private ArgumentCaptor<IQ> argIQ = ArgumentCaptor.forClass(IQ.class);
    private IQ iqResponse;

    private final String requestingMember = "requesting-member";
    private final String providingMember = "providing-member";
    private final Event event = Event.INSTANCE_FULFILLED;

    @Before
    public void setUp() {
        this.order = new ComputeOrder(null, this.requestingMember, this.providingMember, "hostName", 10, 20, 30, "imageid", null,
                "publicKey", null);
        this.remoteNotifyEventRequest = new RemoteNotifyEventRequest(this.order, this.event);
        this.packetSender = Mockito.mock(PacketSender.class);
        PacketSenderHolder.init(packetSender);
        this.iqResponse = new IQ();
    }

    //test case: check if IQ attributes is according to both RemoteNotifyEventRequest constructor parameters
    //and remote notify event request rules
    @Test
    public void testSend() throws Exception {
        //set up
        Mockito.doReturn(this.iqResponse).when(this.packetSender).syncSendPacket(argIQ.capture());
        String orderJson = new Gson().toJson(this.order);

        //exercise
        Void output = this.remoteNotifyEventRequest.send();

        //verify
        IQ iq = argIQ.getValue();
        Assert.assertEquals(IQ.Type.set.toString(), iq.getType().toString());
        Assert.assertEquals(this.order.getRequestingMember().toString(), iq.getTo().toString());
        Assert.assertEquals(this.order.getId(), iq.getID().toString());

        Element iqElementQuery = iq.getElement().element(IqElement.QUERY.toString());
        Assert.assertEquals(RemoteMethod.REMOTE_NOTIFY_EVENT.toString(), iqElementQuery.getNamespaceURI());

        String iqQueryOrderJson = iqElementQuery.element(IqElement.ORDER.toString()).getText();
        Assert.assertEquals(orderJson, iqQueryOrderJson);

        String iqQueryOrderClassName = iqElementQuery.element(IqElement.ORDER_CLASS_NAME.toString()).getText();
        Assert.assertEquals(this.order.getClass().getName(), iqQueryOrderClassName);

        String iqQueryOrderEvent = iqElementQuery.element(IqElement.EVENT.toString()).getText();
        Event eventObjectFromJson = new Gson().fromJson(iqQueryOrderEvent, Event.class);
        Assert.assertEquals(this.event, eventObjectFromJson);

        Assert.assertEquals(null, output);
    }

    //test case: Check if "send" is properly forwarding UnavailableProviderException thrown by
    //"XmppErrorConditionToExceptionTranslator.handleError" when the IQ response is null
    @Test(expected = UnavailableProviderException.class)
    public void testSendWhenResponseIsNull() throws Exception {
        //set up
        Mockito.doReturn(null).when(this.packetSender).syncSendPacket(this.argIQ.capture());
        // exercise/verify
        this.remoteNotifyEventRequest.send();
    }

    //test case: Check if "send" is properly forwarding UnauthorizedRequestException thrown by
    //"XmppErrorConditionToExceptionTranslator.handleError" when the IQ response status is forbidden
    @Test(expected = UnauthorizedRequestException.class)
    public void testSendWhenResponseReturnsForbidden() throws Exception {
        //set up
        Mockito.doReturn(this.iqResponse).when(this.packetSender).syncSendPacket(this.argIQ.capture());
        this.iqResponse.setError(new PacketError(PacketError.Condition.forbidden));

        //exercise/verify
        this.remoteNotifyEventRequest.send();
    }
}
