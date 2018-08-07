package org.fogbowcloud.manager.core.intercomponent.xmpp.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.intercomponent.RemoteFacade;
import org.fogbowcloud.manager.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.manager.core.intercomponent.xmpp.requesters.RemoteGetOrderRequest;
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

@RunWith(PowerMockRunner.class)
@PrepareForTest({ RemoteFacade.class, PacketSenderHolder.class })
public class RemoteGetOrderRequestHandlerTest {

	private static final String IQ_RESULT = "\n<iq type=\"result\" id=\"%s\" from=\"%s\">\n"
			+ "  <query xmlns=\"remoteGetOrder\">\n" + "    <instance>{\"id\":\"fake-instance-id\"}</instance>\n"
			+ "    <instanceClassName>org.fogbowcloud.manager.core.models.instances.Instance</instanceClassName>\n"
			+ "  </query>\n" + "</iq>";

	private static final String IQ_ERROR_RESULT = "\n<iq type=\"error\" id=\"%s\" from=\"%s\">\n"
			+ "  <error code=\"500\" type=\"wait\">\n"
			+ "    <undefined-condition xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\"/>\n"
			+ "    <text xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\">Unexpected exceptionjava.lang.Exception</text>\n"
			+ "  </error>\n" + "</iq>";

	private static final String FAKE_INSTANCE_ID = "fake-instance-id";

	private RemoteGetOrderRequestHandler remoteGetOrderRequestHandler;
	private RemoteFacade remoteFacade;
	private PacketSender packetSender;

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

	// test case: When the handle method is called passing an IQ request, it must
	// return the Order from that.
	@Test
	public void testHandleWithValidIQ() throws Exception {
		// set up
		FederationUser federationUser = createFederationUser();
		Order order = createOrder(federationUser);
		String orderId = order.getId();
		Instance instance = new Instance(FAKE_INSTANCE_ID);

		Mockito.when(this.remoteFacade.getResourceInstance(Mockito.eq(orderId), Mockito.eq(federationUser),
				Mockito.eq(ResourceType.COMPUTE))).thenReturn(instance);

		RemoteGetOrderRequest remoteGetOrderRequest = new RemoteGetOrderRequest(order);
		IQ iq = remoteGetOrderRequest.createIq();

		// exercise
		IQ result = this.remoteGetOrderRequestHandler.handle(iq);

		// verify
		Mockito.verify(this.remoteFacade, Mockito.times(1)).getResourceInstance(Mockito.eq(orderId),
				Mockito.eq(federationUser), Mockito.eq(ResourceType.COMPUTE));

		String iqId = iq.getID();
		String providingMember = order.getProvidingMember();
		String expected = String.format(IQ_RESULT, iqId, providingMember);

		Assert.assertEquals(expected, result.toString());

	}

	// test case: When an Exception occurs, the handle method must return a response
	// error.
	@Test
	public void testHandleWhenThrowsException() throws Exception {
		// set up
		FederationUser federationUser = null;
		Order order = createOrder(federationUser);
		String orderId = order.getId();

		Mockito.when(this.remoteFacade.getResourceInstance(Mockito.eq(orderId), Mockito.eq(federationUser),
				Mockito.eq(ResourceType.COMPUTE))).thenThrow(new Exception());

		RemoteGetOrderRequest remoteGetOrderRequest = new RemoteGetOrderRequest(order);
		IQ iq = remoteGetOrderRequest.createIq();

		// exercise
		IQ result = this.remoteGetOrderRequestHandler.handle(iq);

		// verify
		Mockito.verify(this.remoteFacade, Mockito.times(1)).getResourceInstance(Mockito.eq(orderId),
				Mockito.eq(federationUser), Mockito.eq(ResourceType.COMPUTE));

		String iqId = iq.getID();
		String providingMember = order.getProvidingMember();
		String expected = String.format(IQ_ERROR_RESULT, iqId, providingMember);

		Assert.assertEquals(expected, result.toString());
	}

	private Order createOrder(FederationUser federationUser) throws InvalidParameterException {
		return new ComputeOrder(federationUser, "requestingMember", "providingmember", 1, 2, 3, "imageId", null,
				"publicKey", new ArrayList<>());
	}

	private FederationUser createFederationUser() throws InvalidParameterException {
		Map<String, String> attributes = new HashMap<>();
		attributes.put("user-name", "fogbow");
		return new FederationUser("fake-id", attributes);
	}

}
