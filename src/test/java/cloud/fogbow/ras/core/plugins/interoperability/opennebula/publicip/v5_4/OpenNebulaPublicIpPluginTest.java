package cloud.fogbow.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

import java.util.UUID;

import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaBaseTests;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.network.v5_4.CreateNetworkReserveRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaStateMapper;

@PrepareForTest({DatabaseManager.class, OpenNebulaClientUtil.class, SecurityGroup.class, Thread.class, VirtualNetwork.class})
public class OpenNebulaPublicIpPluginTest extends OpenNebulaBaseTests {

	private static final String ACTIVE_STATE = "ACTIVE";
	private static final String DEFAULT_CLOUD = "default";
	private static final String EMPTY_STRING = "";
	private static final String FAIL_STATE = "fail";
	private static final String FAKE_ID_PROVIDER = "fake-id-provider";
	private static final String FAKE_IP_ADDRESS = "10.1.0.100";
	private static final String FAKE_INSTANCE_NAME = "fake-instance-name";
	private static final String FAKE_NAME = "fake-name";
	private static final String FAKE_ORDER_ID = "fake-order-id";
	private static final String FAKE_PROVIDER = "fake-provider";
	private static final String FAKE_USER_ID = "fake-user-id";
	private static final String LOCAL_TOKEN_VALUE = "user:password";
	private static final String STRING_ID_ONE = "1";
	private static final String STRING_SECURITY_GROUPS = "0,1";

	private static final int FAKE_PUBLIC_NETWORK_ID = 100;
	private static final int ID_VALUE_ONE = 1;
	private static final int FAKE_SIZE = 1;

	private OpenNebulaPuplicIpPlugin plugin;
	private PublicIpOrder publicIpOrder;
	private String instanceId;

	@Before
	public void setUp() throws FogbowException {
	    super.setUp();

		this.plugin = Mockito.spy(new OpenNebulaPuplicIpPlugin(this.openNebulaConfFilePath));

		this.publicIpOrder = this.createPublicIpOrder();
		this.instanceId = this.publicIpOrder.getInstanceId();
	}
	
	// test case: When calling the isReady method, if the state of public IP is
	// READY, it must return true.
	@Test
	public void testIsReadySuccessful() {
		// set up
		String cloudState = OpenNebulaStateMapper.DEFAULT_READY_STATE;

		// exercise
		boolean status = this.plugin.isReady(cloudState);

		// verify
		Assert.assertTrue(status);
	}
	
	// test case: When calling the isReady method, if the state of public IP is
	// ERROR, it must return false.
	@Test
	public void testIsReadyUnsuccessful() {
		// set up
		String cloudState = OpenNebulaStateMapper.DEFAULT_ERROR_STATE;

		// exercise
		boolean status = this.plugin.isReady(cloudState);

		// verify
		Assert.assertFalse(status);
	}
	
	// test case: When calling the hasFailed method, if the state of public IP is
	// ERROR, it must return true.
	@Test
	public void testHasFailedSuccessful() {
		// set up
		String cloudState = OpenNebulaStateMapper.DEFAULT_ERROR_STATE;

		// exercise
		boolean status = this.plugin.hasFailed(cloudState);

		// verify
		Assert.assertTrue(status);
	}
	
	// test case: When calling the hasFailed method, if the state of public IP is
	// READY, it must return false.
	@Test
	public void testHasFailedUnsuccessful() {
		// set up
		String cloudState = OpenNebulaStateMapper.DEFAULT_READY_STATE;

		// exercise
		boolean status = this.plugin.hasFailed(cloudState);

		// verify
		Assert.assertFalse(status);
	}

