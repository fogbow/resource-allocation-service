package org.fogbowcloud.manager.core.intercomponent.xmpp.requesters;

import java.util.HashMap;
import java.util.Map;

import org.dom4j.Element;
import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.exceptions.UnauthorizedRequestException;
import org.fogbowcloud.manager.core.exceptions.UnavailableProviderException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.manager.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.instances.Instance;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.jamppa.component.PacketSender;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

import com.google.gson.Gson;

public class RemoteGetOrderRequestTest {
	
 	private RemoteGetOrderRequest remoteGetOrderRequest;
	private PacketSender packetSender;
	private ArgumentCaptor<IQ> argIQ = ArgumentCaptor.forClass(IQ.class);
	private FederationUser federationUser;
	
	private Instance instance;
	private Order order;
	
 	@Before
	public void setUp() throws InvalidParameterException {
		Map<String, String> attributes = new HashMap<String, String>();
 		attributes.put("user-name", "user-name");
 		this.federationUser = new FederationUser("federation-user-id", attributes);
 		this.order = new ComputeOrder(this.federationUser, "requesting-member", "providing-member", 10, 20, 30, "imageid", null,
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
 		String federationUserJson = new Gson().toJson(this.federationUser);
 		IQ iqResponse = getInstanceIQResponse(this.instance);
 		Mockito.doReturn(iqResponse).when(this.packetSender).syncSendPacket(this.argIQ.capture());

 		//exercise
 		Instance responseInstance = this.remoteGetOrderRequest.send();
		
 		//verify
		IQ iq = argIQ.getValue();
		Assert.assertEquals(IQ.Type.get.toString(), iq.getType().toString());
		Assert.assertEquals(this.order.getProvidingMember(), iq.getTo().toString());
		
		Element iqElementQuery = iq.getElement().element(IqElement.QUERY.toString());
		Assert.assertEquals(RemoteMethod.REMOTE_GET_ORDER.toString(), iqElementQuery.getNamespaceURI());
		
		String iqOrderId = iqElementQuery.element(IqElement.ORDER_ID.toString()).getText();
		Assert.assertEquals(this.order.getId(), iqOrderId);
		
		String iqInstanceId = iqElementQuery.element(IqElement.INSTANCE_TYPE.toString()).getText();
		Assert.assertEquals(this.order.getType().toString(), iqInstanceId);
		
		String iqUser = iq.getElement().element(IqElement.FEDERATION_USER.toString()).getText();
		Assert.assertEquals(federationUserJson, iqUser);
		
		Assert.assertEquals(this.instance, responseInstance);
	}
	
	//test case: checks if "send" is properly forwading UnavailableProviderException thrown by 
	//"XmppErrorConditionToExceptionTranslator.handleError" when the IQ response is null
	@Test (expected = UnavailableProviderException.class)
	public void testSendWhenResponseIsNull() throws Exception {
		//set up
		Mockito.doReturn(null).when(this.packetSender).syncSendPacket(this.argIQ.capture());
 		
		//exercise/verify
		this.remoteGetOrderRequest.send();
	}
	
	//test case: checks if "send" is properly forwading UnauthorizedRequestException thrown by 
	//"XmppErrorConditionToExceptionTranslator.handleError" when the IQ response status is forbidden
	@Test (expected = UnauthorizedRequestException.class)
	public void testSendWhenResponseReturnsForbidden() throws Exception {
		//set up
		IQ iqResponse = new IQ();
		Mockito.doReturn(iqResponse).when(this.packetSender).syncSendPacket(this.argIQ.capture());
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
		Mockito.doReturn(iqResponse).when(this.packetSender).syncSendPacket(this.argIQ.capture());

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
