package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.instances.InstanceState;
import org.fogbowcloud.ras.core.models.instances.PublicIpInstance;
import org.fogbowcloud.ras.core.models.orders.PublicIpOrder;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.models.tokens.OpenNebulaToken;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.secgroup.SecurityGroup;
import org.opennebula.client.vm.VirtualMachine;
import org.opennebula.client.vnet.VirtualNetwork;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({VirtualNetwork.class, SecurityGroup.class})
public class OpenNebulaPublicIpPluginTest {

	private static final String LOCAL_TOKEN_VALUE = "user:password";
	private static final String FAKE_USER_NAME = "fake-user-name";
	private static final String FAKE_INSTANCE_ID = "1 1 1 1";
	private static final String VIRTUAL_MACHINE_NIC_IP_PATH = "VM/NIC/IP";
	private static final String VIRTUAL_MACHINE_CONTENT = "<NIC_ID>1</NIC_ID>";

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
	
	// test case: When calling the requestInstance method, with the valid client and 
	// template, a security group will be allocated, a public IP address range will
	// be added to a virtual network, and the NIC containing the reference to the
	// network and the security group will be appended to virtual machine, returning
	// a set of ID's from this instance.
	@Test
	public void testRequestInstanceSuccessful() throws UnexpectedException, FogbowRasException {
		// set up
		OpenNebulaToken token = createOpenNebulaToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		String computeInstanceId = "1";
		PublicIpOrder publicIpOrder = createPublicIpOrder();

		String virtualNetworkId = "1";
		String vnTemplate = generatePublicNetworkTemplate();
		OneResponse vnResponse = Mockito.mock(OneResponse.class);
		PowerMockito.mockStatic(VirtualNetwork.class);
		BDDMockito.given(VirtualNetwork.allocate(Mockito.any(Client.class), Mockito.contains(vnTemplate)))
				.willReturn(vnResponse);
		Mockito.when(vnResponse.isError()).thenReturn(false);
		Mockito.when(vnResponse.getMessage()).thenReturn(virtualNetworkId);

		String securityGroupsId = "1";
		String sgTemplate = generateSecurityGroupsTemplate();
		OneResponse sgResponse = Mockito.mock(OneResponse.class);
		PowerMockito.mockStatic(SecurityGroup.class);
		BDDMockito.given(SecurityGroup.allocate(Mockito.any(Client.class), Mockito.contains(sgTemplate)))
				.willReturn(sgResponse);
		Mockito.when(sgResponse.isError()).thenReturn(false);
		Mockito.when(sgResponse.getMessage()).thenReturn(securityGroupsId);
		
		String nicTemplate = generateNicTemplate();
		OneResponse nicResponse = Mockito.mock(OneResponse.class);
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		Mockito.doReturn(virtualMachine).when(this.factory).createVirtualMachine(client, computeInstanceId);
		Mockito.when(virtualMachine.nicAttach(Mockito.contains(nicTemplate))).thenReturn(nicResponse);
		Mockito.when(nicResponse.isError()).thenReturn(false);

		OneResponse vmResponse = Mockito.mock(OneResponse.class);
		Mockito.when(virtualMachine.info()).thenReturn(vmResponse);
		Mockito.when(vmResponse.getMessage()).thenReturn(VIRTUAL_MACHINE_CONTENT);

		// exercise
		this.plugin.requestInstance(publicIpOrder, computeInstanceId, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.factory, Mockito.times(1)).allocateSecurityGroup(Mockito.eq(client),
				Mockito.eq(sgTemplate));
		Mockito.verify(this.factory, Mockito.times(1)).allocateVirtualNetwork(Mockito.eq(client),
				Mockito.eq(vnTemplate));
		Mockito.verify(this.factory, Mockito.times(1)).createVirtualMachine(client, computeInstanceId);
		Mockito.verify(virtualMachine, Mockito.times(1)).nicAttach(Mockito.eq(nicTemplate));
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
		this.plugin.deleteInstance(instanceId, null, token);
	}

