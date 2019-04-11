package cloud.fogbow.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

import java.io.File;

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

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.connectivity.cloud.opennebula.OpenNebulaTagNameConstants;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest({OpenNebulaClientUtil.class, SecurityGroup.class, VirtualNetwork.class})
public class OpenNebulaPublicIpPluginTest {

	private static final String FAKE_INSTANCE_ID = "1 1 1 1";
	private static final String FAKE_IP_ADDRESS = "10.1.0.100";
	private static final String FAKE_INSTANCE_NAME = "fake-instance-name";
	private static final String FAKE_ORDER_ID = "fake-order-id";
	private static final String LOCAL_TOKEN_VALUE = "user:password";
	private static final String OPENNEBULA_CLOUD_NAME_DIRECTORY = "opennebula";
	private static final String STRING_ID_ONE = "1";

	private static final int FAKE_PUBLIC_NETWORK_ID = 100;
	private static final int ID_VALUE_ONE = 1;

	private OpenNebulaPuplicIpPlugin plugin;
	
	@Before
	public void setUp() {
		String opennebulaConfFilePath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME
				+ File.separator + OPENNEBULA_CLOUD_NAME_DIRECTORY + File.separator
				+ SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
		
		this.plugin = Mockito.spy(new OpenNebulaPuplicIpPlugin(opennebulaConfFilePath));
	}
	
	// test case: ...
	@Test
	public void testRequestInstanceSuccessfully() throws FogbowException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		PublicIpOrder publicIpOrder = createPublicIpOrder();
		Mockito.doReturn(FAKE_INSTANCE_NAME).when(this.plugin).getRandomUUID();
		System.out.println(this.plugin.getRandomUUID());

		String publicNetworkReserveTemplate = getPublicNetworkReserveTemplate();
		BDDMockito
				.given(OpenNebulaClientUtil.reserveVirtualNetwork(Mockito.eq(client),
						Mockito.eq(FAKE_PUBLIC_NETWORK_ID), Mockito.eq(publicNetworkReserveTemplate)))
				.willReturn(STRING_ID_ONE);
		
		Mockito.doReturn(STRING_ID_ONE).when(this.plugin).createSecurityGroups(Mockito.eq(client),
				Mockito.eq(publicIpOrder));

		String publicNetworkUpdateTemplate = getPlublicNetworkUpdateTemplate();
		BDDMockito
				.given(OpenNebulaClientUtil.updateVirtualNetwork(Mockito.eq(client),
						Mockito.eq(ID_VALUE_ONE), Mockito.eq(publicNetworkUpdateTemplate)))
				.willReturn(STRING_ID_ONE);
		
		String computeInstanceId = STRING_ID_ONE;
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		BDDMockito.given(OpenNebulaClientUtil.getVirtualMachine(Mockito.eq(client), Mockito.eq(computeInstanceId)))
				.willReturn(virtualMachine);
		
		String nicTemplate = getNicTemplate();
		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.when(virtualMachine.nicAttach(nicTemplate)).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(false);
		Mockito.doReturn(STRING_ID_ONE).when(this.plugin).getContent(Mockito.eq(virtualMachine), Mockito.eq(OpenNebulaTagNameConstants.NIC_ID));
		
		CloudUser cloudUser = createCloudUser();
		String expected = FAKE_INSTANCE_ID;
		
		// exercise
		String instanceId = this.plugin.requestInstance(publicIpOrder, computeInstanceId, cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());
		
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.reserveVirtualNetwork(Mockito.eq(client),
				Mockito.eq(FAKE_PUBLIC_NETWORK_ID), Mockito.eq(publicNetworkReserveTemplate));
		
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.updateVirtualNetwork(Mockito.eq(client),
				Mockito.eq(ID_VALUE_ONE), Mockito.eq(publicNetworkUpdateTemplate));
		
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getVirtualMachine(Mockito.eq(client), Mockito.eq(computeInstanceId));
		
		Mockito.verify(this.plugin, Mockito.times(2)).convertToInteger(Mockito.anyString());
		Mockito.verify(this.plugin, Mockito.times(1)).createSecurityGroups(Mockito.eq(client),
				Mockito.eq(publicIpOrder));
		
		Mockito.verify(this.plugin, Mockito.times(1)).getContent(Mockito.eq(virtualMachine), 
				Mockito.eq(OpenNebulaTagNameConstants.NIC_ID));
		
		Mockito.verify(virtualMachine, Mockito.times(1)).nicAttach(Mockito.eq(nicTemplate));
		Mockito.verify(response, Mockito.times(1)).isError();
		
