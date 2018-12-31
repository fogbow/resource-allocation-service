package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.network.v5_4;

import java.io.File;

import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.SystemConstants;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.NetworkAllocationMode;
import org.fogbowcloud.ras.core.models.orders.NetworkOrder;
import org.fogbowcloud.ras.core.models.tokens.OpenNebulaToken;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.secgroup.SecurityGroup;
import org.opennebula.client.vnet.VirtualNetwork;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SecurityGroup.class, VirtualNetwork.class})
public class OpenNebulaNetworkPluginTest {

	private static final String LOCAL_TOKEN_VALUE = "user:password";
	private static final String FAKE_INSTANCE_ID = "fake-instance-id";
	private static final String FAKE_NETWORK_NAME = "fake-network-name";
	private static final String FAKE_ADDRESS = "fake-address";
	private static final String FAKE_GATEWAY = "fake-gateway";
	private static final String FAKE_VLAN_ID = "fake-vlan-id";
	private static final String FAKE_USER_NAME = "fake-user-name";
	private static final String CLOUD_NAME = "opennebula";

	private OpenNebulaClientFactory factory;
	private OpenNebulaNetworkPlugin plugin;

	@Before
	public void setUp() {
		String openenbulaConfFilePath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME +
				File.separator + CLOUD_NAME + File.separator + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
		this.factory = Mockito.spy(new OpenNebulaClientFactory(openenbulaConfFilePath));
		this.plugin = Mockito.spy(new OpenNebulaNetworkPlugin(openenbulaConfFilePath));
	}

	// test case: When calling the requestInstance method, if the OpenNebulaClientFactory class
	// can not create a valid client from a token value, it must throw a UnespectedException.
	@Test(expected = UnexpectedException.class) // verify
	public void testRequestInstanceThrowUnespectedException() throws UnexpectedException, FogbowRasException {
		// set up
		NetworkOrder networkOrder = new NetworkOrder();
		OpenNebulaToken token = createOpenNebulaToken();
		Mockito.doThrow(new UnexpectedException()).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		// exercise
		this.plugin.requestInstance(networkOrder, token);
	}

	// test case: When calling the requestInstance method, with the valid client and
	// template, a virtual network will be allocated. Returned instance ID.
	@Test
	public void testRequestInstanceSuccessful() throws UnexpectedException, FogbowRasException {
		// set up
		OpenNebulaToken token = createOpenNebulaToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		NetworkOrder networkOrder = createNetworkOrder();
		String template = generateSecurityGroupTemplate();
		
		String securityGroupId = "100";
		OneResponse response = Mockito.mock(OneResponse.class);
		PowerMockito.mockStatic(SecurityGroup.class);
		BDDMockito.given(SecurityGroup.allocate(Mockito.any(), Mockito.any())).willReturn(response);
		Mockito.when(response.isError()).thenReturn(false);
		Mockito.when(response.getMessage()).thenReturn(securityGroupId);
		
		template = generateNetworkTemplate();

		response = Mockito.mock(OneResponse.class);
		PowerMockito.mockStatic(VirtualNetwork.class);
		BDDMockito.given(VirtualNetwork.allocate(Mockito.any(), Mockito.any())).willReturn(response);
		Mockito.when(response.isError()).thenReturn(false);

		// exercise
		this.plugin.requestInstance(networkOrder, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.factory, Mockito.times(1)).allocateVirtualNetwork(Mockito.eq(client), Mockito.eq(template));
	}
	
	// test case: When calling the requestInstance method, and an error occurs while
	// attempting to allocate a virtual network, an InvalidParameterException will
	// be thrown.
	@Test(expected = InvalidParameterException.class)
	public void testRequestInstanceThrowInvalidParameterException() throws UnexpectedException, FogbowRasException {
		// set up
		OpenNebulaToken token = createOpenNebulaToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		NetworkOrder networkOrder = createNetworkOrder();
		String template = generateNetworkTemplate();

		OneResponse response = Mockito.mock(OneResponse.class);
		PowerMockito.mockStatic(VirtualNetwork.class);
		PowerMockito.when(VirtualNetwork.allocate(client, template)).thenReturn(response);
		Mockito.doReturn(true).when(response).isError();

		// exercise
		this.plugin.requestInstance(networkOrder, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.factory, Mockito.times(1)).allocateVirtualNetwork(Mockito.eq(client), Mockito.eq(template));
	}
	
