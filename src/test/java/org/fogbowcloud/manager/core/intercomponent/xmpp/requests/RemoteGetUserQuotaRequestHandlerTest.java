package org.fogbowcloud.manager.core.intercomponent.xmpp.requests;

import java.util.HashMap;
import java.util.Map;

import org.dom4j.Element;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.intercomponent.RemoteFacade;
import org.fogbowcloud.manager.core.intercomponent.xmpp.IqElement;
import org.fogbowcloud.manager.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.manager.core.intercomponent.xmpp.RemoteMethod;
import org.fogbowcloud.manager.core.intercomponent.xmpp.handlers.RemoteGetUserQuotaRequestHandler;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.jamppa.component.PacketSender;
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
	private static final String TAG_RESULT_IQ = "\n<iq type=\"result\" id=\"%s\" from=\"%s\"/>";
	private static final String TAG_RESULT_ERRO = "\n<iq type=\"error\" id=\"%s\" from=\"%s\">\n"
			+ "  <error code=\"500\" type=\"wait\">\n"
			+ "    <internal-server-error xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\"/>\n"
			+ "    <text xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\">Unexpected exception</text>\n" + "  </error>\n"
			+ "</iq>";

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
	// the Order from
	// that.
	@Test
	public void testWithValidIQ() throws Exception {

	}

	// test case: When an Exception occurs, the handle method must return a response
	// error.
	@Test
	public void testWhenThrowsException() throws FogbowManagerException, UnexpectedException {

	}

	private IQ createIq() throws InvalidParameterException {
		IQ iq = new IQ(IQ.Type.get);
		iq.setTo("providingmember");

		Map<String, String> attributes = new HashMap<>();
		attributes.put("user-name", "fogbow");
		FederationUser fedUser = new FederationUser("fake-id", attributes);

		Element queryElement = iq.getElement().addElement(IqElement.QUERY.toString(),
				RemoteMethod.REMOTE_GET_USER_QUOTA.toString());
		Element orderIdElement = queryElement.addElement(IqElement.MEMBER_ID.toString());
		orderIdElement.setText(Mockito.anyString());

		Element federationUser = queryElement.addElement(IqElement.FEDERATION_USER.toString());
		orderIdElement.setText(new Gson().toJson(fedUser));

		Element resourceType = queryElement.addElement(IqElement.INSTANCE_TYPE.toString());
		orderIdElement.setText(Mockito.anyString());

		return iq;
	}
}
