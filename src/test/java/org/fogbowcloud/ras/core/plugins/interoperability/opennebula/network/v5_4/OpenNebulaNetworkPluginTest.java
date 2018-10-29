package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.network.v5_4;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.orders.NetworkAllocationMode;
import org.fogbowcloud.ras.core.models.orders.NetworkOrder;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.models.tokens.OpenNebulaToken;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.vnet.VirtualNetwork;

public class OpenNebulaNetworkPluginTest {

	private static final String LOCAL_TOKEN_VALUE = "user:password";
	private static final String FAKE_INSTANCE_ID = "fake-instance-id";
	private static final String FAKE_NETWORK_NAME = "fake-network-name";
	private static final String FAKE_ADDRESS = "fake-address";
	private static final String FAKE_GATEWAY = "fake-gateway";
	private static final String FAKE_VLAN_ID = "fake-vlan-id";
	private static final String FAKE_USER_NAME = "fake-user-name";

	private OpenNebulaClientFactory factory;
	private OpenNebulaNetworkPlugin plugin;

	@Before
	public void setUp() {
		this.factory = Mockito.mock(OpenNebulaClientFactory.class);
		this.plugin = Mockito.spy(new OpenNebulaNetworkPlugin());
	}

	// test case: ...
	@Test(expected = UnexpectedException.class)
	public void testRequestInstanceThrowUnespectedExceptionWhenCallCreateClient()
			throws UnexpectedException, FogbowRasException {
		// set up
		NetworkOrder networkOrder = new NetworkOrder();
		OpenNebulaToken token = createOpenNebulaToken();
		Mockito.doThrow(new UnexpectedException()).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		// exercise
		this.plugin.requestInstance(networkOrder, token);
	}

	// test case ...
	@Test
	public void testRequestInstanceSuccessful() throws UnexpectedException, FogbowRasException {
		// set up
		OpenNebulaToken token = createOpenNebulaToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		NetworkOrder networkOrder = createNetworkOrder();
		String template = generateNetworkTemplate();
				
		// exercise
		this.plugin.requestInstance(networkOrder, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.factory, Mockito.times(1)).allocateVirtualNetwork(Mockito.eq(client),
				Mockito.eq(template));
	}

	// test case: ...
	@Test(expected = UnexpectedException.class)
	public void testDeleteInstanceThrowUnespectedExceptionWhenCallCreateClient()
			throws UnexpectedException, FogbowRasException {
		// set up
		String instanceId = FAKE_INSTANCE_ID;
		OpenNebulaToken token = createOpenNebulaToken();
		Mockito.doThrow(new UnexpectedException()).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		// exercise
		this.plugin.deleteInstance(instanceId, token);
	}

	// test case: ...
	@Test
	public void testDeleteInstanceUnsuccessfully() throws UnexpectedException, FogbowRasException {
		// set up
		OpenNebulaToken token = createOpenNebulaToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		String instanceId = FAKE_INSTANCE_ID;
		VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
		Mockito.doReturn(virtualNetwork).when(this.factory).createVirtualNetwork(client, instanceId);
		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.doReturn(response).when(virtualNetwork).delete();
		Mockito.doReturn(true).when(response).isError();

		// exercise
		this.plugin.deleteInstance(instanceId, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.factory, Mockito.times(1)).createVirtualNetwork(Mockito.any(Client.class),
				Mockito.anyString());
		Mockito.verify(virtualNetwork, Mockito.times(1)).delete();
		Mockito.verify(response, Mockito.times(1)).isError();
	}

	// test case: ...
	@Test
	public void testDeleteInstanceSuccessfully() throws UnexpectedException, FogbowRasException {
		// set up
		OpenNebulaToken token = createOpenNebulaToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		String instanceId = FAKE_INSTANCE_ID;
		VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
		Mockito.doReturn(virtualNetwork).when(this.factory).createVirtualNetwork(client, instanceId);
		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.doReturn(response).when(virtualNetwork).delete();
		Mockito.doReturn(false).when(response).isError();

		// exercise
		this.plugin.deleteInstance(instanceId, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.factory, Mockito.times(1)).createVirtualNetwork(Mockito.any(Client.class),
				Mockito.anyString());
		Mockito.verify(virtualNetwork, Mockito.times(1)).delete();
		Mockito.verify(response, Mockito.times(1)).isError();
	}

	// test case: ...
	@Test(expected = UnexpectedException.class)
	public void testGetInstanceThrowUnespectedExceptionWhenCallCreateClient()
			throws UnexpectedException, FogbowRasException {
		// set up
		String instanceId = FAKE_INSTANCE_ID;
		OpenNebulaToken token = createOpenNebulaToken();
		Mockito.doThrow(new UnexpectedException()).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		// exercise
		this.plugin.getInstance(instanceId, token);
	}

	// test case: ...
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
		Mockito.verify(virtualNetwork, Mockito.times(1)).state();
	}

	private NetworkOrder createNetworkOrder() {
		FederationUserToken federationUserToken = null;
		String requestingMember = null;
		String providingMember = null;
		String name = FAKE_NETWORK_NAME;
		String gateway = FAKE_GATEWAY;
		String address = FAKE_ADDRESS;
		NetworkAllocationMode allocation = null;
				
		NetworkOrder networkOrder = new NetworkOrder(
				federationUserToken, 
				requestingMember, 
				providingMember, 
				name, 
				gateway, 
				address, 
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
		return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + 
				"<TEMPLATE>\n" + 
				"    <AR>\n" + 
				"        <IP>fake-address</IP>\n" + 
				"        <SIZE>1</SIZE>\n" + 
				"        <TYPE>IP4</TYPE>\n" + 
				"    </AR>\n" + 
				"    <BRIDGE></BRIDGE>\n" + 
				"    <DESCRIPTION>Virtual network created by fake-user-name.</DESCRIPTION>\n" + 
				"    <NAME>fake-network-name</NAME>\n" + 
				"    <NETWORK_ADDRESS>fake-address</NETWORK_ADDRESS>\n" + 
				"    <NETWORK_GATEWAY>fake-gateway</NETWORK_GATEWAY>\n" + 
				"    <TYPE>RANGED</TYPE>\n" + 
				"</TEMPLATE>\n";
	}
	
}
