package cloud.fogbow.ras.core.intercomponent.xmpp.requesters;

import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.exceptions.UnavailableProviderException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.core.intercomponent.xmpp.IQMatcher;
import cloud.fogbow.ras.core.intercomponent.xmpp.IqElement;
import cloud.fogbow.ras.core.intercomponent.xmpp.PacketSenderHolder;
import cloud.fogbow.ras.core.intercomponent.xmpp.RemoteMethod;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.api.http.response.Instance;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import com.google.gson.Gson;
import org.dom4j.Element;
import org.jamppa.component.PacketSender;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

public class RemoteGetOrderRequestTest {

    private RemoteGetOrderRequest remoteGetOrderRequest;
    private PacketSender packetSender;
    private SystemUser systemUser;

    private Instance instance;
    private Order order;

    @Before
    public void setUp() {
        this.systemUser = new SystemUser("fake-user-id", "fake-user-name", "token-provider"
        );

        this.order = new ComputeOrder(this.systemUser, "requesting-member",
                "providing-member", "default", "hostName", 10, 20, 30, "imageid", null,
                "publicKey", null);
        this.remoteGetOrderRequest = new RemoteGetOrderRequest(this.order);
        this.packetSender = Mockito.mock(PacketSender.class);
        PacketSenderHolder.setPacketSender(this.packetSender);
        this.instance = new ComputeInstance("compute-instance");
    }

    //test case: checks if IQ attributes is according to both RemoteGetOrderRequest constructor parameters
    //and remote get order request rules. In addition, it checks if the instance from a possible response is
    //properly created and returned by the "send" method
    @Test
    public void testSend() throws Exception {
        //set up
        IQ iqResponse = getInstanceIQResponse(this.instance);
        Mockito.doReturn(iqResponse).when(this.packetSender).syncSendPacket(Mockito.any(IQ.class));
        IQ expectedIQ = RemoteGetOrderRequest.marshal(this.order);

        //exercise
        Instance responseInstance = this.remoteGetOrderRequest.send();

        //verify
        IQMatcher matcher = new IQMatcher(expectedIQ);
        Mockito.verify(this.packetSender).syncSendPacket(Mockito.argThat(matcher));
        Assert.assertEquals(this.instance, responseInstance);
    }

    //test case: checks if "send" is properly forwading UnavailableProviderException thrown by
    //"XmppErrorConditionToExceptionTranslator.handleError" when the IQ response is null
    @Test(expected = UnavailableProviderException.class)
    public void testSendWhenResponseIsNull() throws Exception {
        //set up
        Mockito.doReturn(null).when(this.packetSender).syncSendPacket(Mockito.any());

        //exercise/verify
        this.remoteGetOrderRequest.send();
    }

    //test case: checks if "send" is properly forwading UnauthorizedRequestException thrown by
    //"XmppErrorConditionToExceptionTranslator.handleError" when the IQ response status is forbidden
    @Test(expected = UnauthorizedRequestException.class)
    public void testSendWhenResponseReturnsForbidden() throws Exception {
        //set up
        IQ iqResponse = new IQ();
        Mockito.doReturn(iqResponse).when(this.packetSender).syncSendPacket(Mockito.any());
        iqResponse.setError(new PacketError(PacketError.Condition.forbidden));

        //exercise/verify
        this.remoteGetOrderRequest.send();
    }

    //test case: checks if "send" is properly forwading UnexpectedException thrown by
    //"getInstanceFromResponse" when the instance class name from the IQ response is undefined (wrong or not found)
    @Test(expected = UnexpectedException.class)
    public void testSendWhenImageClassIsUndefined() throws Exception {
        //set up
        Instance instanceResponse = new ComputeInstance("compute-instance");
        IQ iqResponse = getInstanceIQResponseWithWrongClass(instanceResponse);
        Mockito.doReturn(iqResponse).when(this.packetSender).syncSendPacket(Mockito.any());

        //exercise/verify
        this.remoteGetOrderRequest.send();
    }

    private IQ getInstanceIQResponse(Instance instance) {
        IQ iqResponse = new IQ();
        Element queryEl = iqResponse.getElement().addElement(IqElement.QUERY.toString(), RemoteMethod.REMOTE_GET_ORDER.toString());
        Element instanceElement = queryEl.addElement(IqElement.INSTANCE.toString());
        instanceElement.setText(new Gson().toJson(instance));
        Element instanceClassNameElement = queryEl.addElement(IqElement.INSTANCE_CLASS_NAME.toString());
        instanceClassNameElement.setText(instance.getClass().getName());
        return iqResponse;
    }

    private IQ getInstanceIQResponseWithWrongClass(Instance instance) {
        IQ iqResponse = new IQ();
        Element queryEl = iqResponse.getElement().addElement(IqElement.QUERY.toString(), RemoteMethod.REMOTE_GET_ORDER.toString());
        Element instanceElement = queryEl.addElement(IqElement.INSTANCE.toString());
        instanceElement.setText(new Gson().toJson(instance));
        Element instanceClassNameElement = queryEl.addElement(IqElement.INSTANCE_CLASS_NAME.toString());
        instanceClassNameElement.setText("wrong-class-name");
        return iqResponse;
    }
}
