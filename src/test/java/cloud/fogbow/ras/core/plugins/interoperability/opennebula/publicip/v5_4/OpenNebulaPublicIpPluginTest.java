package cloud.fogbow.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.models.FederationUser;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.instances.InstanceState;
import cloud.fogbow.ras.core.models.instances.PublicIpInstance;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
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

import java.io.File;
import java.util.HashMap;

@RunWith(PowerMockRunner.class)
@PrepareForTest({VirtualNetwork.class, SecurityGroup.class})
public class OpenNebulaPublicIpPluginTest {

	private static final String LOCAL_TOKEN_VALUE = "user:password";
	private static final String FAKE_USER_NAME = "fake-user-name";
	private static final String FAKE_INSTANCE_ID = "1 1 1 1";
	private static final String VIRTUAL_MACHINE_NIC_IP_PATH = "VM/NIC/IP";
	private static final String VIRTUAL_MACHINE_CONTENT = "<NIC_ID>1</NIC_ID>";
	private static final String CLOUD_NAME = "opennebula";

	private OpenNebulaClientFactory factory;
	private OpenNebulaPuplicIpPlugin plugin;
	
	@Before
	public void setUp() {
		String openenbulaConfFilePath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME +
				File.separator + CLOUD_NAME + File.separator + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
		this.factory = Mockito.spy(new OpenNebulaClientFactory(openenbulaConfFilePath));
		this.plugin = Mockito.spy(new OpenNebulaPuplicIpPlugin(openenbulaConfFilePath));
	}
	