		Assert.assertEquals(expected, instanceId);
	}

	// test case: ...
	@Test
	public void testGetInstanceSuccessfully() throws FogbowException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		String virtualMachineId = STRING_ID_ONE;
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		BDDMockito.given(OpenNebulaClientUtil.getVirtualMachine(Mockito.eq(client), Mockito.eq(virtualMachineId)))
				.willReturn(virtualMachine);
		
		Mockito.doReturn(FAKE_IP_ADDRESS).when(this.plugin).getContent(Mockito.eq(virtualMachine),
				Mockito.eq(OpenNebulaTagNameConstants.IP));

		String publicIpInstanceId = FAKE_INSTANCE_ID;
		CloudUser cloudUser = createCloudUser();
		PublicIpInstance expected = createPublicIpInstance();

		// exercise
		PublicIpInstance instance = this.plugin.getInstance(publicIpInstanceId, cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());
		
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getVirtualMachine(Mockito.eq(client), Mockito.eq(virtualMachineId));
		
		Assert.assertEquals(expected, instance);
	}
	
	// test case: ...
	@Test
	public void testDeleteInstanceSuccessfully() throws FogbowException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		String virtualMachineId = STRING_ID_ONE;
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		BDDMockito.given(OpenNebulaClientUtil.getVirtualMachine(Mockito.eq(client), Mockito.eq(virtualMachineId)))
				.willReturn(virtualMachine);
		
		Mockito.doReturn(true).when(this.plugin).isPowerOff(virtualMachine);

		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.when(virtualMachine.nicDetach(ID_VALUE_ONE)).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(false);
		
		String securityGroupId = STRING_ID_ONE;
		SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);
		BDDMockito.given(OpenNebulaClientUtil.getSecurityGroup(Mockito.eq(client), Mockito.eq(securityGroupId)))
			.willReturn(securityGroup);
		
		Mockito.when(securityGroup.delete()).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(false);
		
		String virtualNetworkId = STRING_ID_ONE;
		VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
		BDDMockito.given(OpenNebulaClientUtil.getVirtualNetwork(Mockito.eq(client), Mockito.eq(virtualNetworkId)))
			.willReturn(virtualNetwork);
		
		Mockito.when(virtualNetwork.delete()).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(false);
		
		String publicIpInstanceId = FAKE_INSTANCE_ID;
		String computeInstanceId = STRING_ID_ONE;
		CloudUser cloudUser = createCloudUser();
		
		// exercise
		this.plugin.deleteInstance(publicIpInstanceId, computeInstanceId, cloudUser);
		
		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());
		
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getVirtualMachine(Mockito.eq(client), Mockito.eq(virtualMachineId));
		
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getSecurityGroup(Mockito.eq(client), Mockito.eq(securityGroupId));
		
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getVirtualNetwork(Mockito.eq(client), Mockito.eq(virtualNetworkId));
		
		Mockito.verify(this.plugin, Mockito.times(1)).isPowerOff(virtualMachine);
		Mockito.verify(virtualMachine, Mockito.times(1)).nicDetach(ID_VALUE_ONE);
		Mockito.verify(securityGroup, Mockito.times(1)).delete();
		Mockito.verify(virtualNetwork, Mockito.times(1)).delete();
		Mockito.verify(response, Mockito.times(3)).isError();
	}
	
	// test case: ...
	@Test(expected = UnexpectedException.class) // verify
	public void test() throws FogbowException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		String virtualMachineId = STRING_ID_ONE;
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		BDDMockito.given(OpenNebulaClientUtil.getVirtualMachine(Mockito.eq(client), Mockito.eq(virtualMachineId)))
				.willReturn(virtualMachine);
		
		String publicIpInstanceId = FAKE_INSTANCE_ID;
		String computeInstanceId = STRING_ID_ONE;
		CloudUser cloudUser = createCloudUser();
		
		// exercise
		this.plugin.deleteInstance(publicIpInstanceId, computeInstanceId, cloudUser);
	}
	
	private PublicIpInstance createPublicIpInstance() {
		String id = FAKE_INSTANCE_ID;
		String ip = FAKE_IP_ADDRESS;
		InstanceState state = InstanceState.READY;
		return new PublicIpInstance(id, state, ip);
	}

	private CloudUser createCloudUser() {
		String userId = null;
		String userName = null;
		String tokenValue = LOCAL_TOKEN_VALUE;
		return new CloudUser(userId, userName, tokenValue);
	}
	
	private String getNicTemplate() {
		String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + 
				"<TEMPLATE>\n" + 
				"    <NIC>\n" + 
				"        <NETWORK_ID>1</NETWORK_ID>\n" + 
				"    </NIC>\n" + 
				"</TEMPLATE>\n";
		
		return template;
	}

	private String getPlublicNetworkUpdateTemplate() {
		String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + 
				"<TEMPLATE>\n" + 
				"    <SECURITY_GROUPS>0,1</SECURITY_GROUPS>\n" + 
				"</TEMPLATE>\n";
		
		return template;
	}

	private String getPublicNetworkReserveTemplate() {
		String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
				+ "<TEMPLATE>\n" 
				+ "    <NAME>ras-public-ip-fake-instance-name</NAME>\n"
				+ "    <SIZE>1</SIZE>\n" 
				+ "</TEMPLATE>\n";
		
		return template;
	}
	
	private PublicIpOrder createPublicIpOrder() {
		SystemUser systemUser = null;
		String requestingMember = null;
		String providingMember = null;
		String computeOrderId = FAKE_ORDER_ID;
		String cloudName = null;
		return new PublicIpOrder(systemUser, requestingMember, providingMember, cloudName, computeOrderId);
	}
}
