package cloud.fogbow.ras.core.plugins.interoperability.opennebula.network.v5_4;

import java.util.UUID;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaBaseTests;
import org.apache.commons.net.util.SubnetUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.secgroup.SecurityGroup;
import org.opennebula.client.vnet.VirtualNetwork;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import cloud.fogbow.ras.core.models.NetworkAllocationMode;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;

import static cloud.fogbow.ras.core.plugins.interoperability.opennebula.network.v5_4.OpenNebulaNetworkPlugin.*;

@PrepareForTest({OpenNebulaClientUtil.class, SecurityGroup.class, UUID.class, VirtualNetwork.class, DatabaseManager.class})
public class OpenNebulaNetworkPluginTest extends OpenNebulaBaseTests {

	private static final String EMPTY_STRING = "";
	private static final String FAKE_ADDRESS = "10.1.0.0";
	private static final String FIRST_ADDRESS = "10.1.0.1";
	private static final String FAKE_CIDR_ADDRESS = "10.1.0.0/24";
	private static final String FAKE_INSTANCE_ID = "fake-instance-id";
	private static final String FAKE_NETWORK_NAME = "fake-network-name";
	private static final String FAKE_SIZE = "256";
	private static final String FAKE_VLAN_ID = "fake-vlan-id";
	private static final String ID_VALUE_ONE = "1";
	private static final String ID_VALUE_ZERO = "0";
	private static final String TEN_STRING_VALUE = "10";

	private static final int MAXIMUM_INTEGER_VALUE = 2147483647;
	private static final int NEGATIVE_SIZE_VALUE = -1;
	private static final int ZERO_VALUE = 0;
	private static final Integer ONE_VALUE = 1;

	private OpenNebulaNetworkPlugin plugin;
	private VirtualNetwork virtualNetwork;
	private NetworkOrder networkOrder;
	private String orderId;
	private String instanceId;

	@Before
	public void setUp() throws FogbowException {
		super.setUp();

		this.plugin = Mockito.spy(new OpenNebulaNetworkPlugin(this.openNebulaConfFilePath));
		this.virtualNetwork = Mockito.mock(VirtualNetwork.class);
		this.networkOrder = Mockito.spy(this.createNetworkOrder());
		this.orderId = this.networkOrder.getId();
		this.instanceId = this.networkOrder.getInstanceId();

		Mockito.when(OpenNebulaClientUtil.getVirtualNetwork(Mockito.any(Client.class), Mockito.anyString()))
				.thenReturn(this.virtualNetwork);
	}

