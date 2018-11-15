package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.orders.PublicIpOrder;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.models.tokens.OpenNebulaToken;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.vm.VirtualMachine;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({VirtualMachine.class})
public class OpenNebulaPublicIpPluginTest {

	private static final String LOCAL_TOKEN_VALUE = "user:password";
	private static final String FAKE_USER_NAME = "fake-user-name";

	private OpenNebulaClientFactory factory;
	private OpenNebulaPuplicIpPlugin plugin;
	
	@Before
	public void setUp() {
		this.factory = Mockito.spy(new OpenNebulaClientFactory());
		this.plugin = Mockito.spy(new OpenNebulaPuplicIpPlugin());
	}
	
	// test case: When calling the requestInstance method, if the OpenNebulaClientFactory class
	// can not create a valid client from a token value, it must throw a UnespectedException.
	@Test(expected = UnexpectedException.class)
	public void testRequestInstanceThrowUnespectedException() throws UnexpectedException, FogbowRasException {
		// set up
		PublicIpOrder publicIpOrder = new PublicIpOrder();
		String computeInstanceId = null;
		OpenNebulaToken token = createOpenNebulaToken();
		Mockito.doThrow(new UnexpectedException()).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		// exercise
		this.plugin.requestInstance(publicIpOrder, computeInstanceId, token);
	}
	
	// test case: ...
	@Test
	public void testRequestInstance() throws UnexpectedException, FogbowRasException {
		// set up
		OpenNebulaToken token = createOpenNebulaToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);
		
		PublicIpOrder publicIpOrder = createPublicIpOrder();
		
		String computeInstanceId = "1";
		
		String securityGroupsId, addressRangeId;
		securityGroupsId = addressRangeId = "fake-id";
		Mockito.doReturn(securityGroupsId).when(this.plugin).addSecurityGroups(client);
		Mockito.doReturn(addressRangeId).when(this.plugin).addAddressRange(client);

		String template = generateNicTemplate();
		
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		Mockito.doReturn(virtualMachine).when(this.factory).createVirtualMachine(client, computeInstanceId);
		Mockito.doReturn(computeInstanceId).when(this.factory).attachNicToVirtualMachine(client, computeInstanceId, template);
		
		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.when(virtualMachine.nicAttach(template)).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(false);
		
		// exercise
		this.plugin.requestInstance(publicIpOrder, computeInstanceId, token);
				
		// verify
		Mockito.verify(this.factory, Mockito.times(1)).createClient(Mockito.anyString());
	}
	
	private PublicIpOrder createPublicIpOrder() {
		FederationUserToken federationUserToken = null;
		String requestingMember = null;
		String providingMember = null;
		String computeOrderId = "fake-order-id";
		
		PublicIpOrder publicIpOrder = new PublicIpOrder(
				federationUserToken, 
				requestingMember, 
				providingMember, 
				computeOrderId);
		
		return publicIpOrder;
	}

	private OpenNebulaToken createOpenNebulaToken() {
		String provider = null;
		String tokenValue = LOCAL_TOKEN_VALUE;
		String userId = null;
		String userName = FAKE_USER_NAME;
		String signature = null;
		
		OpenNebulaToken token = new OpenNebulaToken(
				provider, 
				tokenValue, 
				userId, 
				userName, 
				signature);
		
		return token;
	}

	private String generateNicTemplate() {
		String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + 
				"<TEMPLATE>\n" +
				"	<NIC>\n" + 
				"   	<NETWORK_ID>0</NETWORK_ID>\n" + 
				"   	<SECURITY_GROUPS>1</SECURITY_GROUPS>\n" + 
				"	</NIC>" +
				"</TEMPLATE>";
		
		return template;
	}
	
	@SuppressWarnings("unused")
	private String generateSecurityGroupsTemplate() {
		String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + 
				"<TEMPLATE>\n" + 
				"    <NAME>Public_IP</NAME>\n" + 
				"    <RULE>\n" + 
				"        <NETWORK_ID>0</NETWORK_ID>\n" + 
				"        <PROTOCOL>TCP</PROTOCOL>\n" + 
				"        <RANGE>1000:2000</RANGE>\n" + 
				"        <RULE_TYPE>inbound</RULE_TYPE>\n" + 
				"    </RULE>\n" + 
				"    <RULE>\n" + 
				"        <NETWORK_ID>0</NETWORK_ID>\n" + 
				"        <PROTOCOL>TCP</PROTOCOL>\n" + 
				"        <RANGE>1000:2000</RANGE>\n" + 
				"        <RULE_TYPE>outbound</RULE_TYPE>\n" + 
				"    </RULE>\n" + 
				"</TEMPLATE>";
		
		return template;
	}	
	
}
