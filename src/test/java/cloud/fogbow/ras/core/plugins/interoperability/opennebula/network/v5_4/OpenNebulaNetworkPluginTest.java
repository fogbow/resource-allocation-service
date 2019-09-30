package cloud.fogbow.ras.core.plugins.interoperability.opennebula.network.v5_4;

import java.io.File;
import java.util.UUID;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaBaseTests;
import org.apache.commons.net.util.SubnetUtils;
import org.h2.security.Fog;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.secgroup.SecurityGroup;
import org.opennebula.client.vnet.VirtualNetwork;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.NetworkAllocationMode;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;

import static cloud.fogbow.ras.core.plugins.interoperability.opennebula.network.v5_4.OpenNebulaNetworkPlugin.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({OpenNebulaClientUtil.class, SecurityGroup.class, UUID.class, VirtualNetwork.class, DatabaseManager.class})
public class OpenNebulaNetworkPluginTest extends OpenNebulaBaseTests {

	private static final String EMPTY_STRING = "";
	private static final String FAKE_ADDRESS = "10.1.0.0";
	private static final String FIRST_ADDRESS = "10.1.0.1";
	private static final String FAKE_CIDR_ADDRESS = "10.1.0.0/24";
	private static final String FAKE_INSTANCE_ID = "fake-instance-id";
	private static final String FAKE_NETWORK_NAME = "fake-network-name";
	private static final String FAKE_SIZE = "256";
	private static final String FAKE_USER_ID = "fake-user-id";
	private static final String FAKE_VLAN_ID = "fake-vlan-id";
	private static final String ID_VALUE_ONE = "1";
	private static final String ID_VALUE_ZERO = "0";
	private static final String LOCAL_TOKEN_VALUE = "user:password";
	private static final String FAKE_ORDER_ID = "fake-order-id";

	private static final int MAXIMUM_INTEGER_VALUE = 2147483647;
	private static final int NEGATIVE_SIZE_VALUE = -1;
	private static final int ZERO_VALUE = 0;

	private OpenNebulaNetworkPlugin plugin;
	private VirtualNetwork virtualNetwork;
	private NetworkOrder networkOrder;

	@Before
	public void setUp() throws FogbowException {
	    super.setUp();

		this.plugin = Mockito.spy(new OpenNebulaNetworkPlugin(this.openNebulaConfFilePath));
		this.virtualNetwork = Mockito.mock(VirtualNetwork.class);
		this.networkOrder = Mockito.spy(this.createNetworkOrder());

		Mockito.when(OpenNebulaClientUtil.getVirtualNetwork(Mockito.any(Client.class), Mockito.anyString()))
				.thenReturn(this.virtualNetwork);
	}

