package org.fogbowcloud.manager.core.intercomponent.xmpp.requests;

import java.util.HashMap;
import java.util.Map;

import org.dom4j.Element;
import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.intercomponent.RemoteFacade;
import org.fogbowcloud.manager.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.manager.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.intercomponent.xmpp.handlers.RemoteGetUserQuotaRequestHandler;
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

import com.google.gson.Gson;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ RemoteFacade.class, PacketSenderHolder.class })
public class RemoteGetUserQuotaRequestHandlerTest {
	private static final String PROVIDINGMEMBER = "providingmember";

	private static final String EXPECTED_QUOTA = "{\"totalQuota\":" + "{\"vCPU\":1,\"ram\":1,\"instances\":1},"
			+ "\"usedQuota\":{\"vCPU\":1,\"ram\":1,\"instances\":1},"
			+ "\"availableQuota\":{\"vCPU\":0,\"ram\":0,\"instances\":0}}";

	private static final String TAG_RESULT_ERRO = "\n<iq type=\"error\" id=\"%s\" from=\"%s\">\n"
			+ "  <error code=\"500\" type=\"wait\">\n"
			+ "    <undefined-condition xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\"/>\n"
			+ "    <text xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\">Unexpected exceptionjava.lang.Exception</text>\n"
			+ "  </error>\n" + "</iq>";

	private RemoteGetUserQuotaRequestHandler remoteGetUserQuotaRequestHandler;
	private RemoteFacade remoteFacade;
	private PacketSender packetSender;

	@Before
	public void setUp() {
		this.remoteGetUserQuotaRequestHandler = new RemoteGetUserQuotaRequestHandler();
		this.remoteFacade = Mockito.mock(RemoteFacade.class);
		PowerMockito.mockStatic(RemoteFacade.class);

		BDDMockito.given(RemoteFacade.getInstance()).willReturn(this.remoteFacade);
		this.packetSender = Mockito.mock(PacketSender.class);

		PowerMockito.mockStatic(PacketSenderHolder.class);
		BDDMockito.given(PacketSenderHolder.getPacketSender()).willReturn(this.packetSender);
	}

	// test case: When call the handle method passing an IQ request, it must return
	// the User Quota from that.
	@Test
	public void testWithValidIQ() throws Exception {
		int vCPU = 1;
		int ram = 1;
		int instances = 1;

		ComputeAllocation totalQuota = new ComputeAllocation(vCPU, ram, instances);
		ComputeAllocation usedQuota = new ComputeAllocation(vCPU, ram, instances);

		Quota exprectedQuota = new ComputeQuota(totalQuota, usedQuota);
		Mockito.doReturn(exprectedQuota).when(this.remoteFacade).getUserQuota(Mockito.anyString(), Mockito.any(),
				Mockito.any());

		IQ iq = createIq();

		// exercise
		IQ result = this.remoteGetUserQuotaRequestHandler.handle(iq);

		// verify
		Element query = result.getElement().element(IqElement.QUERY.toString());
		Element userQuota = query.element(IqElement.USER_QUOTA.toString());

		Mockito.verify(this.remoteFacade, Mockito.times(1)).getUserQuota(Mockito.anyString(), Mockito.any(),
				Mockito.any(ResourceType.class));

		Assert.assertEquals(EXPECTED_QUOTA, userQuota.getText());
	}

	// test case: When an Exception occurs, the handle method must return a response error.
	@Test
	public void testWhenThrowsException() throws Exception {
		Mockito.when(this.remoteFacade.getUserQuota(Mockito.anyString(), Mockito.any(), Mockito.any()))
				.thenThrow(new Exception());

		IQ iq = createIq();

		// exercise
		IQ result = this.remoteGetUserQuotaRequestHandler.handle(iq);

		// verify
		Mockito.verify(this.remoteFacade, Mockito.times(1)).getUserQuota(Mockito.anyString(), Mockito.any(),
				Mockito.any(ResourceType.class));

		String iqId = iq.getID();
		String providingMember = PROVIDINGMEMBER;
		String expected = String.format(TAG_RESULT_ERRO, iqId, providingMember);
		Assert.assertEquals(expected, result.toString());
	}

	private IQ createIq() throws InvalidParameterException {
		IQ iq = new IQ(IQ.Type.get);
		iq.setTo(PROVIDINGMEMBER);

		String id = "fake-id";
		Map<String, String> attributes = new HashMap<>();
		attributes.put("user-name", "fogbow");

		FederationUser fedUser = new FederationUser(id, attributes);

		Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
				RemoteMethod.REMOTE_GET_USER_QUOTA.toString());

		Element orderIdElement = queryElement.addElement(IqElement.MEMBER_ID.toString());
		orderIdElement.setText(id);

		Element federationUser = queryElement.addElement(IqElement.FEDERATION_USER.toString());
		federationUser.setText(new Gson().toJson(fedUser));

		Element resourceType = queryElement.addElement(IqElement.INSTANCE_TYPE.toString());
		resourceType.setText(ResourceType.COMPUTE.toString());

		return iq;
	}
}
