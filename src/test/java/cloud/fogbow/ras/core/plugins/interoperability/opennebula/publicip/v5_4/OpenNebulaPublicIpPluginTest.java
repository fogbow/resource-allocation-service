package cloud.fogbow.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

import java.util.UUID;

import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaBaseTests;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.network.v5_4.CreateNetworkReserveRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaStateMapper;

import static cloud.fogbow.ras.core.plugins.interoperability.opennebula.publicip.v5_4.OpenNebulaPuplicIpPlugin.*;

@PrepareForTest({DatabaseManager.class, OpenNebulaClientUtil.class, SecurityGroup.class, Thread.class, VirtualNetwork.class})
public class OpenNebulaPublicIpPluginTest extends OpenNebulaBaseTests {

	private static final String ACTIVE_STATE = "ACTIVE";
	private static final String EMPTY_STRING = "";
	private static final String FAKE_IP_ADDRESS = "10.1.0.100";
	private static final String FAKE_NAME = "fake-name";
	private static final String STRING_ID_ONE = "1";
	private static final String STRING_SECURITY_GROUPS = "0,1";

	private static final int ID_VALUE_ONE = 1;
	private static final int FAKE_SIZE = 1;

	private OpenNebulaPuplicIpPlugin plugin;
	private PublicIpOrder publicIpOrder;
	private String instanceId;
	private String computeId;
	private VirtualNetwork virtualNetwork;
	private VirtualMachine virtualMachine;
	private OneResponse response;

