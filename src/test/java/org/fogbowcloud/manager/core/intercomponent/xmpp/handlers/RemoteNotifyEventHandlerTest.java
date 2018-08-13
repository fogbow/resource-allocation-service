package org.fogbowcloud.manager.core.intercomponent.xmpp.handlers;

import java.util.ArrayList;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.intercomponent.RemoteFacade;
import org.fogbowcloud.manager.core.intercomponent.xmpp.Event;
import org.fogbowcloud.manager.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.manager.core.intercomponent.xmpp.requesters.RemoteNotifyEventRequest;
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

@RunWith(PowerMockRunner.class)
@PrepareForTest({ RemoteFacade.class, PacketSenderHolder.class })
public class RemoteNotifyEventHandlerTest {

	private static final String IQ_RESULT = "\n<iq type=\"result\" id=\"%s\" from=\"%s\"/>";

	private static final String IQ_ERROR_RESULT = "\n<iq type=\"error\" id=\"%s\" from=\"%s\">\n"
			+ "  <error code=\"500\" type=\"wait\">\n"
			+ "    <internal-server-error xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\"/>\n"
			+ "    <text xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\">Unexpected exception</text>\n" + "  </error>\n"
			+ "</iq>";

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

	// test case: When the handle method is called passing an IQ request, it must
	// return the Order from that.
	@Test
	public void testWithValidIQ() throws Exception {
		// set up
		this.event = Event.INSTANCE_FULFILLED;

		String orderId = createOrder();

		Mockito.doNothing().when(this.remoteFacade).handleRemoteEvent(this.event, this.order);

		IQ iq = RemoteNotifyEventRequest.marshall(this.order, this.event);

		// exercise
		IQ result = this.remoteNotifyEventHandler.handle(iq);

		// verify
		Mockito.verify(this.remoteFacade, Mockito.times(1)).handleRemoteEvent(Mockito.eq(this.event),
				Mockito.eq(this.order));

		String requestingMember = this.order.getRequestingMember();
		String expected = String.format(IQ_RESULT, orderId, requestingMember);

		Assert.assertEquals(expected, result.toString());
	}

	// test case: When an Exception occurs, the handle method must return a response
	// error.
	@Test
	public void testWhenThrowsException() throws FogbowManagerException, UnexpectedException {
		// set up
		String orderId = createOrder();
		Mockito.doThrow(new UnexpectedException()).when(this.remoteFacade).handleRemoteEvent(Mockito.eq(this.event),
				Mockito.eq(this.order));

		IQ iq = RemoteNotifyEventRequest.marshall(this.order, this.event);

		// exercise
		IQ result = this.remoteNotifyEventHandler.handle(iq);

		// verify
		Mockito.verify(this.remoteFacade, Mockito.times(1)).handleRemoteEvent(Mockito.eq(this.event),
				Mockito.eq(this.order));

		String requestingMember = this.order.getRequestingMember();
		String expected = String.format(IQ_ERROR_RESULT, orderId, requestingMember);

		Assert.assertEquals(expected, result.toString());
	}

	private String createOrder() throws InvalidParameterException {
		this.order = new ComputeOrder(null, "requestingmember", "providingmember", 1, 2, 3, "imageId", null,
				"publicKey", new ArrayList<>());
		return this.order.getId();
	}

}
