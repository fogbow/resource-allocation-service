package cloud.fogbow.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.secgroup.SecurityGroup;
import org.opennebula.client.vm.VirtualMachine;
import org.opennebula.client.vnet.VirtualNetwork;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest({OpenNebulaClientUtil.class, SecurityGroup.class, VirtualNetwork.class})
public class OpenNebulaPublicIpPluginTest {

	private static final String FAKE_INSTANCE_ID = "1 1 1 1";
	private static final String FAKE_IP_ADDRESS = "10.0.0.1";
	private static final String FAKE_ORDER_ID = "fake-order-id";
	private static final String FAKE_PUBLIC_NETWORK = "fake-public-network";
	private static final String ID_VALUE_ONE = "1";
	private static final String LOCAL_TOKEN_VALUE = "user:password";
	private static final String VIRTUAL_MACHINE_NIC_IP_PATH = "VM/NIC/IP";
	private static final String VIRTUAL_MACHINE_CONTENT = "<NIC_ID>1</NIC_ID>";

	private OpenNebulaPuplicIpPlugin plugin;
	
	@Before
	public void setUp() {
		this.plugin = Mockito.spy(new OpenNebulaPuplicIpPlugin());
	}
	
	// test case: When calling the requestInstance method, with the valid client and 
	// template, a security group will be allocated, a public IP address range will
	// be added to a virtual network, and the NIC containing the reference to the
	// network and the security group will be appended to virtual machine, returning
	// a set of ID's from this instance.
	@Test
	public void testRequestInstanceSuccessful() throws FogbowException {
		// set up
		Mockito.doReturn(FAKE_IP_ADDRESS).when(this.plugin).getAvailableFixedIp(Mockito.any(Client.class));
		PublicIpOrder publicIpOrder = createPublicIpOrder();
		String sgTemplate = generateSecurityGroupsTemplate(publicIpOrder.getId());

		String securityGroupsId = ID_VALUE_ONE;
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.allocateSecurityGroup(Mockito.any(Client.class), Mockito.contains(sgTemplate)))
				.willReturn(securityGroupsId);

		String vnTemplate = generatePublicNetworkTemplate(FAKE_IP_ADDRESS);
		String virtualNetworkId = ID_VALUE_ONE;
		BDDMockito.given(OpenNebulaClientUtil.allocateVirtualNetwork(Mockito.any(Client.class), Mockito.contains(vnTemplate)))
				.willReturn(virtualNetworkId);

		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		BDDMockito.given(OpenNebulaClientUtil.getVirtualMachine(Mockito.any(Client.class), Mockito.anyString()))
				.willReturn(virtualMachine);

		String nicTemplate = generateNicTemplate();
		OneResponse vmResponse = Mockito.mock(OneResponse.class);
		Mockito.when(virtualMachine.nicAttach(Mockito.contains(nicTemplate))).thenReturn(vmResponse);
		Mockito.when(vmResponse.isError()).thenReturn(false);
		Mockito.when(virtualMachine.info()).thenReturn(vmResponse);
		Mockito.when(vmResponse.getMessage()).thenReturn(VIRTUAL_MACHINE_CONTENT);

		CloudUser cloudUser = createCloudUser();
		String computeInstanceId = ID_VALUE_ONE;

		// exercise
		this.plugin.requestInstance(publicIpOrder, computeInstanceId, cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getVirtualMachine(Mockito.any(Client.class), Mockito.anyString());

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.allocateSecurityGroup(Mockito.any(Client.class), Mockito.contains(sgTemplate));
		
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.allocateVirtualNetwork(Mockito.any(Client.class), Mockito.contains(vnTemplate));
		
		Mockito.verify(this.plugin, Mockito.times(1)).getAvailableFixedIp(Mockito.any(Client.class));
		Mockito.verify(virtualMachine, Mockito.times(1)).nicAttach(Mockito.eq(nicTemplate));
	}