	@Test
	public void testRequestInstance() throws FogbowException {
	    // set up
		SubnetUtils.SubnetInfo subnetInfo = new SubnetUtils(FAKE_CIDR_ADDRESS).getInfo();
		String firstAddress = subnetInfo.getLowAddress();
		int size = subnetInfo.getAddressCount();

		Mockito.doReturn(ZERO_VALUE).when(this.plugin).getAddressRangeIndex(
				Mockito.any(VirtualNetwork.class), Mockito.anyString(), Mockito.anyInt());
		Mockito.doReturn(ID_VALUE_ZERO).when(this.plugin).getAddressRangeId(
				Mockito.any(VirtualNetwork.class), Mockito.anyInt(), Mockito.anyString());
		Mockito.doReturn(FAKE_ADDRESS).when(this.plugin).getNextAvailableAddress(
				Mockito.any(VirtualNetwork.class), Mockito.anyInt());
		Mockito.doReturn(this.networkOrder.getInstanceId()).when(this.plugin).doRequestInstance(
				Mockito.any(Client.class), Mockito.anyString(), Mockito.any(CreateNetworkReserveRequest.class));

		// exercise
		this.plugin.requestInstance(this.networkOrder, this.cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class);
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.eq(this.cloudUser.getToken()));
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class);
		OpenNebulaClientUtil.getVirtualNetwork(Mockito.any(Client.class), Mockito.anyString());

		Mockito.verify(this.networkOrder, Mockito.times(TestUtils.RUN_ONCE)).getCidr();
		Mockito.verify(this.networkOrder, Mockito.times(TestUtils.RUN_ONCE)).getName();
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getAddressRangeIndex(
				Mockito.eq(this.virtualNetwork), Mockito.eq(firstAddress), Mockito.eq(size));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getAddressRangeId(
				Mockito.eq(this.virtualNetwork), Mockito.eq(ZERO_VALUE), Mockito.eq(FAKE_CIDR_ADDRESS));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getNextAvailableAddress(
				Mockito.eq(this.virtualNetwork), Mockito.eq(ZERO_VALUE));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doRequestInstance(
				Mockito.any(Client.class), Mockito.anyString(), Mockito.any(CreateNetworkReserveRequest.class));
	}

	@Test
	public void testGetAddressRangeIndex() throws InvalidParameterException {
		// set up
		Integer fakeIndex = 1;

	    Mockito.when(this.virtualNetwork.xpath(Mockito.eq(String.format(ADDRESS_RANGE_IP_PATH_FORMAT, fakeIndex))))
				.thenReturn(FAKE_ADDRESS)
				.thenReturn(EMPTY_STRING);
		Mockito.when(this.virtualNetwork.xpath(Mockito.eq(String.format(ADDRESS_RANGE_SIZE_PATH_FORMAT, fakeIndex))))
				.thenReturn("10");
		Mockito.when(this.virtualNetwork.xpath(Mockito.eq(String.format(ADDRESS_RANGE_USED_LEASES_PATH_FORMAT, fakeIndex))))
				.thenReturn(ID_VALUE_ZERO);

		// exercise
		Integer index = this.plugin.getAddressRangeIndex(this.virtualNetwork, FIRST_ADDRESS, fakeIndex);
		Integer nullIndex = this.plugin.getAddressRangeIndex(this.virtualNetwork, FIRST_ADDRESS, fakeIndex);

		// verifiy
		Mockito.verify(this.virtualNetwork, Mockito.times(6)).xpath(Mockito.anyString());
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).convertToInteger(Mockito.anyString());
		Assert.assertEquals(fakeIndex, index);
		Assert.assertEquals(null, nullIndex);
	}

	@Test
	public void testGetAddressRangeId() throws NoAvailableResourcesException {
		// set up
		Integer fakeIndex = 1;

		Mockito.when(this.virtualNetwork.xpath(Mockito.eq(String.format(ADDRESS_RANGE_ID_PATH_FORMAT, fakeIndex))))
				.thenReturn(String.valueOf(fakeIndex));

		// exercise
		String index = this.plugin.getAddressRangeId(this.virtualNetwork, fakeIndex, FAKE_CIDR_ADDRESS);

		// verify
		Mockito.verify(this.virtualNetwork, Mockito.times(TestUtils.RUN_ONCE)).xpath(Mockito.anyString());
		Assert.assertEquals(String.valueOf(fakeIndex), index);
	}

	@Test
	public void testGetAddressRangeIdFail() {
	    // set up
		String expectedMessage = String.format(Messages.Exception.UNABLE_TO_CREATE_NETWORK_RESERVE, FAKE_CIDR_ADDRESS);

		try {
			// exercise
			String index = this.plugin.getAddressRangeId(this.virtualNetwork, null, FAKE_CIDR_ADDRESS);
			Assert.fail();
		} catch (NoAvailableResourcesException e) {
		    // verify
            Assert.assertEquals(expectedMessage, e.getMessage());
		}
	}

	@Test
	public void testGetNextAvailableAddress() throws InvalidParameterException {
		// set up
		Integer fakeIndex = 1;

		Mockito.when(this.virtualNetwork.xpath(Mockito.eq(String.format(ADDRESS_RANGE_IP_PATH_FORMAT, fakeIndex))))
				.thenReturn(FAKE_ADDRESS);
		Mockito.when(this.virtualNetwork.xpath(Mockito.eq(String.format(ADDRESS_RANGE_SIZE_PATH_FORMAT, fakeIndex))))
				.thenReturn("10");
		Mockito.when(this.virtualNetwork.xpath(Mockito.eq(String.format(ADDRESS_RANGE_USED_LEASES_PATH_FORMAT, fakeIndex))))
				.thenReturn(String.valueOf(fakeIndex))
				.thenReturn(ID_VALUE_ZERO);

		// exercise
		this.plugin.getNextAvailableAddress(this.virtualNetwork, fakeIndex);
		String defaultFistIp = this.plugin.getNextAvailableAddress(this.virtualNetwork, fakeIndex);

		// verify
		Mockito.verify(this.virtualNetwork, Mockito.times(6)).xpath(Mockito.anyString());
		Assert.assertEquals(FAKE_ADDRESS, defaultFistIp);
	}

	// test case: When you call the createSecurityGroup method with a valid client,
	// virtual network ID, and network request, it must create a security group for
	// this network by returning its ID.
	@Test
	public void testCreateSecurityGroupSuccessfully() throws UnexpectedException, InvalidParameterException,
			UnauthorizedRequestException, InstanceNotFoundException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		String virtualNetworkId = ID_VALUE_ONE;
		VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
		BDDMockito.given(OpenNebulaClientUtil.getVirtualNetwork(Mockito.eq(client), Mockito.eq(virtualNetworkId)))
				.willReturn(virtualNetwork);

		NetworkOrder networkOrder = Mockito.spy(new NetworkOrder());
		networkOrder.setId(FAKE_ORDER_ID);
		String securityGroupName = SystemConstants.PN_SECURITY_GROUP_PREFIX + FAKE_ORDER_ID;
		String securityGroupTemplate = getSecurityGroupTemplate(securityGroupName);

		String securityGroupID = ID_VALUE_ONE;
		BDDMockito.given(
				OpenNebulaClientUtil.allocateSecurityGroup(Mockito.eq(client), Mockito.eq(securityGroupTemplate)))
				.willReturn(securityGroupID);

		// exercise
		this.plugin.createSecurityGroup(client, FAKE_ORDER_ID, networkOrder.getId());

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getVirtualNetwork(Mockito.eq(client), Mockito.eq(virtualNetworkId));

		Mockito.verify(virtualNetwork, Mockito.times(1))
				.xpath(Mockito.eq(OpenNebulaNetworkPlugin.VNET_ADDRESS_RANGE_IP_PATH));

		Mockito.verify(virtualNetwork, Mockito.times(1))
				.xpath(Mockito.eq(OpenNebulaNetworkPlugin.VNET_ADDRESS_RANGE_SIZE_PATH));
		
		Mockito.verify(this.plugin, Mockito.times(1)).generateSecurityGroupName(Mockito.eq(networkOrder.getId()));

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.allocateSecurityGroup(Mockito.eq(client), Mockito.eq(securityGroupTemplate));
	}
	
	// test case: When calling the getSecurityGroupBy method, with valid security
	// groups associated with the virtual network passed by parameter, it must
	// return the ID of the security group created together with this Virtual
	// Network.
	@Test
	@Ignore
	public void testGetSecurityGroupByVirtualNetworkSuccessfully()
			throws UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {

		// set up
		VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		PowerMockito.when(OpenNebulaClientUtil.getVirtualNetwork(Mockito.any(Client.class), Mockito.anyString()))
				.thenReturn(virtualNetwork);

		String securityGroups = ID_VALUE_ZERO + OpenNebulaNetworkPlugin.SECURITY_GROUPS_SEPARATOR + ID_VALUE_ONE;
		Mockito.when(virtualNetwork.xpath(Mockito.anyString())).thenReturn(securityGroups);

		String expected = ID_VALUE_ONE;

		// exercise
		//String securityGroupId = this.plugin.getSecurityGroups(virtualNetwork)[0];

		// verify
		Mockito.verify(virtualNetwork, Mockito.times(1))
				.xpath(Mockito.eq(OpenNebulaNetworkPlugin.VNET_TEMPLATE_SECURITY_GROUPS_PATH));

		//Assert.assertEquals(expected, securityGroupId);
	}
	
	// test case: When calling the getSecurityGroupBy method, with an empty security
	// group, it must return a null security group ID associated with the virtual
	// network passed by parameter.
	@Test
    @Ignore
	public void testGetSecurityGroupByVirtualNetworkWithSecurityGroupsEmpty()
			throws UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {

		// set up
		VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		PowerMockito.when(OpenNebulaClientUtil.getVirtualNetwork(Mockito.any(Client.class), Mockito.anyString()))
				.thenReturn(virtualNetwork);

		String securityGroups = EMPTY_STRING;
		Mockito.when(virtualNetwork.xpath(Mockito.anyString())).thenReturn(securityGroups);

		// exercise
		//String securityGroupId = this.plugin.getSecurityGroups(virtualNetwork)[0];

		// verify
		Mockito.verify(virtualNetwork, Mockito.times(1))
				.xpath(Mockito.eq(OpenNebulaNetworkPlugin.VNET_TEMPLATE_SECURITY_GROUPS_PATH));

		//Assert.assertNull(securityGroupId);
	}
	
	// test case: When calling the requestInstance method, with a valid client and
	// an order without a network name, a template must be generated with a default
	// network name and the other associated data, to reserve a network, returning
	// its instance ID.
	@Test
	public void testRequestInstanceSuccessfullyWithDefaultNetworkName() throws FogbowException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		int defaultNetworkID = Integer.parseInt(ID_VALUE_ZERO);
		String networkName = null;
		NetworkOrder networkOrder = createNetworkOrder();

		networkName = SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + FAKE_NETWORK_NAME;
		String networkReserveTemplate = getNetworkReserveTemplate(networkName);
		Mockito.doReturn(FAKE_NETWORK_NAME).when(this.plugin).getRandomUUID();

		BDDMockito.given(OpenNebulaClientUtil.reserveVirtualNetwork(Mockito.eq(client), Mockito.eq(defaultNetworkID),
				Mockito.eq(networkReserveTemplate))).willReturn(ID_VALUE_ONE);

		VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
		BDDMockito.given(OpenNebulaClientUtil.getVirtualNetwork(Mockito.eq(client), Mockito.eq(ID_VALUE_ONE)))
				.willReturn(virtualNetwork);

		Mockito.doReturn(ID_VALUE_ONE).when(this.plugin).createSecurityGroup(Mockito.any(Client.class),
				Mockito.anyString(), Mockito.eq(networkOrder.getId()));

		int virtualNetworkID = Integer.parseInt(ID_VALUE_ONE);
		String networkUpdateTemplate = getNetworkUpdateTemplate();
		BDDMockito.given(OpenNebulaClientUtil.updateVirtualNetwork(Mockito.eq(client), Mockito.eq(virtualNetworkID),
				Mockito.eq(networkUpdateTemplate))).willReturn(ID_VALUE_ONE);

		CloudUser cloudUser = createCloudUser();

		// exercise
		this.plugin.requestInstance(networkOrder, cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.reserveVirtualNetwork(Mockito.eq(client), Mockito.eq(defaultNetworkID),
				Mockito.eq(networkReserveTemplate));

		Mockito.verify(this.plugin, Mockito.times(1)).createSecurityGroup(Mockito.any(Client.class),
				Mockito.anyString(), Mockito.eq(networkOrder.getId()));

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.updateVirtualNetwork(Mockito.eq(client), Mockito.eq(virtualNetworkID),
				Mockito.eq(networkUpdateTemplate));
	}
	
	// test case: When calling the requestInstance method, with a valid client and
	// an order with a network name, a template must be generated with the
	// associated data, to reserve a network, returning its instance ID.
	@Test
	public void testRequestInstanceSuccessfullyWithoutDefaultNetworkName() throws FogbowException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		int defaultNetworkID = Integer.parseInt(ID_VALUE_ZERO);
		String networkName = FAKE_NETWORK_NAME;
		NetworkOrder networkOrder = createNetworkOrder();
		String networkReserveTemplate = getNetworkReserveTemplate(networkName);

		BDDMockito.given(OpenNebulaClientUtil.reserveVirtualNetwork(Mockito.eq(client), Mockito.eq(defaultNetworkID),
				Mockito.eq(networkReserveTemplate))).willReturn(ID_VALUE_ONE);

		VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
		BDDMockito.given(OpenNebulaClientUtil.getVirtualNetwork(Mockito.eq(client), Mockito.eq(ID_VALUE_ONE)))
				.willReturn(virtualNetwork);

		Mockito.doReturn(ID_VALUE_ONE).when(this.plugin).createSecurityGroup(Mockito.any(Client.class),
				Mockito.anyString(), Mockito.eq(networkOrder.getId()));

		int virtualNetworkID = Integer.parseInt(ID_VALUE_ONE);
		String networkUpdateTemplate = getNetworkUpdateTemplate();
		BDDMockito.given(OpenNebulaClientUtil.updateVirtualNetwork(Mockito.eq(client), Mockito.eq(virtualNetworkID),
				Mockito.eq(networkUpdateTemplate))).willReturn(ID_VALUE_ONE);

		CloudUser cloudUser = createCloudUser();

		// exercise
		this.plugin.requestInstance(networkOrder, cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.reserveVirtualNetwork(Mockito.eq(client), Mockito.eq(defaultNetworkID),
				Mockito.eq(networkReserveTemplate));

		Mockito.verify(this.plugin, Mockito.times(1)).createSecurityGroup(Mockito.any(Client.class),
				Mockito.anyString(), Mockito.eq(networkOrder.getId()));

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.updateVirtualNetwork(Mockito.eq(client), Mockito.eq(virtualNetworkID),
				Mockito.eq(networkUpdateTemplate));
	}
	
	// test case: When calling the deleteInstance method, if the removal call is not
	// answered an error response is returned.
	@Test
	public void testDeleteInstanceUnsuccessfully() throws FogbowException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
		PowerMockito.mockStatic(VirtualNetwork.class);
		BDDMockito.given(OpenNebulaClientUtil.getVirtualNetwork(Mockito.eq(client), Mockito.anyString()))
				.willReturn(virtualNetwork);

		String securityGroups = ID_VALUE_ZERO + OpenNebulaNetworkPlugin.SECURITY_GROUPS_SEPARATOR + ID_VALUE_ONE;
		Mockito.when(virtualNetwork.xpath(OpenNebulaNetworkPlugin.VNET_TEMPLATE_SECURITY_GROUPS_PATH))
				.thenReturn(securityGroups);

		SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);
		PowerMockito.mockStatic(SecurityGroup.class);
		BDDMockito.given(OpenNebulaClientUtil.getSecurityGroup(Mockito.eq(client), Mockito.anyString()))
				.willReturn(securityGroup);

		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.when(securityGroup.delete()).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(true);

		Mockito.when(virtualNetwork.delete()).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(true);

		CloudUser cloudUser = createCloudUser();
		String instanceId = ID_VALUE_ONE;

		NetworkOrder networkOrder = new NetworkOrder();
		networkOrder.setInstanceId(instanceId);

		// exercise
		this.plugin.deleteInstance(networkOrder, cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getVirtualNetwork(Mockito.eq(client), Mockito.anyString());

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getSecurityGroup(Mockito.eq(client), Mockito.anyString());

		Mockito.verify(virtualNetwork, Mockito.times(1)).delete();
		Mockito.verify(response, Mockito.times(2)).isError();
	}

	// test case: When calling the deleteInstance method, with the instance ID and
	// token valid, the instance of virtual network will be removed.
	@Test
	public void testDeleteInstanceSuccessfully() throws FogbowException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
		PowerMockito.mockStatic(VirtualNetwork.class);
		BDDMockito.given(OpenNebulaClientUtil.getVirtualNetwork(Mockito.eq(client), Mockito.anyString()))
				.willReturn(virtualNetwork);

		String securityGroups = ID_VALUE_ZERO + OpenNebulaNetworkPlugin.SECURITY_GROUPS_SEPARATOR + ID_VALUE_ONE;
		Mockito.when(virtualNetwork.xpath(OpenNebulaNetworkPlugin.VNET_TEMPLATE_SECURITY_GROUPS_PATH))
				.thenReturn(securityGroups);

		SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);
		PowerMockito.mockStatic(SecurityGroup.class);
		BDDMockito.given(OpenNebulaClientUtil.getSecurityGroup(Mockito.eq(client), Mockito.anyString()))
				.willReturn(securityGroup);

		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.when(securityGroup.delete()).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(false);

		Mockito.when(virtualNetwork.delete()).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(false);

		CloudUser cloudUser = createCloudUser();
		String instanceId = ID_VALUE_ONE;

		NetworkOrder networkOrder = new NetworkOrder();
		networkOrder.setInstanceId(instanceId);

		// exercise
		this.plugin.deleteInstance(networkOrder, cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getVirtualNetwork(Mockito.eq(client), Mockito.anyString());

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getSecurityGroup(Mockito.eq(client), Mockito.anyString());

		Mockito.verify(virtualNetwork, Mockito.times(1)).delete();
		Mockito.verify(response, Mockito.times(2)).isError();

		Mockito.verify(virtualNetwork, Mockito.times(1)).delete();
		Mockito.verify(response, Mockito.times(2)).isError();
	}

	// test case: When calling the getInstance method, with a valid client from a
	// token value and instance ID, it must returned a instance of a virtual
	// network.
	@Test
	public void testGetInstanceSuccessfully() throws FogbowException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
		PowerMockito.mockStatic(VirtualNetwork.class);
		BDDMockito.given(OpenNebulaClientUtil.getVirtualNetwork(Mockito.eq(client), Mockito.anyString()))
				.willReturn(virtualNetwork);

		Mockito.doReturn(FAKE_INSTANCE_ID).when(virtualNetwork).getId();
		Mockito.doReturn(FAKE_NETWORK_NAME).when(virtualNetwork).getName();
		Mockito.doReturn(FAKE_VLAN_ID).when(virtualNetwork)
				.xpath(Mockito.eq(OpenNebulaNetworkPlugin.VNET_TEMPLATE_VLAN_ID_PATH));

		Mockito.doReturn(FAKE_ADDRESS).when(virtualNetwork)
				.xpath(Mockito.eq(OpenNebulaNetworkPlugin.VNET_ADDRESS_RANGE_IP_PATH));

		Mockito.doReturn(FAKE_SIZE).when(virtualNetwork)
				.xpath(Mockito.eq(OpenNebulaNetworkPlugin.VNET_ADDRESS_RANGE_SIZE_PATH));

		Mockito.doReturn(FAKE_CIDR_ADDRESS).when(this.plugin).generateAddressCidr(FAKE_ADDRESS, FAKE_SIZE);

		CloudUser cloudUser = createCloudUser();
		String instanceId = FAKE_INSTANCE_ID;

		NetworkOrder networkOrder = new NetworkOrder();
		networkOrder.setInstanceId(instanceId);

		// exercise
		this.plugin.getInstance(networkOrder, cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getVirtualNetwork(Mockito.eq(client), Mockito.anyString());

		Mockito.verify(virtualNetwork, Mockito.times(1)).getId();
		Mockito.verify(virtualNetwork, Mockito.times(1)).getName();
		Mockito.verify(virtualNetwork, Mockito.times(3)).xpath(Mockito.anyString());
	}
	
	// test case: When calling the getAddressRangeSize method, an invalid CIDR, it
	// must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class) // verify
	public void testGetAddressRangeSizeThrowInvalidParameterException() throws InvalidParameterException {
		// set up
		String cidr = null;

		// exercise
		this.plugin.getAddressRangeSize(cidr);
	}
	
	// test case: When calling the calculateCIDR method with a negative size value,
	// this will return the maximum CIDR value of an IPV4 network mask.
	@Test
	public void testCalculateCIDRWithNegativeSize() {
		// set up
		int size = NEGATIVE_SIZE_VALUE;
		int expected = OpenNebulaNetworkPlugin.IPV4_AMOUNT_BITS;

		// exercise
		int value = this.plugin.calculateCidr(size);

		// verify
		Assert.assertEquals(expected, value);
	}
		
	// test case: When calling the calculateCIDR method with a size max of an
	// integer value, this will return a value zero.
	@Test
	public void testCalculateCIDRWithSizeMaxOfIntegerValue() {
		// set up
		int size = MAXIMUM_INTEGER_VALUE;
		int expected = ZERO_VALUE;

		// exercise
		int value = this.plugin.calculateCidr(size);

		// verify
		Assert.assertEquals(expected, value);
	}
		
	// test case: When calling the generateAddressCIDR method with a valid address
	// and size, it must return an address in CIDR format.
	@Test
	public void testGenerateAddressCIDRSuccessfully() throws InvalidParameterException {
		// set up
		String address = FAKE_ADDRESS;
		String size = FAKE_SIZE;
		String expected = FAKE_CIDR_ADDRESS;

		// exercise
		String cidr = this.plugin.generateAddressCidr(address, size);
		// verify
		Assert.assertEquals(expected, cidr);
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

	private NetworkOrder createNetworkOrder() {
		String providingMember = null;
		String cloudName = null;
		String name = FAKE_NETWORK_NAME;
		String gateway = "10.10.10.1";
		String cidr = FAKE_CIDR_ADDRESS;
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

	private CloudUser createCloudUser() {
		String tokenValue = LOCAL_TOKEN_VALUE;
		String userId = FAKE_USER_ID;

		CloudUser cloudUser = new CloudUser(userId, null, tokenValue);
		return cloudUser;
	}

	private String getSecurityGroupTemplate(String securityGroupName) {
		String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" 
				+ "<TEMPLATE>\n"
				+ "    <NAME>fogbow-sg-pn-fake-order-id</NAME>\n"
				+ "    <RULE>\n"
				+ "        <PROTOCOL>ALL</PROTOCOL>\n"
				+ "        <RULE_TYPE>inbound</RULE_TYPE>\n"
				+ "    </RULE>\n"
				+ "    <RULE>\n"
				+ "        <PROTOCOL>ALL</PROTOCOL>\n"
				+ "        <RULE_TYPE>outbound</RULE_TYPE>\n"
				+ "    </RULE>\n"
				+ "</TEMPLATE>\n";
		
		return String.format(template, securityGroupName);
	}
	
	private String getNetworkReserveTemplate(String networkName) {
		String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
				+ "<TEMPLATE>\n"
				+ "    <NAME>%s</NAME>\n"
				+ "    <SIZE>256</SIZE>\n"
				+ "</TEMPLATE>\n";
		
		return String.format(template, networkName);
	}
	
	private String getNetworkUpdateTemplate() {
		return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
				+ "<TEMPLATE>\n"
				+ "    <SECURITY_GROUPS>0,1</SECURITY_GROUPS>\n"
				+ "</TEMPLATE>\n";
	}
	
}