	// test case: When calling the requestInstance method, with a valid client and
	// an order without a network name, a template must be generated with a default
	// network name and the other associated data, to reserve a network, returning
	// its instance ID.
	@Test
	public void testRequestInstance() throws FogbowException {
		// set up
		CreateNetworkReserveRequest request = Mockito.mock(CreateNetworkReserveRequest.class);

		Mockito.doReturn(request).when(this.plugin).getCreateNetworkReserveRequest(
				Mockito.any(NetworkOrder.class), Mockito.any(VirtualNetwork.class));
		Mockito.doReturn(this.networkOrder.getInstanceId()).when(this.plugin).doRequestInstance(
				Mockito.any(Client.class), Mockito.anyString(), Mockito.any(CreateNetworkReserveRequest.class));

		// exercise
		this.plugin.requestInstance(this.networkOrder, this.cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class);
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.eq(this.cloudUser.getToken()));
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class);
		OpenNebulaClientUtil.getVirtualNetwork(Mockito.any(Client.class), Mockito.anyString());

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doRequestInstance(
				Mockito.any(Client.class), Mockito.anyString(), Mockito.any(CreateNetworkReserveRequest.class));
	}

	// test case: when invoking getCreateNetworkReserveRequest with valid network order and virtual network,
	// the plugin should create and return the respective CreateNetworkReserveRequest
	@Test
	public void testGetCreateNetworkReserveRequest() throws InvalidParameterException, UnacceptableOperationException {
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

		// exercise
		this.plugin.getCreateNetworkReserveRequest(this.networkOrder, this.virtualNetwork);

		// verify
		Mockito.verify(this.networkOrder, Mockito.times(TestUtils.RUN_ONCE)).getCidr();
		Mockito.verify(this.networkOrder, Mockito.times(TestUtils.RUN_ONCE)).getName();
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getAddressRangeIndex(
				Mockito.eq(this.virtualNetwork), Mockito.eq(firstAddress), Mockito.eq(size));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getAddressRangeId(
				Mockito.eq(this.virtualNetwork), Mockito.eq(ZERO_VALUE), Mockito.eq(FAKE_CIDR_ADDRESS));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getNextAvailableAddress(
				Mockito.eq(this.virtualNetwork), Mockito.eq(ZERO_VALUE));
	}

	// test case: when invoking getAddressRangeIndex with valid virtual network, the plugin
	// should return the address range index where the new network reservation should be
	// made; return null otherwise.
	@Test
	public void testGetAddressRangeIndex() throws InvalidParameterException {
		// set up
		Mockito.when(this.virtualNetwork.xpath(Mockito.eq(String.format(ADDRESS_RANGE_IP_PATH_FORMAT, ONE_VALUE))))
				.thenReturn(FAKE_ADDRESS)
				.thenReturn(EMPTY_STRING);
		Mockito.when(this.virtualNetwork.xpath(Mockito.eq(String.format(ADDRESS_RANGE_SIZE_PATH_FORMAT, ONE_VALUE))))
				.thenReturn(TEN_STRING_VALUE);
		Mockito.when(this.virtualNetwork.xpath(Mockito.eq(String.format(ADDRESS_RANGE_USED_LEASES_PATH_FORMAT, ONE_VALUE))))
				.thenReturn(ID_VALUE_ZERO);

		// exercise
		Integer index = this.plugin.getAddressRangeIndex(this.virtualNetwork, FIRST_ADDRESS, ONE_VALUE);
		Integer nullIndex = this.plugin.getAddressRangeIndex(this.virtualNetwork, FIRST_ADDRESS, ONE_VALUE);

		// verifiy
		Mockito.verify(this.virtualNetwork, Mockito.times(TestUtils.RUN_SIX_TIMES)).xpath(Mockito.anyString());
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).convertToInteger(Mockito.anyString());
		Assert.assertEquals(ONE_VALUE, index);
		Assert.assertEquals(null, nullIndex);
	}

	// test case: when invoking getAddressRangeId with valid virtual network, address range index and
	// cidr, the plugin should return the address range id where the new network reservation should
	// be made.
	@Test
	public void testGetAddressRangeId() throws UnacceptableOperationException {
		// set up
		Mockito.when(this.virtualNetwork.xpath(Mockito.eq(String.format(ADDRESS_RANGE_ID_PATH_FORMAT, ONE_VALUE))))
				.thenReturn(String.valueOf(ONE_VALUE));

		// exercise
		String index = this.plugin.getAddressRangeId(this.virtualNetwork, ONE_VALUE, FAKE_CIDR_ADDRESS);

		// verify
		Mockito.verify(this.virtualNetwork, Mockito.times(TestUtils.RUN_ONCE)).xpath(Mockito.anyString());
		Assert.assertEquals(String.valueOf(ONE_VALUE), index);
	}

	// test case: when invoking getAddressRangeId with a null address range index (meaning no address range fits
	// the order) the plugin should throw a UnacceptableOperationException.
	@Test
	public void testGetAddressRangeIdFail() {
		// set up
		String expectedMessage = String.format(Messages.Exception.UNABLE_TO_CREATE_NETWORK_RESERVE, FAKE_CIDR_ADDRESS);

		// exercise
		try {
			this.plugin.getAddressRangeId(this.virtualNetwork, null, FAKE_CIDR_ADDRESS);
			Assert.fail();
		} catch (UnacceptableOperationException e) {
			// verify
			Assert.assertEquals(expectedMessage, e.getMessage());
		}
	}

	// test case: when invoking getNextAvailableAddress with valid virtual network and address range index,
	// the plugin should return the first available address for the new network reservation.
	@Test
	public void testGetNextAvailableAddress() throws InvalidParameterException {
		// set up
		Mockito.when(this.virtualNetwork.xpath(Mockito.eq(String.format(ADDRESS_RANGE_IP_PATH_FORMAT, ONE_VALUE))))
				.thenReturn(FAKE_ADDRESS);
		Mockito.when(this.virtualNetwork.xpath(Mockito.eq(String.format(ADDRESS_RANGE_SIZE_PATH_FORMAT, ONE_VALUE))))
				.thenReturn(TEN_STRING_VALUE);
		Mockito.when(this.virtualNetwork.xpath(Mockito.eq(String.format(ADDRESS_RANGE_USED_LEASES_PATH_FORMAT, ONE_VALUE))))
				.thenReturn(String.valueOf(ONE_VALUE))
				.thenReturn(ID_VALUE_ZERO);

		// exercise
		this.plugin.getNextAvailableAddress(this.virtualNetwork, ONE_VALUE);
		String defaultFistIp = this.plugin.getNextAvailableAddress(this.virtualNetwork, ONE_VALUE);

		// verify
		Mockito.verify(this.virtualNetwork, Mockito.times(TestUtils.RUN_SIX_TIMES)).xpath(Mockito.anyString());
		Assert.assertEquals(FAKE_ADDRESS, defaultFistIp);
	}

	// test case: when invoking doRequestInstance with valid client, order id and create network reservation
	// request, the plugin should return the newly created reservation instance id.
	@Test
	public void testDoRequestInstance() throws InvalidParameterException, InstanceNotFoundException, UnauthorizedRequestException {
		// set up
		String updateNetworkTemplate = this.getNetworkUpdateTemplate();
		CreateNetworkReserveRequest request = Mockito.spy(this.getCreateNetworkReserveRequest());

		Mockito.when(OpenNebulaClientUtil.reserveVirtualNetwork(Mockito.any(Client.class), Mockito.anyInt(), Mockito.anyString()))
				.thenReturn(ID_VALUE_ZERO);
		Mockito.when(OpenNebulaClientUtil.updateVirtualNetwork(Mockito.any(Client.class), Mockito.anyInt(), Mockito.anyString()))
				.thenReturn(ID_VALUE_ONE);
		Mockito.doReturn(ZERO_VALUE).when(this.plugin).convertToInteger(Mockito.anyString());
		Mockito.doReturn(updateNetworkTemplate).when(this.plugin).getNetworkUpdateTemplate(
				Mockito.any(Client.class), Mockito.anyString());

		// exercise
		this.plugin.doRequestInstance(this.client, this.orderId, request);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class);
		OpenNebulaClientUtil.reserveVirtualNetwork(
				Mockito.any(Client.class), Mockito.eq(ZERO_VALUE), Mockito.anyString());
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class);
		OpenNebulaClientUtil.updateVirtualNetwork(
				Mockito.eq(this.client), Mockito.eq(ZERO_VALUE), Mockito.eq(updateNetworkTemplate));

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).convertToInteger(Mockito.eq(ID_VALUE_ZERO));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getNetworkUpdateTemplate(
				Mockito.eq(this.client), Mockito.eq(ID_VALUE_ZERO));
		Mockito.verify(request, Mockito.times(TestUtils.RUN_ONCE)).getVirtualNetworkReserved();
	}

	// test case: when invoking createSecurityGroup with valid client, instance id and order id,
	// the plugin should create the respective security group and update the network order reservation.
	@Test
	public void testGetNetworkUpdateTemplate() throws InvalidParameterException, UnauthorizedRequestException, InstanceNotFoundException {
		// set up
		Mockito.doReturn(ID_VALUE_ZERO).when(this.plugin).createSecurityGroup(
				Mockito.any(Client.class), Mockito.anyString());

		// exercise
		this.plugin.createSecurityGroup(this.client, this.instanceId);

		// verify
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).createSecurityGroup(
				Mockito.eq(this.client), Mockito.eq(this.instanceId));
	}

	// test case: When you call the createSecurityGroup method with a valid client,
	// virtual network ID, and network request, it must create a security group for
	// this network by returning its ID.
	@Test
	public void testCreateSecurityGroup() throws UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {
		// set up
		Mockito.when(OpenNebulaClientUtil.allocateSecurityGroup(Mockito.any(Client.class), Mockito.anyString())).thenReturn(ID_VALUE_ZERO);
		Mockito.when(this.virtualNetwork.xpath(Mockito.eq(VNET_ADDRESS_RANGE_IP_PATH))).thenReturn(FAKE_ADDRESS);
		Mockito.when(this.virtualNetwork.xpath(Mockito.eq(VNET_ADDRESS_RANGE_SIZE_PATH))).thenReturn(FAKE_SIZE);

		// exercise
		this.plugin.createSecurityGroup(this.client, this.instanceId);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getVirtualNetwork(Mockito.eq(this.client), Mockito.eq(this.instanceId));
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.allocateSecurityGroup(Mockito.eq(this.client), Mockito.anyString());

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).generateSecurityGroupName(Mockito.eq(this.instanceId));
	}

	// test case: when invoking getInstance with valid order and cloud user, the plugin should return
	// the respective ONe virtual network as a fogbow instance
	@Test
	public void testGetInstance() throws FogbowException {
		// set up
		NetworkInstance instance = new NetworkInstance(FAKE_INSTANCE_ID);

		Mockito.doReturn(instance).when(this.plugin).doGetInstance(Mockito.any(VirtualNetwork.class));

		// exercise
		this.plugin.getInstance(this.networkOrder, this.cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getVirtualNetwork(Mockito.eq(this.client), Mockito.eq(this.instanceId));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetInstance(Mockito.eq(this.virtualNetwork));
	}

	// test case: when invoking doGetInstance with a valid ONe virtual network, the plugin
	// should return the respective fogbow instance
	@Test
	public void testDoGetInstance() throws InvalidParameterException {
	    // set up
		Mockito.when(this.virtualNetwork.getId()).thenReturn(ID_VALUE_ZERO);
		Mockito.when(this.virtualNetwork.getName()).thenReturn(FAKE_NETWORK_NAME);
		Mockito.when(this.virtualNetwork.xpath(Mockito.eq(VNET_TEMPLATE_VLAN_ID_PATH))).thenReturn(FAKE_VLAN_ID);
		Mockito.when(this.virtualNetwork.xpath(Mockito.eq(VNET_ADDRESS_RANGE_IP_PATH))).thenReturn(FAKE_ADDRESS);
		Mockito.when(this.virtualNetwork.xpath(Mockito.eq(VNET_ADDRESS_RANGE_SIZE_PATH))).thenReturn(FAKE_SIZE);
		Mockito.doReturn(FAKE_ADDRESS).when(this.plugin).generateAddressCidr(Mockito.anyString(), Mockito.anyString());

		// exercise
		NetworkInstance instance = this.plugin.doGetInstance(this.virtualNetwork);

		// verify
		Mockito.verify(this.virtualNetwork, Mockito.times(TestUtils.RUN_ONCE)).getId();
		Mockito.verify(this.virtualNetwork, Mockito.times(TestUtils.RUN_ONCE)).getName();
		Mockito.verify(this.virtualNetwork, Mockito.times(TestUtils.RUN_ONCE)).xpath(Mockito.eq(VNET_TEMPLATE_VLAN_ID_PATH));
		Mockito.verify(this.virtualNetwork, Mockito.times(TestUtils.RUN_ONCE)).xpath(Mockito.eq(VNET_ADDRESS_RANGE_IP_PATH));
		Mockito.verify(this.virtualNetwork, Mockito.times(TestUtils.RUN_ONCE)).xpath(Mockito.eq(VNET_ADDRESS_RANGE_SIZE_PATH));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).generateAddressCidr(
				Mockito.eq(FAKE_ADDRESS), Mockito.eq(FAKE_SIZE));

		Assert.assertNotNull(instance);
	}

	// test case: when invoking deleteInstance with valid order and cloud user,
	// the plugin should retrieve and delete the respective ONe virtual network.
	@Test
	public void testDeleteInstance() throws FogbowException {
		// set up
		SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);

		Mockito.doReturn(securityGroup).when(this.plugin).getSecurityGroupForVirtualNetwork(
				Mockito.any(Client.class), Mockito.any(VirtualNetwork.class), Mockito.anyString());
		Mockito.doNothing().when(this.plugin).deleteSecurityGroup(Mockito.any(SecurityGroup.class));
		Mockito.doNothing().when(this.plugin).doDeleteInstance(Mockito.any(VirtualNetwork.class));

		// exercise
		this.plugin.deleteInstance(this.networkOrder, this.cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class);
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.eq(this.cloudUser.getToken()));
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getVirtualNetwork(Mockito.eq(this.client), Mockito.eq(this.instanceId));

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getSecurityGroupForVirtualNetwork(
				Mockito.eq(this.client), Mockito.eq(this.virtualNetwork), Mockito.eq(this.instanceId));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).deleteSecurityGroup(securityGroup);
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doDeleteInstance(this.virtualNetwork);
	}

	// test case: When calling the getSecurityGroupBy method, with valid security
	// groups associated with the virtual network passed by parameter, it must
	// return the ID of the security group created together with this Virtual
	// Network.
	@Test
	public void testGetSecurityGroupForVirtualNetwork() throws UnauthorizedRequestException, InstanceNotFoundException,
			InvalidParameterException {
		// set up
        String securityGroupIds = ID_VALUE_ZERO + SECURITY_GROUPS_SEPARATOR + ID_VALUE_ONE;
        SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);

        Mockito.when(securityGroup.getName()).thenReturn(FAKE_NETWORK_NAME);
		Mockito.when(this.virtualNetwork.xpath(VNET_TEMPLATE_SECURITY_GROUPS_PATH)).thenReturn(securityGroupIds);
		Mockito.when(OpenNebulaClientUtil.getSecurityGroup(Mockito.any(Client.class), Mockito.anyString()))
				.thenReturn(securityGroup);
		Mockito.doReturn(FAKE_NETWORK_NAME).when(this.plugin).generateSecurityGroupName(Mockito.anyString());

		// excercise
		SecurityGroup secGroup = this.plugin.getSecurityGroupForVirtualNetwork(this.client, this.virtualNetwork, this.networkOrder.getId());


		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class);
		OpenNebulaClientUtil.getSecurityGroup(Mockito.eq(this.client), Mockito.eq(ID_VALUE_ZERO));

		Mockito.verify(this.virtualNetwork, Mockito.times(TestUtils.RUN_ONCE)).xpath(Mockito.eq(VNET_TEMPLATE_SECURITY_GROUPS_PATH));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).generateSecurityGroupName(Mockito.eq(this.orderId));
		Mockito.verify(securityGroup, Mockito.times(TestUtils.RUN_ONCE)).getName();

		Assert.assertNotNull(secGroup);
	}

	// test case: When calling the getSecurityGroupBy method, with an empty security
	// group, it must return a null security group ID associated with the virtual
	// network passed by parameter.
	@Test
	public void testGetSecurityGroupForVirtualNetworkNull() throws UnauthorizedRequestException, InstanceNotFoundException,
			InvalidParameterException {
		// set up
		Mockito.when(this.virtualNetwork.xpath(VNET_TEMPLATE_SECURITY_GROUPS_PATH)).thenReturn(null);

		// excercise
		SecurityGroup secGroup = this.plugin.getSecurityGroupForVirtualNetwork(this.client, this.virtualNetwork, this.networkOrder.getId());


		// verify
		Mockito.verify(this.virtualNetwork, Mockito.times(TestUtils.RUN_ONCE)).xpath(Mockito.eq(VNET_TEMPLATE_SECURITY_GROUPS_PATH));

		Assert.assertNull(secGroup);
	}

	// test case: when invoking deleteSecurityGroup, the plugin should delete the respective
	// ONe sec group; log an error otherwise
	@Test
	public void testDeleteSecurityGroup() {
		// set up
		SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);
		OneResponse response = Mockito.mock(OneResponse.class);

		Mockito.when(securityGroup.delete()).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(false).thenReturn(true);
		Mockito.when(response.getMessage()).thenReturn(ID_VALUE_ZERO);

		// exercise
		this.plugin.deleteSecurityGroup(securityGroup);
		this.plugin.deleteSecurityGroup(securityGroup);

		// verify
		Mockito.verify(securityGroup, Mockito.times(TestUtils.RUN_TWICE)).delete();
		Mockito.verify(response, Mockito.times(TestUtils.RUN_TWICE)).isError();
		Mockito.verify(response, Mockito.times(TestUtils.RUN_ONCE)).getMessage();
	}

	// test case: when calling doDeleteInstance with a valid ONe virtual network, the plugin
	// should delete it
	@Test
	public void testDoDeleteInstance() throws UnexpectedException {
	    // set up
		OneResponse response = Mockito.mock(OneResponse.class);

		Mockito.when(response.isError()).thenReturn(false);
		Mockito.when(this.virtualNetwork.delete()).thenReturn(response);

		// exercise
		this.plugin.doDeleteInstance(this.virtualNetwork);

		// verify
		Mockito.verify(this.virtualNetwork, Mockito.times(TestUtils.RUN_ONCE)).delete();
		Mockito.verify(response, Mockito.times(TestUtils.RUN_ONCE)).isError();
	}

	// test case: when calling doDeleteInstance with an invalid ONe virtual network, the plugin
	// should throw an UnpectedException
	@Test
	public void testDoDeleteInstanceFail() {
		// set up
		OneResponse response = Mockito.mock(OneResponse.class);
		String message = String.format(Messages.Error.ERROR_WHILE_REMOVING_RESOURCE, VIRTUAL_NETWORK_RESOURCE, ID_VALUE_ZERO);

		Mockito.when(response.isError()).thenReturn(true);
		Mockito.when(response.getMessage()).thenReturn(ID_VALUE_ZERO);
		Mockito.when(this.virtualNetwork.delete()).thenReturn(response);

		// exercise
		try {
			this.plugin.doDeleteInstance(this.virtualNetwork);
			Assert.fail();
		} catch (UnexpectedException e) {
		    Assert.assertEquals(message, e.getMessage());
		}

		// verify
		Mockito.verify(this.virtualNetwork, Mockito.times(TestUtils.RUN_ONCE)).delete();
		Mockito.verify(response, Mockito.times(TestUtils.RUN_ONCE)).isError();
		Mockito.verify(response, Mockito.times(TestUtils.RUN_ONCE)).getMessage();
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
		String gateway = FAKE_ADDRESS;
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

	private String getNetworkUpdateTemplate() {
		return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
				+ "<TEMPLATE>\n"
				+ "    <SECURITY_GROUPS>0,1</SECURITY_GROUPS>\n"
				+ "</TEMPLATE>\n";
	}

	private CreateNetworkReserveRequest getCreateNetworkReserveRequest() {
		return new CreateNetworkReserveRequest.Builder()
				.name(FAKE_NETWORK_NAME)
				.size(Integer.parseInt(FAKE_SIZE))
				.ip(FAKE_ADDRESS)
				.addressRangeId(ID_VALUE_ZERO)
				.build();
	}
}
