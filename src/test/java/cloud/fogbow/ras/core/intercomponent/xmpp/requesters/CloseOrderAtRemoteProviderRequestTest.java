package cloud.fogbow.ras.core.intercomponent.xmpp.requesters;

import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.exceptions.UnavailableProviderException;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.intercomponent.xmpp.IqElement;
import cloud.fogbow.ras.core.intercomponent.xmpp.PacketSenderHolder;
import cloud.fogbow.ras.core.intercomponent.xmpp.RemoteMethod;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import com.google.gson.Gson;
import org.dom4j.Element;
import org.jamppa.component.PacketSender;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

public class CloseOrderAtRemoteProviderRequestTest {

    private CloseOrderAtRemoteProviderRequest closeOrderAtRemoteProviderRequest;
    private Order order;
    private PacketSender packetSender;
    private ArgumentCaptor<IQ> argIQ = ArgumentCaptor.forClass(IQ.class);
    private IQ iqResponse;

    private final String requestingMember = "requesting-member";
    private final String providingMember = "providing-member";
    private final OrderState newState = OrderState.FULFILLED;

    @Before
    public void setUp() {
        this.order = new ComputeOrder(null, this.requestingMember, this.providingMember, "default", "hostName", 10, 20, 30, "imageid", null,
                "publicKey", null);
        this.closeOrderAtRemoteProviderRequest = new CloseOrderAtRemoteProviderRequest(this.order);
        this.packetSender = Mockito.mock(PacketSender.class);
        PacketSenderHolder.setPacketSender(this.packetSender);
        this.iqResponse = new IQ();
    }

    //test case: check if IQ attributes is according to CloseOrderAtRemoteProviderRequest constructor parameter
    @Test
    public void testSend() throws Exception {
        //set up
        Mockito.doReturn(this.iqResponse).when(this.packetSender).syncSendPacket(argIQ.capture());

        //exercise
        Void output = this.closeOrderAtRemoteProviderRequest.send();

        //verify
        IQ iq = argIQ.getValue();
        Assert.assertEquals(IQ.Type.set.toString(), iq.getType().toString());
        Assert.assertEquals(SystemConstants.JID_SERVICE_NAME + SystemConstants.JID_CONNECTOR + SystemConstants.XMPP_SERVER_NAME_PREFIX + this.order.getRequester(), iq.getTo().toString());
        Assert.assertEquals(this.order.getId(), iq.getID());

        Element iqElementQuery = iq.getElement().element(IqElement.QUERY.toString());
        Assert.assertEquals(RemoteMethod.REMOTE_NOTIFY_EVENT.toString(), iqElementQuery.getNamespaceURI());

        Element remoteOrderIdElement = iqElementQuery.element(IqElement.ORDER_ID.toString());
        Assert.assertEquals(this.order.getId(), remoteOrderIdElement.getText());

        Assert.assertEquals(null, output);
    }

    //test case: Check if "send" is properly forwarding UnavailableProviderException thrown by
    //"XmppErrorConditionToExceptionTranslator.handleError" when the IQ response is null
    @Test(expected = UnavailableProviderException.class)
    public void testSendWhenResponseIsNull() throws Exception {
        //set up
        Mockito.doReturn(null).when(this.packetSender).syncSendPacket(this.argIQ.capture());
        // exercise/verify
        this.closeOrderAtRemoteProviderRequest.send();
    }

    //test case: Check if "send" is properly forwarding UnauthorizedRequestException thrown by
    //"XmppErrorConditionToExceptionTranslator.handleError" when the IQ response status is forbidden
    @Test(expected = UnauthorizedRequestException.class)
    public void testSendWhenResponseReturnsForbidden() throws Exception {
        //set up
        Mockito.doReturn(this.iqResponse).when(this.packetSender).syncSendPacket(this.argIQ.capture());
        this.iqResponse.setError(new PacketError(PacketError.Condition.forbidden));

        //exercise/verify
        this.closeOrderAtRemoteProviderRequest.send();
    }
}