	// test case: When calling the requestInstance method, if the OpenNebulaClientFactory class
	// can not create a valid client from a token value, it must throw a UnespectedException.
	@Test(expected = UnexpectedException.class)
	public void testRequestInstanceThrowUnespectedException() throws FogbowException {
		// set up
		PublicIpOrder publicIpOrder = new PublicIpOrder();
		String computeInstanceId = null;
		CloudToken token = createCloudToken();
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
	public void testRequestInstanceSuccessful() throws FogbowException {
		// set up
		CloudToken token = createCloudToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		String computeInstanceId = "1";
		PublicIpOrder publicIpOrder = createPublicIpOrder();
		
		Mockito.doReturn("0.0.0.0").when(this.plugin).getAvailableFixedIp(client);

		String securityGroupsId = "1";
		String sgTemplate = generateSecurityGroupsTemplate(publicIpOrder.getId());
		
		OneResponse sgResponse = Mockito.mock(OneResponse.class);
		PowerMockito.mockStatic(SecurityGroup.class);
		BDDMockito.given(SecurityGroup.allocate(Mockito.any(Client.class), Mockito.contains(sgTemplate)))
				.willReturn(sgResponse);
		Mockito.when(sgResponse.isError()).thenReturn(false);
		Mockito.when(sgResponse.getMessage()).thenReturn(securityGroupsId);
		
		String virtualNetworkId = "1";
		String vnTemplate = generatePublicNetworkTemplate();
		
		OneResponse vnResponse = Mockito.mock(OneResponse.class);
		PowerMockito.mockStatic(VirtualNetwork.class);
		BDDMockito.given(VirtualNetwork.allocate(Mockito.any(Client.class), Mockito.contains(vnTemplate)))
				.willReturn(vnResponse);
		Mockito.when(vnResponse.isError()).thenReturn(false);
		Mockito.when(vnResponse.getMessage()).thenReturn(virtualNetworkId);

		String nicTemplate = generateNicTemplate();
		
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		Mockito.doReturn(virtualMachine).when(this.factory).createVirtualMachine(client, computeInstanceId);
		
		OneResponse vmResponse = Mockito.mock(OneResponse.class);
		Mockito.when(virtualMachine.nicAttach(Mockito.contains(nicTemplate))).thenReturn(vmResponse);
		Mockito.when(vmResponse.isError()).thenReturn(false);

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
	public void testDeleteInstanceThrowUnespectedException() throws FogbowException {
		// set up
		String instanceId = FAKE_INSTANCE_ID;
		CloudToken token = createCloudToken();
		Mockito.doThrow(new UnexpectedException()).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		// exercise
		this.plugin.deleteInstance(instanceId, null, token);
	}

	// test case: When calling the deleteInstance method, with the instance ID and
	// token valid, the instance of public ip will be removed.
	@Test
	public void testDeleteInstanceSuccessful() throws FogbowException {
		// set up
		CloudToken token = createCloudToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		String instanceId = FAKE_INSTANCE_ID;
		String computeInstanceId = "1";
		String virtualNetworkId = "1";
		String securityGroupId = "1";
		int id = 1;
		
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		Mockito.doReturn(virtualMachine).when(this.factory).createVirtualMachine(client, computeInstanceId);
		
		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.when(response.getMessage()).thenReturn(computeInstanceId);
		
		Mockito.when(virtualMachine.nicDetach(id)).thenReturn(response);
		Mockito.doReturn(false).when(response).isError();
		
		VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
		Mockito.doReturn(virtualNetwork).when(this.factory).createVirtualNetwork(client, virtualNetworkId);
		
		Mockito.when(virtualNetwork.delete()).thenReturn(response);
		Mockito.doReturn(false).when(response).isError();

		SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);
		Mockito.doReturn(securityGroup).when(this.factory).createSecurityGroup(client, securityGroupId);
		
		Mockito.when(securityGroup.delete()).thenReturn(response);
		Mockito.doReturn(false).when(response).isError();
		
		// exercise
		this.plugin.deleteInstance(instanceId, computeInstanceId, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.factory, Mockito.times(1)).createVirtualMachine(Mockito.any(Client.class), Mockito.anyString());
		Mockito.verify(this.factory, Mockito.times(1)).createVirtualNetwork(Mockito.any(Client.class), Mockito.anyString());
		Mockito.verify(this.factory, Mockito.times(1)).createSecurityGroup(Mockito.any(Client.class), Mockito.anyString());
		Mockito.verify(virtualMachine, Mockito.times(1)).nicDetach(id);
		Mockito.verify(virtualNetwork, Mockito.times(1)).delete();
		Mockito.verify(securityGroup, Mockito.times(1)).delete();
		Mockito.verify(response, Mockito.times(3)).isError();
	}
	
	// test case: When calling the getInstance method, if the OpenNebulaClientFactory class 
	// can not create a valid client from a token value, it must throw a UnespectedException.
	@Test(expected = UnexpectedException.class) // verify
	public void testGetInstanceThrowUnespectedException() throws FogbowException {
		// set up
		String instanceId = FAKE_INSTANCE_ID;
		CloudToken token = createCloudToken();
		Mockito.doThrow(new UnexpectedException()).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		// exercise
		this.plugin.getInstance(instanceId, token);
	}
	
	// test case: When calling the getInstance method, with a valid client from a token value and
	// instance ID, it must returned a instance of a public ip.
	@Test
	public void testGetInstanceSuccessful() throws FogbowException {
		// set up
		CloudToken token = createCloudToken();
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
		FederationUser federationUser = null;
		String requestingMember = null;
		String providingMember = null;
		String computeOrderId = "fake-order-id";
		String cloudName = "fake-public-network";

		PublicIpOrder publicIpOrder = new PublicIpOrder(
				federationUser, 
				requestingMember, 
				providingMember,
				cloudName,
				computeOrderId);
		
		return publicIpOrder;
	}

	private CloudToken createCloudToken() {
		String provider = null;
		String tokenValue = LOCAL_TOKEN_VALUE;
		String userId = null;
		String userName = FAKE_USER_NAME;

		return new CloudToken(new FederationUser(provider, userId, userName, LOCAL_TOKEN_VALUE, new HashMap<>()));
	}

	private String generateNicTemplate() {
		String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + 
				"<TEMPLATE>\n" + 
				"    <NIC>\n" + 
				"        <NETWORK_ID>1</NETWORK_ID>\n" + 
//				"        <SECURITY_GROUPS>1</SECURITY_GROUPS>\n" + 
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
//				"    <LEASES>\n" + 
//				"        <IP>1.1.1.1</IP>\n" + 
//				"    </LEASES>\n" + 
//				"    <LEASES>\n" + 
//				"        <IP>2.2.2.2</IP>\n" + 
//				"    </LEASES>\n" + 
//				"    <LEASES>\n" + 
//				"        <IP>3.3.3.3</IP>\n" + 
//				"    </LEASES>\n" + 
				"    <NAME>fake-public-network</NAME>\n" + 
				"    <SECURITY_GROUPS>0,1</SECURITY_GROUPS>\n" +
				"    <TYPE>FIXED</TYPE>\n" + 
				"</TEMPLATE>\n";
		
		return template;
	}
	
	private String generateSecurityGroupsTemplate(String publicipOrderId) {
		String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + 
				"<TEMPLATE>\n" + 
				"    <NAME>ras-sg-pip-%s</NAME>\n" + 
				"    <RULE>\n" + 
//				"        <NETWORK_ID>1</NETWORK_ID>\n" + 
				"        <PROTOCOL>ALL</PROTOCOL>\n" + 
				"        <RULE_TYPE>inbound</RULE_TYPE>\n" + 
				"    </RULE>\n" + 
				"    <RULE>\n" + 
//				"        <NETWORK_ID>1</NETWORK_ID>\n" + 
				"        <PROTOCOL>ALL</PROTOCOL>\n" + 
				"        <RULE_TYPE>outbound</RULE_TYPE>\n" + 
				"    </RULE>\n" + 
				"</TEMPLATE>\n";
		
		return String.format(template,publicipOrderId);
	}	
}