	// test case: When calling the deleteInstance method, with the instance ID and
	// token valid, the instance of public ip will be removed.
	@Test
	public void testDeleteInstanceSuccessful() throws FogbowException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);
		
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		BDDMockito.given(OpenNebulaClientUtil.getVirtualMachine(Mockito.eq(client), Mockito.anyString()))
				.willReturn(virtualMachine);
		
		String computeInstanceId = ID_VALUE_ONE;
		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.when(response.getMessage()).thenReturn(computeInstanceId);
		
		Mockito.when(virtualMachine.nicDetach(Mockito.anyInt())).thenReturn(response);
		Mockito.doReturn(false).when(response).isError();
		
		VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
		BDDMockito.given(OpenNebulaClientUtil.getVirtualNetwork(Mockito.eq(client), Mockito.anyString()))
				.willReturn(virtualNetwork);
		
		Mockito.when(virtualNetwork.delete()).thenReturn(response);
		Mockito.doReturn(false).when(response).isError();

		SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);
		BDDMockito.given(OpenNebulaClientUtil.getSecurityGroup(Mockito.eq(client), Mockito.anyString()))
				.willReturn(securityGroup);
		
		Mockito.when(securityGroup.delete()).thenReturn(response);
		Mockito.doReturn(false).when(response).isError();
		
		CloudUser cloudUser = createCloudUser();
		String instanceId = FAKE_INSTANCE_ID;
		int id = Integer.parseInt(ID_VALUE_ONE);
		
		// exercise
		this.plugin.deleteInstance(instanceId, computeInstanceId, cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getVirtualMachine(Mockito.eq(client), Mockito.anyString());
		
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getVirtualNetwork(Mockito.eq(client), Mockito.anyString());
		
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getSecurityGroup(Mockito.eq(client), Mockito.anyString());
		
		Mockito.verify(virtualMachine, Mockito.times(1)).nicDetach(id);
		Mockito.verify(virtualNetwork, Mockito.times(1)).delete();
		Mockito.verify(securityGroup, Mockito.times(1)).delete();
		Mockito.verify(response, Mockito.times(3)).isError();
	}
	
	// test case: When calling the getInstance method, with a valid client from a token value and
	// instance ID, it must returned a instance of a public ip.
	@Test
	public void testGetInstanceSuccessful() throws FogbowException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		BDDMockito.given(OpenNebulaClientUtil.getVirtualMachine(Mockito.eq(client), Mockito.anyString()))
				.willReturn(virtualMachine);

		String publicIp = FAKE_IP_ADDRESS;
		Mockito.when(virtualMachine.xpath(VIRTUAL_MACHINE_NIC_IP_PATH)).thenReturn(publicIp);
		
		CloudUser cloudUser = createCloudUser();
		String instanceId = FAKE_INSTANCE_ID;
		
		// exercise
		PublicIpInstance publicIpInstance = this.plugin.getInstance(instanceId, cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getVirtualMachine(Mockito.eq(client), Mockito.anyString());
		
		Mockito.verify(virtualMachine, Mockito.times(1)).xpath(Mockito.anyString());
		
		PublicIpInstance expected = new PublicIpInstance(instanceId, InstanceState.READY, publicIp);
		Assert.assertEquals(expected, publicIpInstance);
	}
	
	private PublicIpOrder createPublicIpOrder() {
		SystemUser systemUser = null;
		String requestingMember = null;
		String providingMember = null;
		String computeOrderId = FAKE_ORDER_ID;
		String cloudName = FAKE_PUBLIC_NETWORK;

		PublicIpOrder publicIpOrder = new PublicIpOrder(
				systemUser,
				requestingMember, 
				providingMember,
				cloudName,
				computeOrderId);
		
		return publicIpOrder;
	}

	private CloudUser createCloudUser() {
		String userId = null;
		String userName = null;
		String tokenValue = LOCAL_TOKEN_VALUE;

		return new CloudUser(userId, userName, tokenValue);
	}

	private String generateNicTemplate() {
		String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + 
				"<TEMPLATE>\n" + 
				"    <NIC>\n" + 
				"        <NETWORK_ID>1</NETWORK_ID>\n" + 
				"    </NIC>\n" + 
				"</TEMPLATE>\n";
		
		return template;
	}
	
	private String generatePublicNetworkTemplate(String ip) {
		String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + 
				"<TEMPLATE>\n" + 
				"    <BRIDGE>vbr1</BRIDGE>\n" + 
				"    <VN_MAD>fw</VN_MAD>\n" + 
				"    <LEASES>\n" + 
				"        <IP>%s</IP>\n" + 
				"    </LEASES>\n" + 
				"    <NAME>fake-public-network</NAME>\n" + 
				"    <SECURITY_GROUPS>0,1</SECURITY_GROUPS>\n" +
				"    <TYPE>FIXED</TYPE>\n" + 
				"</TEMPLATE>\n";
		
		return String.format(template, ip);
	}
	
	private String generateSecurityGroupsTemplate(String publicipOrderId) {
		String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + 
				"<TEMPLATE>\n" + 
				"    <NAME>ras-sg-pip-%s</NAME>\n" + 
				"    <RULE>\n" + 
				"        <PROTOCOL>ALL</PROTOCOL>\n" + 
				"        <RULE_TYPE>inbound</RULE_TYPE>\n" + 
				"    </RULE>\n" + 
				"    <RULE>\n" + 
				"        <PROTOCOL>ALL</PROTOCOL>\n" + 
				"        <RULE_TYPE>outbound</RULE_TYPE>\n" + 
				"    </RULE>\n" + 
				"</TEMPLATE>\n";
		
		return String.format(template,publicipOrderId);
	}	
}
