package cloud.fogbow.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

import java.io.File;
import java.util.UUID;

import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaStateMapper;
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
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
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
@PrepareForTest({OpenNebulaClientUtil.class, SecurityGroup.class, Thread.class, VirtualNetwork.class})
public class OpenNebulaPublicIpPluginTest {

	private static final String ACTIVE_STATE = "ACTIVE";
	private static final String EMPTY_STRING = "";
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
	
	// test case: When calling the requestInstance method, with a client and an
	// order valid, templates must be generated for each resource with the
	// associated data, to reserve a public network, create a specific security
	// group e attached the NIC of this network, returning its instance ID.
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

		Mockito.doReturn(STRING_ID_ONE).when(this.plugin).createSecurityGroup(Mockito.eq(client),
				Mockito.eq(publicIpOrder));

		String publicNetworkUpdateTemplate = getPlublicNetworkUpdateTemplate();
		BDDMockito.given(OpenNebulaClientUtil.updateVirtualNetwork(Mockito.eq(client), Mockito.eq(ID_VALUE_ONE),
				Mockito.eq(publicNetworkUpdateTemplate))).willReturn(STRING_ID_ONE);

		String computeInstanceId = STRING_ID_ONE;
		publicIpOrder.setComputeId(computeInstanceId);
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		BDDMockito.given(OpenNebulaClientUtil.getVirtualMachine(Mockito.eq(client), Mockito.eq(computeInstanceId)))
				.willReturn(virtualMachine);

		String nicTemplate = getNicTemplate();
		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.when(virtualMachine.nicAttach(nicTemplate)).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(false);
		Mockito.doReturn(STRING_ID_ONE).when(this.plugin).getContent(Mockito.eq(virtualMachine),
				Mockito.eq(OpenNebulaTagNameConstants.NIC_ID));

		CloudUser cloudUser = createCloudUser();
		String expected = FAKE_INSTANCE_ID;

		// exercise
		String instanceId = this.plugin.requestInstance(publicIpOrder, cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.reserveVirtualNetwork(Mockito.eq(client), Mockito.eq(FAKE_PUBLIC_NETWORK_ID),
				Mockito.eq(publicNetworkReserveTemplate));

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.updateVirtualNetwork(Mockito.eq(client), Mockito.eq(ID_VALUE_ONE),
				Mockito.eq(publicNetworkUpdateTemplate));

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getVirtualMachine(Mockito.eq(client), Mockito.eq(computeInstanceId));

		Mockito.verify(this.plugin, Mockito.times(2)).convertToInteger(Mockito.anyString());
		Mockito.verify(this.plugin, Mockito.times(1)).createSecurityGroup(Mockito.eq(client),
				Mockito.eq(publicIpOrder));

		Mockito.verify(this.plugin, Mockito.times(1)).getContent(Mockito.eq(virtualMachine),
				Mockito.eq(OpenNebulaTagNameConstants.NIC_ID));

		Mockito.verify(virtualMachine, Mockito.times(1)).nicAttach(Mockito.eq(nicTemplate));
		Mockito.verify(response, Mockito.times(1)).isError();

		Assert.assertEquals(expected, instanceId);
	}

	// test case: When calling the getInstance method, with an instance ID and cloud
	// user valid, it must be returned an instance of a Public IP attached to a
	// virtual machine.
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
	
	// test case: When calling the deleteInstance method, if this separates the
	// resource from the virtual machine by removing the security group and the
	// associated public network, the execution will be successful.
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
		this.plugin.deleteInstance(publicIpInstanceId, cloudUser);

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
	
	// test case: When calling the deleteInstance method and unable to disconnect
	// the virtual machine, to detach the resource, it must return an
	// UnexpectedException.
	@Test(expected = UnexpectedException.class) // verify
	public void testDeleteInstanceTrhowUnexpectedException() throws FogbowException {
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
		this.plugin.deleteInstance(publicIpInstanceId, cloudUser);
	}
	