	// test case: When calling the deleteInstance method, if the OpenNebulaClientFactory class
	// can not create a valid client from a token value, it must throw a UnespectedException.
	@Test(expected = UnexpectedException.class) // verify
	public void testDeleteInstanceThrowUnespectedException() throws UnexpectedException, FogbowRasException {
		// set up
		String instanceId = FAKE_INSTANCE_ID;
		OpenNebulaToken token = createOpenNebulaToken();
		Mockito.doThrow(new UnexpectedException()).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		// exercise
		this.plugin.deleteInstance(instanceId, token);
	}

	// test case: When calling the deleteInstance method, if the removal call is not
	// answered an error response is returned.
	@Test
	public void testDeleteInstanceUnsuccessful() throws UnexpectedException, FogbowRasException {
		// set up
		OpenNebulaToken token = createOpenNebulaToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		String instanceId = "1";
		VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
		Mockito.doReturn(virtualNetwork).when(this.factory).createVirtualNetwork(client, instanceId);
		
		String securityGroups = "0,100";
		Mockito.when(virtualNetwork.xpath("/VNET/TEMPLATE/SECURITY_GROUPS")).thenReturn(securityGroups);
		
		String securityGroupId = "100";
		SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);
		Mockito.doReturn(securityGroup).when(this.factory).createSecurityGroup(Mockito.eq(client), Mockito.eq(securityGroupId));
		
		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.when(securityGroup.delete()).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(true);
		
		Mockito.when(virtualNetwork.delete()).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(true);

