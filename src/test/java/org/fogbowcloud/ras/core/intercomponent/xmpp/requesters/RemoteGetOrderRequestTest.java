package org.fogbowcloud.ras.core.intercomponent.xmpp.requesters;

import com.google.gson.Gson;
import org.dom4j.Element;
import org.fogbowcloud.ras.core.exceptions.UnauthorizedRequestException;
import org.fogbowcloud.ras.core.exceptions.UnavailableProviderException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.intercomponent.xmpp.IQMatcher;
import org.fogbowcloud.ras.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.ras.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.ras.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.ras.core.models.instances.ComputeInstance;
import org.fogbowcloud.ras.core.models.instances.Instance;
import org.fogbowcloud.ras.core.models.orders.ComputeOrder;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
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
    private FederationUserToken federationUserToken;

    private Instance instance;
    private Order order;

    @Before
    public void setUp() {
        this.federationUserToken = new FederationUserToken("fake-token-provider",
                "fake-federation-token-value", "fake-user-id", "fake-user-name");
        this.order = new ComputeOrder(this.federationUserToken, "requesting-member",
                "providing-member", "hostName", 10, 20, 30, "imageid", null,
                "publicKey", null);
        this.remoteGetOrderRequest = new RemoteGetOrderRequest(this.order);
        this.packetSender = Mockito.mock(PacketSender.class);
        PacketSenderHolder.init(packetSender);
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