	// test case: When calling the deletePublicNetwork method and not get a
	// satisfactory response from the cloud, the resource will not be removed.
	@Test
	public void testDeletePublicNetworkUnsuccessfully() throws UnauthorizedRequestException, InstanceNotFoundException,
			InvalidParameterException, UnexpectedException {

		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		String virtualNetworkId = STRING_ID_ONE;
		VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
		BDDMockito.given(OpenNebulaClientUtil.getVirtualNetwork(Mockito.eq(client), Mockito.eq(virtualNetworkId)))
				.willReturn(virtualNetwork);

		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.when(virtualNetwork.delete()).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(true);

		// exercise
		this.plugin.deletePublicNetwork(client, virtualNetworkId);

		// verify
		Mockito.verify(response, Mockito.times(1)).getErrorMessage();
	}
	
	// test case: When calling the deleteSecurityGroup method and not get a
	// satisfactory response from the cloud, the resource will not be removed.
	@Test
	public void testDeleteSecurityGroupUnsuccessfully() throws UnexpectedException, UnauthorizedRequestException,
			InvalidParameterException, InstanceNotFoundException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		String securityGroupId = STRING_ID_ONE;
		SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);
		BDDMockito.given(OpenNebulaClientUtil.getSecurityGroup(Mockito.eq(client), Mockito.eq(securityGroupId)))
				.willReturn(securityGroup);

		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.when(securityGroup.delete()).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(true);

		// exercise
		this.plugin.deleteSecurityGroup(client, securityGroupId);

		// verify
		Mockito.verify(response, Mockito.times(1)).getErrorMessage();
	}
	
	// test case: When calling the detachNetworkInterfaceConnected method and cannot
	// detach the NIC of the virtual machine passed by parameter, this will fail.
	@Test
	public void testDetachNetworkInterfaceConnectedUnsuccessfully()
			throws InvalidParameterException, UnauthorizedRequestException, InstanceNotFoundException {
		// set up
		OneResponse response = Mockito.mock(OneResponse.class);
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		Mockito.when(virtualMachine.nicDetach(Mockito.eq(ID_VALUE_ONE))).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(true);

		String nicId = STRING_ID_ONE;

		// exercise
		this.plugin.detachNetworkInterfaceConnected(virtualMachine, nicId);

		// verify
		Mockito.verify(response, Mockito.times(1)).getErrorMessage();
	}
	
	// test case: When calling the isPowerOff method and exceeding the number of
	// attempts to verify the status of the virtual machine in the cloud, it must
	// return false.
	@Test
	public void testExceededNumberAttemptsToVerifyIsPowerOff() {
		// set up
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		Mockito.when(virtualMachine.stateStr()).thenReturn(ACTIVE_STATE);

		// exercise
		boolean powerOff = this.plugin.isPowerOff(virtualMachine);

		// verify
		Assert.assertFalse(powerOff);
	}

	// test case: When calling the isPowerOff method with a valid virtual machine,
	// it must return true if the state of the virtual machine switches to
	// power-off.
	@Test
	public void testIsPowerOff() {
		// set up
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		Mockito.when(virtualMachine.stateStr()).thenReturn(OpenNebulaPuplicIpPlugin.POWEROFF_STATE);

		// exercise
		boolean powerOff = this.plugin.isPowerOff(virtualMachine);

		// verify
		Assert.assertTrue(powerOff);
	}
	
	// test case: When calling the getContent method with a valid virtual machine,
	// it must return the contents of the last occurrence relative to the tag passed
	// by parameter.
	@Test
	public void testGetContentSuccessfully() {
		// set up
		String xml = generateXML();
		OneResponse response = Mockito.mock(OneResponse.class);
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		Mockito.when(virtualMachine.info()).thenReturn(response);
		Mockito.when(response.getMessage()).thenReturn(xml);

		String tag = OpenNebulaTagNameConstants.NIC_ID;
		String expected = STRING_ID_ONE;

		// exercise
		String content = this.plugin.getContent(virtualMachine, tag);

		// verify
		Mockito.verify(virtualMachine, Mockito.times(1)).info();
		Mockito.verify(response, Mockito.times(1)).getMessage();

		Assert.assertEquals(expected, content);
	}

	// test case: When calling the attachNetworkInterfaceConnected method with a
	// template or a compute instance ID invalid, it must throw an
	// InvalidParameterException.
	@Test(expected = InvalidParameterException.class) // verify
	public void testAttachNetworkInterfaceConnectedThrowInvalidParameterException() throws UnexpectedException,
			UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {

		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		String computeInstanceId = STRING_ID_ONE;
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		BDDMockito.given(OpenNebulaClientUtil.getVirtualMachine(Mockito.eq(client), Mockito.eq(computeInstanceId)))
				.willReturn(virtualMachine);

		String template = EMPTY_STRING;
		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.when(virtualMachine.nicAttach(Mockito.eq(template))).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(true);

		// exercise
		this.plugin.attachNetworkInterfaceConnected(client, computeInstanceId, template);
	}
	
	// test case: When calling the createSecurityGroup method with a valid client
	// and a public IP order, it must create a security group for that public
	// network, returning its ID.
	@Test
	public void testCreateSecurityGroupSuccessfully() throws UnexpectedException, InvalidParameterException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		String instanceId = STRING_ID_ONE;
		String template = getSecurityGroupTemplate();
		BDDMockito.given(OpenNebulaClientUtil.allocateSecurityGroup(Mockito.eq(client), Mockito.eq(template)))
				.willReturn(instanceId);

		PublicIpOrder publicIpOrder = Mockito.mock(PublicIpOrder.class);
		Mockito.when(publicIpOrder.getId()).thenReturn(FAKE_ORDER_ID);

		// exercise
		this.plugin.createSecurityGroup(client, publicIpOrder);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.allocateSecurityGroup(Mockito.eq(client), Mockito.eq(template));
	}

	// test case: Successful call verification of getRandomUUID method.
	@Test
	public void testGetRandomUUIDSuccessfully() {
		// set up
		String value = EMPTY_STRING;
		PowerMockito.mockStatic(UUID.class);
		PowerMockito.when(UUID.randomUUID().toString()).thenReturn(value);

		// exercise
		this.plugin.getRandomUUID();

		// verify
		PowerMockito.verifyStatic(UUID.class, VerificationModeFactory.times(1));
		UUID.randomUUID().toString();
	}
	
	// test case: When calling the convertToInteger method with an invalid numeric
	// string, it must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testConvertToIntegerUnsuccessfully() throws InvalidParameterException {
		// set up
		String number = EMPTY_STRING;
		// exercise
		this.plugin.convertToInteger(number);
	}
	
	private String generateXML() {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" 
				+ "<VM>\n"
				+ "  <TEMPLATE>\n"
				+ "    <NIC>\n"
				+ "      <AR_ID>0</AR_ID>\n"
				+ "    </NIC>\n"
				+ "    <NIC>\n"
				+ "      <NIC_ID>1</NIC_ID>\n"
				+ "    </NIC>\n"
				+ "  </TEMPLATE>\n"
				+ "</VM>\n";
		
		return xml;
	}
	
	private String getSecurityGroupTemplate() {
		String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + 
				"<TEMPLATE>\n" + 
				"    <NAME>ras-sg-pip-fake-order-id</NAME>\n" + 
				"    <RULE>\n" + 
				"        <PROTOCOL>ALL</PROTOCOL>\n" + 
				"        <RULE_TYPE>inbound</RULE_TYPE>\n" + 
				"    </RULE>\n" + 
				"    <RULE>\n" + 
				"        <PROTOCOL>ALL</PROTOCOL>\n" + 
				"        <RULE_TYPE>outbound</RULE_TYPE>\n" + 
				"    </RULE>\n" + 
				"</TEMPLATE>\n";
		return template;
	}
	
	private String getNicTemplate() {
		String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
				+ "<TEMPLATE>\n"
				+ "    <NIC>\n"
				+ "        <NETWORK_ID>1</NETWORK_ID>\n"
				+ "    </NIC>\n"
				+ "</TEMPLATE>\n";
		
		return template;
	}

	private String getPlublicNetworkUpdateTemplate() {
		String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
				+ "<TEMPLATE>\n"
				+ "    <SECURITY_GROUPS>0,1</SECURITY_GROUPS>\n"
				+ "</TEMPLATE>\n";
		
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
	
	private PublicIpInstance createPublicIpInstance() {
		String id = FAKE_INSTANCE_ID;
		String ip = FAKE_IP_ADDRESS;
		return new PublicIpInstance(id, "ready", ip);
	}

	private CloudUser createCloudUser() {
		String userId = null;
		String userName = null;
		String tokenValue = LOCAL_TOKEN_VALUE;
		return new CloudUser(userId, userName, tokenValue);
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
