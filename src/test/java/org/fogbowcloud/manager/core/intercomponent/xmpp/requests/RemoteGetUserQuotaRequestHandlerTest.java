package org.fogbowcloud.manager.core.intercomponent.xmpp.requests;

import java.util.HashMap;
import java.util.Map;

import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.intercomponent.RemoteFacade;
import org.fogbowcloud.manager.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.manager.core.intercomponent.xmpp.handlers.RemoteGetUserQuotaRequestHandler;
import org.fogbowcloud.manager.core.intercomponent.xmpp.requesters.RemoteGetUserQuotaRequest;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.quotas.ComputeQuota;
import org.fogbowcloud.manager.core.models.quotas.Quota;
import org.fogbowcloud.manager.core.models.quotas.allocation.ComputeAllocation;
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
public class RemoteGetUserQuotaRequestHandlerTest {
	private PacketSender packetSender;
	private RemoteFacade remoteFacade;

	private static final String FED_USER_ID = "fake-id";
	private static final String PROVIDING_MEMBER = "providingmember";
	
	private RemoteGetUserQuotaRequestHandler remoteGetUserQuotaRequestHandler;
	private RemoteGetUserQuotaRequest getUserQuotaRequest;

	private static final String EXPECTED_QUOTA = "\n<iq type=\"result\" id=\"%s\" from=\"%s\">\n"
					+ "  <query xmlns=\"remoteGetUserQuota\">\n"
					+ "    <userQuota>{\"totalQuota\":{\"vCPU\":1,\"ram\":1,\"instances\":1},"
									+ "\"usedQuota\":{\"vCPU\":1,\"ram\":1,\"instances\":1},"
									+ "\"availableQuota\":{\"vCPU\":0,\"ram\":0,\"instances\":0}}"
						+ "</userQuota>\n"
					+ "    <userQuotaClassName>org.fogbowcloud.manager.core.models.quotas.ComputeQuota</userQuotaClassName>\n"
					+ "  </query>\n</iq>";

	private static final String IQ_ERROR_RESPONSE = "\n<iq type=\"error\" id=\"%s\" from=\"%s\">\n"
					+ "  <error code=\"500\" type=\"wait\">\n"
					+ "    <undefined-condition xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\"/>\n"
					+ "    <text xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\">Unexpected exceptionjava.lang.Exception</text>\n"
					+ "  </error>\n</iq>";

	@Before
	public void setUp() throws InvalidParameterException {
		this.remoteGetUserQuotaRequestHandler = new RemoteGetUserQuotaRequestHandler();
		this.remoteFacade = Mockito.mock(RemoteFacade.class);
		PowerMockito.mockStatic(RemoteFacade.class);

		BDDMockito.given(RemoteFacade.getInstance()).willReturn(this.remoteFacade);
		this.packetSender = Mockito.mock(PacketSender.class);

		PowerMockito.mockStatic(PacketSenderHolder.class);
		BDDMockito.given(PacketSenderHolder.getPacketSender()).willReturn(this.packetSender);
		this.getUserQuotaRequest = new RemoteGetUserQuotaRequest(
												PROVIDING_MEMBER,
												this.createFederationUser(),
												ResourceType.COMPUTE);
	}

	// test case: When the handle method is called passing an IQ request, it must return the User Quota from that.
	@Test
	public void testWithValidIQ() throws Exception {
		// set up
		Quota expectedQuota = this.getQuota();
		
		Mockito.doReturn(expectedQuota)
			.when(this.remoteFacade)
				.getUserQuota(Mockito.anyString(), Mockito.any(), Mockito.any());

		IQ iq = this.getUserQuotaRequest.createIq();

		// exercise
		IQ result = this.remoteGetUserQuotaRequestHandler.handle(iq);

		// verify
		String expected = String.format(EXPECTED_QUOTA, iq.getID(), PROVIDING_MEMBER);

		Mockito.verify(this.remoteFacade, Mockito.times(1))
			.getUserQuota(Mockito.anyString(), Mockito.any(), Mockito.any(ResourceType.class));

		Assert.assertEquals(expected, result.toString());
	}

	// test case: When an Exception occurs, the handle method must return a response error.
	@Test
	public void testUpdateResponseWhenExceptionIsThrown() throws Exception {
		Mockito.when(this.remoteFacade
			.getUserQuota(Mockito.anyString(), Mockito.any(), Mockito.any()))
				.thenThrow(new Exception());

		IQ iq = this.getUserQuotaRequest.createIq();

		// exercise
		IQ result = this.remoteGetUserQuotaRequestHandler.handle(iq);

		// verify
		Mockito.verify(this.remoteFacade, Mockito.times(1))
			.getUserQuota(Mockito.anyString(), Mockito.any(), Mockito.any(ResourceType.class));

		String expected = String.format(IQ_ERROR_RESPONSE, iq.getID(), PROVIDING_MEMBER);
		Assert.assertEquals(expected, result.toString());
	}
	
	private Quota getQuota() {
		// set up
		int vCPU = 1;
		int ram = 1;
		int instances = 1;

		ComputeAllocation totalQuota = new ComputeAllocation(vCPU, ram, instances);
		ComputeAllocation usedQuota = new ComputeAllocation(vCPU, ram, instances);
		return new ComputeQuota(totalQuota, usedQuota);
	}
	
	private FederationUser createFederationUser() throws InvalidParameterException {
		Map<String, String> attributes = new HashMap<>();
		attributes.put("user-name", "fogbow");
		return new FederationUser(FED_USER_ID, attributes);
	}
}