	// test case: When calling the deleteInstance method, with the instance ID and
	// token valid, the instance of public ip will be removed.
	@Test
	public void testDeleteInstanceSuccessful() throws UnexpectedException, FogbowRasException {
		// set up
		OpenNebulaToken token = createOpenNebulaToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		String instanceId = FAKE_INSTANCE_ID;
		String computeInstanceId = "1";
		String networkId = "0";
		int id = 1;
		
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		Mockito.doReturn(virtualMachine).when(this.factory).createVirtualMachine(client, computeInstanceId);
		
		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.when(response.getMessage()).thenReturn(computeInstanceId);
		
		Mockito.when(virtualMachine.nicDetach(id)).thenReturn(response);
		Mockito.doReturn(false).when(response).isError();
		
		VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
		Mockito.doReturn(virtualNetwork).when(this.factory).createVirtualNetwork(client, networkId);
		
		Mockito.when(virtualNetwork.free(id)).thenReturn(response);
		Mockito.doReturn(false).when(response).isError();

		// exercise
		this.plugin.deleteInstance(instanceId, computeInstanceId, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.factory, Mockito.times(1)).createVirtualMachine(Mockito.any(Client.class), Mockito.anyString());
		Mockito.verify(virtualMachine, Mockito.times(1)).nicDetach(id);
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
	// instance ID, it must returned a instance of a public ip.
	@Test
	public void testGetInstanceSuccessful() throws UnexpectedException, FogbowRasException {
		// set up
		OpenNebulaToken token = createOpenNebulaToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		String instanceId = FAKE_INSTANCE_ID;
		String computeInstanceId = "1";
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		Mockito.doReturn(virtualMachine).when(this.factory).createVirtualMachine(client, computeInstanceId);

		String publicIp = "0.0.0.0";
		Mockito.when(virtualMachine.xpath(VIRTUAL_MACHINE_NIC_IP_PATH)).thenReturn(publicIp);
		
		// exercise
		PublicIpInstance publicIpInstance = this.plugin.getInstance(instanceId, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.factory, Mockito.times(1)).createVirtualMachine(Mockito.any(Client.class),
				Mockito.anyString());
		Mockito.verify(virtualMachine, Mockito.times(1)).xpath(Mockito.anyString());
		
		PublicIpInstance expected = new PublicIpInstance(instanceId, InstanceState.READY, publicIp);
		Assert.assertEquals(expected, publicIpInstance);
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
				"    <NIC>\n" + 
				"        <NETWORK_ID>1</NETWORK_ID>\n" + 
				"        <SECURITY_GROUPS>1</SECURITY_GROUPS>\n" + 
				"    </NIC>\n" + 
				"</TEMPLATE>\n";
		
		return template;
	}
	
	private String generatePublicNetworkTemplate() {
		String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + 
				"<TEMPLATE>\n" + 
				"    <BRIDGE>vbr1</BRIDGE>\n" + 
				"    <VN_MAD>fw</VN_MAD>\n" + 
				"    <LEASES>\n" + 
				"        <IP>0.0.0.0</IP>\n" + 
				"    </LEASES>\n" + 
				"    <LEASES>\n" + 
				"        <IP>1.1.1.1</IP>\n" + 
				"    </LEASES>\n" + 
				"    <LEASES>\n" + 
				"        <IP>2.2.2.2</IP>\n" + 
				"    </LEASES>\n" + 
				"    <LEASES>\n" + 
				"        <IP>3.3.3.3</IP>\n" + 
				"    </LEASES>\n" + 
				"    <NAME>public-network</NAME>\n" + 
				"    <TYPE>FIXED</TYPE>\n" + 
				"</TEMPLATE>\n";
		
		return template;
	}
	
	private String generateSecurityGroupsTemplate() {
		String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + 
				"<TEMPLATE>\n" + 
				"    <NAME>Public_IP</NAME>\n" + 
				"    <RULE>\n" + 
				"        <NETWORK_ID>1</NETWORK_ID>\n" + 
				"        <PROTOCOL>TCP</PROTOCOL>\n" + 
				"        <RANGE>1:65536</RANGE>\n" + 
				"        <RULE_TYPE>inbound</RULE_TYPE>\n" + 
				"    </RULE>\n" + 
				"    <RULE>\n" + 
				"        <NETWORK_ID>1</NETWORK_ID>\n" + 
				"        <PROTOCOL>TCP</PROTOCOL>\n" + 
				"        <RANGE>1:65536</RANGE>\n" + 
				"        <RULE_TYPE>outbound</RULE_TYPE>\n" + 
				"    </RULE>\n" + 
				"</TEMPLATE>\n";
		
		return template;
	}	
}
