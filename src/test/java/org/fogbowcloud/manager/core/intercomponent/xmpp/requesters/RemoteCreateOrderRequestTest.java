package org.fogbowcloud.manager.core.intercomponent.xmpp.requesters;

import org.dom4j.Element;
import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.exceptions.UnauthorizedRequestException;
import org.fogbowcloud.manager.core.exceptions.UnavailableProviderException;
import org.fogbowcloud.manager.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.manager.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.jamppa.component.PacketSender;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

import com.google.gson.Gson;

public class RemoteCreateOrderRequestTest {

	private final String providingMember = "providing-member";

	private RemoteCreateOrderRequest remoteCreateOrderRequest;
	private Order order;
	private PacketSender packetSender;
	private ArgumentCaptor<IQ> argIQ = ArgumentCaptor.forClass(IQ.class);
	private IQ iqResponse;

	@Before
	public void setUp() throws InvalidParameterException {
		this.order = new ComputeOrder(null, "requesting-member", this.providingMember, 10, 20, 30, "imageid", null,
				"publicKey", null);
		this.remoteCreateOrderRequest = new RemoteCreateOrderRequest(this.order);
		this.packetSender = Mockito.mock(PacketSender.class);
		PacketSenderHolder.init(packetSender);
		this.iqResponse = new IQ();
	}

	//test case: check if IQ attributes is according to both Order parameters and remote create order request rules
	@Test
	public void testSend() throws Exception {
		// set up
		Mockito.doReturn(this.iqResponse).when(this.packetSender).syncSendPacket(argIQ.capture());
		String orderJson = new Gson().toJson(this.order);

		// exercise
		this.remoteCreateOrderRequest.send();

		// verify
		IQ iq = argIQ.getValue();
		Assert.assertEquals(IQ.Type.set.toString(), iq.getType().toString());
		Assert.assertEquals(this.order.getProvidingMember().toString(), iq.getTo().toString());
		Assert.assertEquals(this.order.getId(), iq.getID().toString());
		
		Element iqElementQuery = iq.getElement().element(IqElement.QUERY.toString());
		Assert.assertEquals(RemoteMethod.REMOTE_CREATE_ORDER.toString(), iqElementQuery.getNamespaceURI());
		
		String iqQueryOrderClassName = iqElementQuery.element(IqElement.ORDER_CLASS_NAME.toString()).getText();
		Assert.assertEquals(this.order.getClass().getName(), iqQueryOrderClassName);
		
		String iqQueryOrderJson= iqElementQuery.element(IqElement.ORDER.toString()).getText();
		Assert.assertEquals(orderJson, iqQueryOrderJson);
	}
	
	//test case: Check if "send" is properly forwading UnavailableProviderException thrown by 
	//"XmppErrorConditionToExceptionTranslator.handleError" when the IQ response is null
	@Test (expected = UnavailableProviderException.class)
	public void testSendWhenResponseIsNull() throws Exception {
		// set up
		Mockito.doReturn(null).when(this.packetSender).syncSendPacket(this.argIQ.capture());

		// exercise/verify
		this.remoteCreateOrderRequest.send();
	}
	
	//test case: Check if "send" is properly forwading UnauthorizedRequestException thrown by 
	//"XmppErrorConditionToExceptionTranslator.handleError" when the IQ response status is forbidden
	@Test (expected = UnauthorizedRequestException.class)
	public void testSendWhenResponseReturnsForbidden() throws Exception {
		// set up
		Mockito.doReturn(this.iqResponse).when(this.packetSender).syncSendPacket(this.argIQ.capture());
		this.iqResponse.setError(new PacketError(PacketError.Condition.forbidden));
		
		// exercise/verify
		this.remoteCreateOrderRequest.send();
	}
}