		// exercise
		this.plugin.deleteInstance(instanceId, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.factory, Mockito.times(1)).createVirtualNetwork(Mockito.any(Client.class),
				Mockito.anyString());
		Mockito.verify(virtualNetwork, Mockito.times(1)).delete();
		Mockito.verify(response, Mockito.times(2)).isError();
	}

	// test case: When calling the deleteInstance method, with the instance ID and
	// token valid, the instance of virtual network will be removed.
	@Test
	public void testDeleteInstanceSuccessful() throws UnexpectedException, FogbowRasException {
		// set up
		OpenNebulaToken token = createOpenNebulaToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		String instanceId = "1";
		VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
		Mockito.doReturn(virtualNetwork).when(this.factory).createVirtualNetwork(client, instanceId);
		
		String securityGroups = "0,100";
		Mockito.when(virtualNetwork.xpath("/VNET/TEMPLATE/SECURITY_GROUPS")).thenReturn(securityGroups);
		
		String securityGroupId = "100";
		SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);
		Mockito.doReturn(securityGroup).when(this.factory).createSecurityGroup(Mockito.eq(client), Mockito.eq(securityGroupId));
		
		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.when(securityGroup.delete()).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(false);
		
		Mockito.when(virtualNetwork.delete()).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(false);
		
		// exercise
		this.plugin.deleteInstance(instanceId, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.factory, Mockito.times(1)).createVirtualNetwork(Mockito.any(Client.class),
				Mockito.anyString());
		Mockito.verify(virtualNetwork, Mockito.times(1)).delete();
		Mockito.verify(response, Mockito.times(2)).isError();
	}

	// test case: When calling the getInstance method, if the OpenNebulaClientFactory class
	// can not create a valid client from a token value, it must throw a UnespectedException.
	@Test(expected = UnexpectedException.class) // verify
	public void testGetInstanceThrowUnespectedException() throws UnexpectedException, FogbowRasException {
		// set up
		String instanceId = FAKE_INSTANCE_ID;
		OpenNebulaToken token = createOpenNebulaToken();
		Mockito.doThrow(new UnexpectedException()).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		// exercise
		this.plugin.getInstance(instanceId, token);
	}

	// test case: When calling the getInstance method, with a valid client from a token value and
	// instance ID, it must returned a instance of a virtual network.
	@Test
	public void testGetInstanceSuccessful() throws UnexpectedException, FogbowRasException {
		// set up
		OpenNebulaToken token = createOpenNebulaToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		String instanceId = FAKE_INSTANCE_ID;
		VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
		Mockito.doReturn(virtualNetwork).when(this.factory).createVirtualNetwork(client, instanceId);

		Mockito.doReturn(FAKE_INSTANCE_ID).when(virtualNetwork).getId();
		Mockito.doReturn(FAKE_NETWORK_NAME).when(virtualNetwork).getName();
		Mockito.doReturn(FAKE_ADDRESS).when(virtualNetwork).xpath(Mockito.anyString());
		Mockito.doReturn(FAKE_GATEWAY).when(virtualNetwork).xpath(Mockito.anyString());
		Mockito.doReturn(FAKE_VLAN_ID).when(virtualNetwork).xpath(Mockito.anyString());
		Mockito.doReturn(1).when(virtualNetwork).state();

		// exercise
		this.plugin.getInstance(instanceId, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.factory, Mockito.times(1)).createVirtualNetwork(Mockito.any(Client.class),
				Mockito.anyString());
		Mockito.verify(virtualNetwork, Mockito.times(1)).getId();
		Mockito.verify(virtualNetwork, Mockito.times(1)).getName();
		Mockito.verify(virtualNetwork, Mockito.times(3)).xpath(Mockito.anyString());
	}

	private NetworkOrder createNetworkOrder() {
		String providingMember = null;
		String cloudName = null;
		String name = FAKE_NETWORK_NAME;
		String gateway = "10.10.10.1";
		String cidr = "10.10.10.0/24";
		NetworkAllocationMode allocation = null;
				
		NetworkOrder networkOrder = new NetworkOrder(
				providingMember, 
				cloudName, 
				name, 
				gateway, 
				cidr, 
				allocation);
		
		return networkOrder;
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
	
	private String generateNetworkTemplate() {
		String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + 
				"<TEMPLATE>\n" + 
				"    <AR>\n" + 
				"        <IP>10.10.10.0</IP>\n" + 
				"        <SIZE>256</SIZE>\n" + 
				"        <TYPE>IP4</TYPE>\n" + 
				"    </AR>\n" + 
				"    <BRIDGE>br0</BRIDGE>\n" + 
				"    <VN_MAD>fw</VN_MAD>\n" + 
				"    <DESCRIPTION>Virtual network created by fake-user-name</DESCRIPTION>\n" + 
				"    <NAME>fake-network-name</NAME>\n" + 
				"    <NETWORK_ADDRESS>10.10.10.0</NETWORK_ADDRESS>\n" + 
				"    <NETWORK_GATEWAY>10.10.10.1</NETWORK_GATEWAY>\n" + 
				"    <SECURITY_GROUPS>0,100</SECURITY_GROUPS>\n" + 
				"    <TYPE>RANGED</TYPE>\n" + 
				"</TEMPLATE>\n";
		
		return template;
	}
	
	private String generateSecurityGroupTemplate() {
		String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + 
				"<TEMPLATE>\n" + 
				"    <NAME>ras-sg-pn-391ba2b9-1946-420e-8dc7-3b6c47bca11f</NAME>\n" + 
				"    <RULE>\n" + 
				"        <IP>10.10.10.0</IP>\n" + 
				"        <PROTOCOL>ALL</PROTOCOL>\n" + 
				"        <SIZE>256</SIZE>\n" + 
				"        <RULE_TYPE>inbound</RULE_TYPE>\n" + 
				"    </RULE>\n" + 
				"    <RULE>\n" + 
				"        <PROTOCOL>ALL</PROTOCOL>\n" + 
				"        <RULE_TYPE>outbound</RULE_TYPE>\n" + 
				"    </RULE>\n" + 
				"</TEMPLATE>\n";
		
		return template;
	}
	
}