	@Before
	public void setUp() throws FogbowException {
	    super.setUp();

		this.plugin = Mockito.spy(new OpenNebulaPuplicIpPlugin(this.openNebulaConfFilePath));

		this.publicIpOrder = this.createPublicIpOrder();
		this.instanceId = this.publicIpOrder.getInstanceId();
		this.computeId = this.publicIpOrder.getComputeId();
		this.virtualNetwork = Mockito.mock(VirtualNetwork.class);
		this.virtualMachine = Mockito.mock(VirtualMachine.class);
		this.response = Mockito.mock(OneResponse.class);

		Mockito.when(OpenNebulaClientUtil.getVirtualNetwork(Mockito.any(Client.class), Mockito.anyString()))
				.thenReturn(this.virtualNetwork);
		Mockito.when(OpenNebulaClientUtil.getVirtualMachine(Mockito.any(Client.class), Mockito.anyString()))
				.thenReturn(this.virtualMachine);
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
		Mockito.doReturn(ID_VALUE_ONE).when(this.plugin).convertToInteger(Mockito.anyString());
		Mockito.doReturn(this.publicIpOrder.getInstanceId()).when(this.plugin).doRequestInstance(
				Mockito.any(Client.class), Mockito.anyInt(), Mockito.any(CreateNetworkReserveRequest.class));
		Mockito.doReturn(STRING_ID_ONE).when(this.plugin).createSecurityGroup(
				Mockito.any(Client.class), Mockito.any(PublicIpOrder.class));
		Mockito.doNothing().when(this.plugin).addSecurityGroupToPublicIp(
				Mockito.any(Client.class), Mockito.anyString(), Mockito.anyString());
		Mockito.doNothing().when(this.plugin).attachPublicIpToCompute(
				Mockito.any(Client.class), Mockito.anyString(), Mockito.anyString());

		// exercise
		this.plugin.requestInstance(this.publicIpOrder, this.cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.eq(this.cloudUser.getToken()));

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).convertToInteger(Mockito.anyString());
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doRequestInstance(
				Mockito.eq(this.client), Mockito.eq(ID_VALUE_ONE), Mockito.any(CreateNetworkReserveRequest.class ));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).createSecurityGroup(
				Mockito.eq(this.client), Mockito.eq(this.publicIpOrder));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).addSecurityGroupToPublicIp(
				Mockito.eq(this.client), Mockito.eq(this.instanceId), Mockito.eq(STRING_ID_ONE));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).attachPublicIpToCompute(
				Mockito.eq(this.client), Mockito.eq(this.instanceId), Mockito.eq(this.publicIpOrder.getComputeOrderId()));
	}

	// test case: when invoking doRequest instance with valid client, default network id and
	// create network reserve request, create a new ONe network reserve relative to the new order
	@Test
	public void testDoRequestInstance() throws InvalidParameterException {
		// set up
        CreateNetworkReserveRequest request = Mockito.spy(this.createNetworkReserveRequest());

		Mockito.when(OpenNebulaClientUtil.reserveVirtualNetwork(Mockito.any(Client.class), Mockito.anyInt(), Mockito.anyString()))
				.thenReturn(STRING_ID_ONE);

		// exercise
		this.plugin.doRequestInstance(this.client, ID_VALUE_ONE, request);

		// verify
        PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenNebulaClientUtil.reserveVirtualNetwork(Mockito.eq(this.client), Mockito.eq(ID_VALUE_ONE), Mockito.anyString());

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

		Mockito.doReturn(template).when(this.plugin).createNicTemplate(Mockito.anyString());
		Mockito.when(this.virtualMachine.nicAttach(Mockito.anyString())).thenReturn(this.response);
		Mockito.when(this.response.isError()).thenReturn(false);

		// exercise
		this.plugin.attachPublicIpToCompute(this.client, this.instanceId, this.computeId);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getVirtualMachine(Mockito.eq(this.client), Mockito.eq(this.computeId));

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).createNicTemplate(Mockito.eq(this.instanceId));
		Mockito.verify(this.virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).nicAttach(Mockito.eq(template));
		Mockito.verify(this.response, Mockito.times(TestUtils.RUN_ONCE)).isError();
	}

	// test case: when invoking attachPublicIpToCompute with invalid parameters, the plugin
	// should throw an InvalidParameterException
	@Test
	public void testAttachPublicIpToComputeFail() throws UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {
		// set up
		String template = this.getNicTemplate();
		String message = String.format(Messages.Error.ERROR_WHILE_CREATING_NIC, template) + " " +
				String.format(Messages.Error.ERROR_MESSAGE, this.response.getMessage());

		Mockito.doReturn(template).when(this.plugin).createNicTemplate(Mockito.anyString());
		Mockito.when(this.virtualMachine.nicAttach(Mockito.anyString())).thenReturn(this.response);
		Mockito.when(this.response.isError()).thenReturn(true);

		// exercise
		try {
			this.plugin.attachPublicIpToCompute(this.client, this.instanceId, this.computeId);
			Assert.fail();
		} catch (InvalidParameterException e) {
			Assert.assertEquals(message, e.getMessage());
		}

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getVirtualMachine(Mockito.eq(this.client), Mockito.eq(this.computeId));

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).createNicTemplate(Mockito.eq(this.instanceId));
		Mockito.verify(this.virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).nicAttach(Mockito.eq(template));
		Mockito.verify(this.response, Mockito.times(TestUtils.RUN_ONCE)).isError();
	}

	// test case: when invoking deleteInstance with valid order and cloud user, the plugin
	// should retrieve the respective ONe virtual machine, shut it off, detach its public ip nic,
	// delete the fogbow security group and finally delete the public ip instance network reserve
	@Test
	public void testDeleteInstance() throws FogbowException {
	    // set up
		Mockito.doReturn(this.response).when(this.virtualMachine).poweroff(Mockito.anyBoolean());
		Mockito.doReturn(this.response).when(this.virtualMachine).resume();
		Mockito.doReturn(true).when(this.plugin).isPowerOff(Mockito.any(VirtualMachine.class));
		Mockito.doNothing().when(this.plugin).detachPublicIpFromCompute(Mockito.any(VirtualMachine.class), Mockito.anyString());
		Mockito.doNothing().when(this.plugin).deleteSecurityGroup(Mockito.any(Client.class), Mockito.anyString());
		Mockito.doNothing().when(this.plugin).doDeleteInstance(Mockito.any(Client.class), Mockito.anyString());

		// exercise
		this.plugin.deleteInstance(this.publicIpOrder, this.cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.eq(this.cloudUser.getToken()));
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getVirtualMachine(Mockito.eq(this.client), Mockito.eq(this.publicIpOrder.getComputeId()));

		Mockito.verify(this.virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).poweroff(Mockito.eq(SHUT_OFF));
		Mockito.verify(this.virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).resume();
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).isPowerOff(Mockito.eq(this.virtualMachine));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).detachPublicIpFromCompute(
				Mockito.eq(this.virtualMachine), Mockito.eq(this.instanceId));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).deleteSecurityGroup(
				Mockito.eq(this.client), Mockito.eq(this.instanceId));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doDeleteInstance(
				Mockito.eq(this.client), Mockito.eq(this.instanceId));
	}

	// test case: when invoking deleteInstance with invalid parameters, the plugin should
	// throw an UnexpectedException
	@Test
	public void testDeleteInstanceFail() throws FogbowException {
		// set up
		String message = String.format(Messages.Error.ERROR_WHILE_REMOVING_RESOURCE, PUBLIC_IP_RESOURCE, this.instanceId);

		Mockito.doReturn(this.response).when(this.virtualMachine).poweroff(Mockito.anyBoolean());
		Mockito.doReturn(false).when(this.plugin).isPowerOff(Mockito.any(VirtualMachine.class));

		// exercise
        try	{
			this.plugin.deleteInstance(this.publicIpOrder, this.cloudUser);
			Assert.fail();
		} catch (UnexpectedException e) {
        	Assert.assertEquals(message, e.getMessage());
		}

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.eq(this.cloudUser.getToken()));
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getVirtualMachine(Mockito.eq(this.client), Mockito.eq(this.publicIpOrder.getComputeId()));

		Mockito.verify(this.virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).poweroff(Mockito.eq(SHUT_OFF));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).isPowerOff(Mockito.eq(this.virtualMachine));
	}

	// test case: when invoking detachPublicIpFromCompute with valid ONe virtual machine and
	// public ip instance id, the plugin should retrieve the respective nic and detach it
	// from the vm
	@Test
	public void testDetachPublicIpFromCompute() throws InvalidParameterException, UnexpectedException {
		// set up
	    Mockito.when(this.virtualMachine.xpath(Mockito.anyString())).thenReturn(STRING_ID_ONE);
		Mockito.when(this.virtualMachine.nicDetach(Mockito.anyInt())).thenReturn(this.response);
	    Mockito.when(this.response.isError()).thenReturn(false);
	    Mockito.doReturn(ID_VALUE_ONE).when(this.plugin).convertToInteger(Mockito.anyString());

	    // exercise
		this.plugin.detachPublicIpFromCompute(this.virtualMachine, this.instanceId);

		// verify
		Mockito.verify(this.virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).xpath(
				Mockito.eq(String.format(EXPRESSION_NIC_ID_FROM_NETWORK, this.instanceId)));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).convertToInteger(Mockito.eq(STRING_ID_ONE));
		Mockito.verify(this.virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).nicDetach(Mockito.eq(ID_VALUE_ONE));
		Mockito.verify(this.response, Mockito.times(TestUtils.RUN_ONCE)).isError();
	}

	// test case: when invoking detachPublicIpFromCompute with invalid parameters, the plugin
	// should throw and UnexpectedException
	@Test
	public void testDetachPublicIpFromComputeFail() throws InvalidParameterException {
		// set up
		String message = String.format(Messages.Error.ERROR_MESSAGE, STRING_ID_ONE);

		Mockito.when(this.virtualMachine.xpath(Mockito.anyString())).thenReturn(STRING_ID_ONE);
		Mockito.when(this.virtualMachine.nicDetach(Mockito.anyInt())).thenReturn(this.response);
		Mockito.when(this.response.isError()).thenReturn(true);
		Mockito.when(this.response.getErrorMessage()).thenReturn(STRING_ID_ONE);
		Mockito.doReturn(ID_VALUE_ONE).when(this.plugin).convertToInteger(Mockito.anyString());

		// exercise
		try {
			this.plugin.detachPublicIpFromCompute(this.virtualMachine, this.instanceId);
			Assert.fail();
		} catch (UnexpectedException e) {
			Assert.assertEquals(message, e.getMessage());
		}

		// verify
		Mockito.verify(this.virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).xpath(
				Mockito.eq(String.format(EXPRESSION_NIC_ID_FROM_NETWORK, this.instanceId)));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).convertToInteger(Mockito.eq(STRING_ID_ONE));
		Mockito.verify(this.virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).nicDetach(Mockito.eq(ID_VALUE_ONE));
		Mockito.verify(this.response, Mockito.times(TestUtils.RUN_ONCE)).isError();
		Mockito.verify(this.response, Mockito.times(TestUtils.RUN_ONCE)).getErrorMessage();
	}

	// test case: when invoking deleteSecurityGroup with valid client and public ip instance id,
	// the plugin should retrieve the respective fogbow security group and delete it
	@Test
	public void testDeleteSecurityGroup() throws UnauthorizedRequestException, InstanceNotFoundException,
			InvalidParameterException, UnexpectedException {
		// set up
		SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);

		Mockito.doReturn(securityGroup).when(this.plugin).getSecurityGroupForPublicIpNetwork(
				Mockito.any(Client.class), Mockito.anyString());
		Mockito.when(securityGroup.delete()).thenReturn(this.response);
		Mockito.when(this.response.isError()).thenReturn(false);

		// exercise
		this.plugin.deleteSecurityGroup(this.client, this.instanceId);

		// verify
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getSecurityGroupForPublicIpNetwork(
				Mockito.eq(this.client), Mockito.eq(this.instanceId));
		Mockito.verify(securityGroup, Mockito.times(TestUtils.RUN_ONCE)).delete();
		Mockito.verify(this.response, Mockito.times(TestUtils.RUN_ONCE)).isError();
	}

	// test case: when invoking deleteSecurityGroup and the respective secgroup is not found, the plugin
	// should throw an UnexpectedException
	@Test(expected = UnexpectedException.class)
	public void testDeleteSecurityGroupNull() throws UnauthorizedRequestException, InstanceNotFoundException,
			InvalidParameterException, UnexpectedException {
		// set up
		Mockito.doReturn(null).when(this.plugin).getSecurityGroupForPublicIpNetwork(
				Mockito.any(Client.class), Mockito.anyString());

		// exercise
        this.plugin.deleteSecurityGroup(this.client, this.instanceId);
	}

	// test case: when invoking deleteSecurityGroup with invalid parameters, the plugin should
	// throw an UnexpectedException
	@Test
	public void testDeleteSecurityGroupFail() throws UnauthorizedRequestException, InstanceNotFoundException,
			InvalidParameterException {
		// set up
		String message = String.format(Messages.Error.ERROR_MESSAGE, STRING_ID_ONE);
		SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);

		Mockito.doReturn(securityGroup).when(this.plugin).getSecurityGroupForPublicIpNetwork(
				Mockito.any(Client.class), Mockito.anyString());
		Mockito.when(securityGroup.delete()).thenReturn(this.response);
		Mockito.when(this.response.isError()).thenReturn(true);
		Mockito.when(this.response.getErrorMessage()).thenReturn(STRING_ID_ONE);

		// exercise
		try {
			this.plugin.deleteSecurityGroup(this.client, this.instanceId);
			Assert.fail();
		} catch (UnexpectedException e) {
			Assert.assertEquals(message, e.getMessage());
		}

		// verify
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getSecurityGroupForPublicIpNetwork(
				Mockito.eq(this.client), Mockito.eq(this.instanceId));
		Mockito.verify(securityGroup, Mockito.times(TestUtils.RUN_ONCE)).delete();
		Mockito.verify(this.response, Mockito.times(TestUtils.RUN_ONCE)).isError();
		Mockito.verify(this.response, Mockito.times(TestUtils.RUN_ONCE)).getErrorMessage();
	}

	// test case: when invoking getSecurityGroupForPublicnetwork with valid client and public ip id
	// the plugin should retrieve the respective fogbow security group
	@Test
	public void testGetSecurityGroupForPublicIpNetwork() throws UnauthorizedRequestException, InstanceNotFoundException,
			InvalidParameterException {
		// set up
		String secGroupName = SystemConstants.PIP_SECURITY_GROUP_PREFIX + this.instanceId;
		SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);

		Mockito.when(OpenNebulaClientUtil.getSecurityGroup(Mockito.any(Client.class), Mockito.anyString()))
				.thenReturn(securityGroup);
		Mockito.when(this.virtualNetwork.xpath(Mockito.anyString())).thenReturn(STRING_SECURITY_GROUPS);
		Mockito.when(securityGroup.getName()).thenReturn(secGroupName);

		// exercise
		SecurityGroup secGroup = this.plugin.getSecurityGroupForPublicIpNetwork(this.client, this.instanceId);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getVirtualNetwork(Mockito.eq(this.client), Mockito.eq(this.instanceId));
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getSecurityGroup(Mockito.eq(this.client), Mockito.anyString());

		Mockito.verify(this.virtualNetwork, Mockito.times(TestUtils.RUN_ONCE)).xpath(Mockito.eq(VNET_TEMPLATE_SECURITY_GROUPS_PATH));
		Mockito.verify(securityGroup, Mockito.times(TestUtils.RUN_ONCE)).getName();
		Assert.assertNotNull(secGroup);
	}

	// test case: when invoking getSecurityGroupForPublicIpNetwork and the fogbow secgroup is not found
	// the plugin should return null
	@Test
	public void testGetSecurityGroupForPublicIpNetworkNull() throws UnauthorizedRequestException, InstanceNotFoundException,
			InvalidParameterException {
		// set up
		Mockito.when(this.virtualNetwork.xpath(Mockito.anyString())).thenReturn(null);

		// exercise
		SecurityGroup secGroup = this.plugin.getSecurityGroupForPublicIpNetwork(this.client, this.instanceId);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getVirtualNetwork(Mockito.eq(this.client), Mockito.eq(this.instanceId));

		Mockito.verify(this.virtualNetwork, Mockito.times(TestUtils.RUN_ONCE)).xpath(Mockito.eq(VNET_TEMPLATE_SECURITY_GROUPS_PATH));
		Assert.assertNull(secGroup);
	}

	// test case: when invoking doDeleteInstance the plugin should retrieve the respective ONe
	// virtual network and delete it
	@Test
	public void testDoDeleteInstance() throws UnexpectedException, InstanceNotFoundException, InvalidParameterException,
			UnauthorizedRequestException {
		// set up
		Mockito.when(this.virtualNetwork.delete()).thenReturn(this.response);
		Mockito.when(this.response.isError()).thenReturn(false);

		// exercise
        this.plugin.doDeleteInstance(this.client, this.instanceId);

        // verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getVirtualNetwork(Mockito.eq(this.client), Mockito.eq(this.instanceId));

		Mockito.verify(this.virtualNetwork, Mockito.times(TestUtils.RUN_ONCE)).delete();
		Mockito.verify(this.response, Mockito.times(TestUtils.RUN_ONCE)).isError();
	}

	// test case: when invoking doDeleteInstance with invalid parameters the plugin should
	// throw an UnexpectedException
	@Test
	public void testDoDeleteInstanceFail() throws InstanceNotFoundException, InvalidParameterException,
			UnauthorizedRequestException {
		// set up
		Mockito.when(this.virtualNetwork.delete()).thenReturn(this.response);
		Mockito.when(this.response.isError()).thenReturn(true);
		Mockito.when(this.response.getErrorMessage()).thenReturn(STRING_ID_ONE);

		// exercise
        try {
			this.plugin.doDeleteInstance(this.client, this.instanceId);
			Assert.fail();
		} catch (UnexpectedException e) {
        	Assert.assertEquals(STRING_ID_ONE, e.getMessage());
		}

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getVirtualNetwork(Mockito.eq(this.client), Mockito.eq(this.instanceId));

		Mockito.verify(this.virtualNetwork, Mockito.times(TestUtils.RUN_ONCE)).delete();
		Mockito.verify(this.response, Mockito.times(TestUtils.RUN_ONCE)).isError();
		Mockito.verify(this.response, Mockito.times(TestUtils.RUN_ONCE)).getErrorMessage();
	}

	// test case: when invoking getInstance with valid public ip order and cloud user, the plugin
	// should instantiate the respective fogbow public ip instance and return it
	@Test
	public void testGetInstance() throws FogbowException {
		// set up
		Mockito.doReturn(FAKE_IP_ADDRESS).when(this.plugin).doGetInstance(
				Mockito.any(Client.class), Mockito.anyString(), Mockito.anyString());

		// exercise
		PublicIpInstance publicIpInstance = this.plugin.getInstance(this.publicIpOrder, this.cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.eq(this.cloudUser.getToken()));

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetInstance(
				Mockito.eq(this.client), Mockito.eq(this.instanceId), Mockito.eq(this.publicIpOrder.getComputeId()));
		Assert.assertEquals(FAKE_IP_ADDRESS, publicIpInstance.getIp());
	}

	// test case: when invoking doGetInstance with valid client, public ip instance, and compute id
	// the plugin should return the respective public ip address
	@Test
	public void testDoGetInstance() throws UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {
		// set up
		String publicIpPath = String.format(EXPRESSION_IP_FROM_NETWORK, this.instanceId);

		Mockito.when(this.virtualMachine.xpath(Mockito.anyString())).thenReturn(FAKE_IP_ADDRESS);

		// exercise
		this.plugin.doGetInstance(this.client, this.instanceId, this.computeId);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getVirtualMachine(Mockito.eq(this.client), Mockito.eq(this.computeId));

		Mockito.verify(this.virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).xpath(publicIpPath);
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

	private String getNicTemplate() {
		String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
				+ "<TEMPLATE>\n"
				+ "    <NIC>\n"
				+ "        <NETWORK_ID>1</NETWORK_ID>\n"
				+ "    </NIC>\n"
				+ "</TEMPLATE>\n";
		
		return template;
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