	// test case: When calling the requestInstance method, with a client and an
	// order valid, templates must be generated for each resource with the
	// associated data, to reserve a public network, create a specific security
	// group e attached the NIC of this network, returning its instance ID.
	@Test
	public void testRequestInstance() throws FogbowException {
	    // set up
		Mockito.doReturn(this.instanceId).when(this.plugin).doRequestInstance(
				Mockito.any(Client.class), Mockito.any(PublicIpOrder.class), Mockito.any(CreateNetworkReserveRequest.class));

		// exercise
		this.plugin.requestInstance(this.publicIpOrder, this.cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.eq(this.cloudUser.getToken()));

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doRequestInstance(
				Mockito.eq(this.client), Mockito.eq(this.publicIpOrder), Mockito.any(CreateNetworkReserveRequest.class ));
	}

	// test case: when invoking doRequest instance with valid client, default network id and
	// create network reserve request, create a new ONe network reserve relative to the new order
	@Test
	public void testDoRequestInstance() throws InvalidParameterException, InstanceNotFoundException, UnauthorizedRequestException {
		// set up
        CreateNetworkReserveRequest request = Mockito.spy(this.createNetworkReserveRequest());

		Mockito.doReturn(ID_VALUE_ONE).when(this.plugin).convertToInteger(Mockito.anyString());
		Mockito.doReturn(STRING_ID_ONE).when(this.plugin).createSecurityGroup(
				Mockito.any(Client.class), Mockito.any(PublicIpOrder.class));
		Mockito.doNothing().when(this.plugin).addSecurityGroupToPublicIp(
				Mockito.any(Client.class), Mockito.anyString(), Mockito.anyString());
		Mockito.doNothing().when(this.plugin).attachPublicIpToCompute(
				Mockito.any(Client.class), Mockito.anyString(), Mockito.anyString());
		Mockito.when(OpenNebulaClientUtil.reserveVirtualNetwork(Mockito.any(Client.class), Mockito.anyInt(), Mockito.anyString()))
				.thenReturn(STRING_ID_ONE);

		// exercise
		this.plugin.doRequestInstance(this.client, this.publicIpOrder, request);

		// verify
        PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenNebulaClientUtil.reserveVirtualNetwork(Mockito.eq(this.client), Mockito.eq(ID_VALUE_ONE), Mockito.anyString());

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).convertToInteger(Mockito.anyString());
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).createSecurityGroup(
				Mockito.eq(this.client), Mockito.eq(this.publicIpOrder));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).addSecurityGroupToPublicIp(
				Mockito.eq(this.client), Mockito.eq(STRING_ID_ONE), Mockito.eq(STRING_ID_ONE));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).attachPublicIpToCompute(
				Mockito.eq(this.client), Mockito.eq(STRING_ID_ONE), Mockito.eq(this.publicIpOrder.getComputeOrderId()));
		Mockito.verify(request, Mockito.times(TestUtils.RUN_ONCE)).getVirtualNetworkReserved();
	}

	// test case: when invoking createSecurityGroup with valid client and public ip order,
	// the plugin should allocate the default fogbow security group in ONe and return its id
	@Test
	public void testCreateSecurityGroup() throws InvalidParameterException {
		// set up
		Mockito.when(OpenNebulaClientUtil.allocateSecurityGroup(Mockito.any(Client.class), Mockito.anyString()))
				.thenReturn(STRING_ID_ONE);

		// exercise
		this.plugin.createSecurityGroup(this.client, this.publicIpOrder);

		// verify
        PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenNebulaClientUtil.allocateSecurityGroup(Mockito.eq(this.client), Mockito.anyString());
	}

	// test case: when invoking addSecurityGroupToPublicIp with valid client, instance id and security group id,
	// the plugin should update the respective network reserve with the newly created ONe secgroup
	@Test
	public void testAddSecurityGroupToPublicIp() throws InvalidParameterException {
		// set up
		Mockito.when(OpenNebulaClientUtil.updateVirtualNetwork(Mockito.any(Client.class), Mockito.anyInt(), Mockito.anyString()))
				.thenReturn(STRING_ID_ONE);
		Mockito.doReturn(ID_VALUE_ONE).when(this.plugin).convertToInteger(Mockito.anyString());

		// exercise
		this.plugin.addSecurityGroupToPublicIp(this.client, this.instanceId, STRING_ID_ONE);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.updateVirtualNetwork(Mockito.eq(this.client), Mockito.eq(ID_VALUE_ONE), Mockito.anyString());

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).convertToInteger(Mockito.eq(this.instanceId));
	}

	// test case: when invoking attachPublicIpToCompute with valid client, public ip id and compute id,
	// the plugin should attach the respective ONe network reserve nic to the virtual machine
	@Test
	public void testAttachPublicIpToCompute() throws UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {
		// set up
		String template = this.getNicTemplate();
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		OneResponse response = Mockito.mock(OneResponse.class);

		Mockito.doReturn(template).when(this.plugin).createNicTemplate(Mockito.anyString());
		Mockito.when(OpenNebulaClientUtil.getVirtualMachine(Mockito.any(Client.class), Mockito.anyString()))
				.thenReturn(virtualMachine);
		Mockito.when(virtualMachine.nicAttach(Mockito.anyString())).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(false);

		// exercise
		this.plugin.attachPublicIpToCompute(this.client, this.instanceId, STRING_ID_ONE);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getVirtualMachine(Mockito.eq(this.client), Mockito.eq(STRING_ID_ONE));

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).createNicTemplate(Mockito.eq(this.instanceId));
		Mockito.verify(virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).nicAttach(Mockito.eq(template));
		Mockito.verify(response, Mockito.times(TestUtils.RUN_ONCE)).isError();
	}

	// test case: when invoking attachPublicIpToCompute with invalid parameters, the plugin
	// should throw an InvalidParameterException
	@Test
	public void testAttachPublicIpToComputeFail() throws UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {
		// set up
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		OneResponse response = Mockito.mock(OneResponse.class);
		String template = this.getNicTemplate();
		String message = String.format(Messages.Error.ERROR_WHILE_CREATING_NIC, template) + " " +
				String.format(Messages.Error.ERROR_MESSAGE, response.getMessage());

		Mockito.doReturn(template).when(this.plugin).createNicTemplate(Mockito.anyString());
		Mockito.when(OpenNebulaClientUtil.getVirtualMachine(Mockito.any(Client.class), Mockito.anyString()))
				.thenReturn(virtualMachine);
		Mockito.when(virtualMachine.nicAttach(Mockito.anyString())).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(true);

		// exercise
		try {
			this.plugin.attachPublicIpToCompute(this.client, this.instanceId, STRING_ID_ONE);
			Assert.fail();
		} catch (InvalidParameterException e) {
			Assert.assertEquals(message, e.getMessage());
		}

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getVirtualMachine(Mockito.eq(this.client), Mockito.eq(STRING_ID_ONE));

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).createNicTemplate(Mockito.eq(this.instanceId));
		Mockito.verify(virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).nicAttach(Mockito.eq(template));
		Mockito.verify(response, Mockito.times(TestUtils.RUN_ONCE)).isError();
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

		PublicIpOrder publicIpOrder = createPublicIpOrder();
		String virtualMachineId = publicIpOrder.getComputeId();
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		BDDMockito.given(OpenNebulaClientUtil.getVirtualMachine(Mockito.eq(client), Mockito.eq(virtualMachineId)))
				.willReturn(virtualMachine);
		Mockito.when(virtualMachine.xpath(OpenNebulaPuplicIpPlugin.EXPRESSION_IP_FROM_NETWORK))
				.thenReturn(FAKE_IP_ADDRESS);

		CloudUser cloudUser = createCloudUser();
		PublicIpInstance expected = createPublicIpInstance();

		// exercise
		PublicIpInstance instance = this.plugin.getInstance(publicIpOrder, cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getVirtualMachine(Mockito.eq(client), Mockito.eq(virtualMachineId));

		Mockito.verify(virtualMachine, Mockito.times(1))
				.xpath(String.format(OpenNebulaPuplicIpPlugin.EXPRESSION_IP_FROM_NETWORK, STRING_ID_ONE));

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

		PublicIpOrder publicIpOrder = createPublicIpOrder();
		String virtualMachineId = publicIpOrder.getComputeId();
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		BDDMockito.given(OpenNebulaClientUtil.getVirtualMachine(Mockito.eq(client), Mockito.eq(virtualMachineId)))
				.willReturn(virtualMachine);

		Mockito.when(virtualMachine
				.xpath(String.format(OpenNebulaPuplicIpPlugin.EXPRESSION_NIC_ID_FROM_NETWORK, STRING_ID_ONE)))
				.thenReturn(STRING_ID_ONE);
		
		Mockito.when(virtualMachine
				.xpath(String.format(OpenNebulaPuplicIpPlugin.EXPRESSION_SECURITY_GROUPS_FROM_NIC_ID, STRING_ID_ONE)))
				.thenReturn(STRING_SECURITY_GROUPS);

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

		CloudUser cloudUser = createCloudUser();

		// exercise
		this.plugin.deleteInstance(publicIpOrder, cloudUser);

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
		Mockito.verify(virtualMachine, Mockito.times(1))
				.xpath(String.format(OpenNebulaPuplicIpPlugin.EXPRESSION_NIC_ID_FROM_NETWORK, STRING_ID_ONE));
		Mockito.verify(virtualMachine, Mockito.times(1))
				.xpath(String.format(OpenNebulaPuplicIpPlugin.EXPRESSION_SECURITY_GROUPS_FROM_NIC_ID, STRING_ID_ONE));
		Mockito.verify(virtualMachine, Mockito.times(1)).nicDetach(ID_VALUE_ONE);
		Mockito.verify(securityGroup, Mockito.times(1)).delete();
		Mockito.verify(virtualNetwork, Mockito.times(1)).delete();
		Mockito.verify(response, Mockito.times(3)).isError();
	}
	
	// test case: When calling the deleteInstance method and unable to disconnect
	// the virtual machine, to detach the resource, it must return an
	// UnexpectedException.
	@Test(expected = UnexpectedException.class) // verify
	public void testDeleteInstanceThrowUnexpectedException() throws FogbowException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		PublicIpOrder publicIpOrder = createPublicIpOrder();
		String virtualMachineId = publicIpOrder.getComputeId();
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		BDDMockito.given(OpenNebulaClientUtil.getVirtualMachine(Mockito.eq(client), Mockito.eq(virtualMachineId)))
				.willReturn(virtualMachine);
		
		Mockito.when(virtualMachine
				.xpath(String.format(OpenNebulaPuplicIpPlugin.EXPRESSION_NIC_ID_FROM_NETWORK, STRING_ID_ONE)))
				.thenReturn(STRING_ID_ONE);
		
		Mockito.when(virtualMachine
				.xpath(String.format(OpenNebulaPuplicIpPlugin.EXPRESSION_SECURITY_GROUPS_FROM_NIC_ID, STRING_ID_ONE)))
				.thenReturn(STRING_SECURITY_GROUPS);
		
		Mockito.when(virtualMachine.stateStr()).thenReturn(FAIL_STATE);

		CloudUser cloudUser = createCloudUser();

		// exercise
		this.plugin.deleteInstance(publicIpOrder, cloudUser);
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
		this.plugin.doDeleteInstance(client, this.publicIpOrder);

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
		this.plugin.detachPublicIpFromCompute(virtualMachine, nicId);

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

	private String getSecurityGroupTemplate() {
		String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + 
				"<TEMPLATE>\n" + 
				"    <NAME>fogbow-sg-pip-fake-order-id</NAME>\n" +
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
				+ "    <NAME>fogbow-fake-instance-name</NAME>\n"
				+ "    <SIZE>1</SIZE>\n" 
				+ "</TEMPLATE>\n";
		
		return template;
	}
	
	private PublicIpInstance createPublicIpInstance() {
		String id = STRING_ID_ONE;
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
		return this.testUtils.createLocalPublicIpOrder(STRING_ID_ONE);
	}

	private CreateNetworkReserveRequest createNetworkReserveRequest() {
		return new CreateNetworkReserveRequest.Builder()
				.name(FAKE_NAME)
				.size(FAKE_SIZE)
				.build();
	}
}
