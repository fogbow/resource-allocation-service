package cloud.fogbow.ras.core.plugins.interoperability.opennebula.network.v5_4;

import java.io.File;
import java.util.UUID;

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
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.NetworkAllocationMode;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.plugins.interoperability.NetworkPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest({OpenNebulaClientUtil.class, SecurityGroup.class, UUID.class, VirtualNetwork.class})
public class OpenNebulaNetworkPluginTest {

	private static final String EMPTY_STRING = "";
	private static final String FAKE_ADDRESS = "10.1.0.0";
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
	private static final String OPENNEBULA_CLOUD_NAME_DIRECTORY = "opennebula";

	private static final int MAXIMUM_INTEGER_VALUE = 2147483647;
	private static final int NEGATIVE_SIZE_VALUE = -1;
	private static final int ZERO_VALUE = 0;
	
	private OpenNebulaNetworkPlugin plugin;

	@Before
	public void setUp() {
		String opennebulaConfFilePath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME
				+ File.separator + OPENNEBULA_CLOUD_NAME_DIRECTORY + File.separator
				+ SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

		this.plugin = Mockito.spy(new OpenNebulaNetworkPlugin(opennebulaConfFilePath));
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
		String securityGroupName = NetworkPlugin.SECURITY_GROUP_PREFIX + FAKE_ORDER_ID;
		String securityGroupTemplate = getSecurityGroupTemplate(securityGroupName);

		String securityGroupID = ID_VALUE_ONE;
		BDDMockito.given(
				OpenNebulaClientUtil.allocateSecurityGroup(Mockito.eq(client), Mockito.eq(securityGroupTemplate)))
				.willReturn(securityGroupID);

		// exercise
		this.plugin.createSecurityGroup(client, virtualNetworkId, networkOrder);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getVirtualNetwork(Mockito.eq(client), Mockito.eq(virtualNetworkId));

		Mockito.verify(virtualNetwork, Mockito.times(1))
				.xpath(Mockito.eq(OpenNebulaNetworkPlugin.VNET_ADDRESS_RANGE_IP_PATH));

		Mockito.verify(virtualNetwork, Mockito.times(1))
				.xpath(Mockito.eq(OpenNebulaNetworkPlugin.VNET_ADDRESS_RANGE_SIZE_PATH));
		
		Mockito.verify(this.plugin, Mockito.times(1)).generateSecurityGroupName(Mockito.eq(networkOrder));

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.allocateSecurityGroup(Mockito.eq(client), Mockito.eq(securityGroupTemplate));
	}
	
	// test case: When calling the getSecurityGroupBy method, with valid security
	// groups associated with the virtual network passed by parameter, it must
	// return the ID of the security group created together with this Virtual
	// Network.
	@Test
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
		String securityGroupId = this.plugin.getSecurityGroupBy(virtualNetwork);

		// verify
		Mockito.verify(virtualNetwork, Mockito.times(1))
				.xpath(Mockito.eq(OpenNebulaNetworkPlugin.VNET_TEMPLATE_SECURITY_GROUPS_PATH));

		Assert.assertEquals(expected, securityGroupId);
	}
	
	// test case: When calling the getSecurityGroupBy method, with a null security
	// group, it must return a null security group ID associated with the virtual
	// network passed by parameter.
	@Test
	public void testGetSecurityGroupByVirtualNetworkWithSecurityGroupsNull()
			throws UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {

		// set up
		VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		PowerMockito.when(OpenNebulaClientUtil.getVirtualNetwork(Mockito.any(Client.class), Mockito.anyString()))
				.thenReturn(virtualNetwork);

		String securityGroups = null;
		Mockito.when(virtualNetwork.xpath(Mockito.anyString())).thenReturn(securityGroups);

		// exercise
		String securityGroupId = this.plugin.getSecurityGroupBy(virtualNetwork);

		// verify
		Mockito.verify(virtualNetwork, Mockito.times(1))
				.xpath(Mockito.eq(OpenNebulaNetworkPlugin.VNET_TEMPLATE_SECURITY_GROUPS_PATH));

		Assert.assertNull(securityGroupId);
	}
	
	// test case: When calling the getSecurityGroupBy method, with an empty security
	// group, it must return a null security group ID associated with the virtual
	// network passed by parameter.
	@Test
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
		String securityGroupId = this.plugin.getSecurityGroupBy(virtualNetwork);

		// verify
		Mockito.verify(virtualNetwork, Mockito.times(1))
				.xpath(Mockito.eq(OpenNebulaNetworkPlugin.VNET_TEMPLATE_SECURITY_GROUPS_PATH));

		Assert.assertNull(securityGroupId);
	}
	
	// test case: When calling the getSecurityGroupBy method, with only one
	// associated default security groups, it must return a null security group ID
	// of the virtual network passed by parameter.
	@Test
	public void testGetSecurityGroupByVirtualNetworkWithSecurityGroupsWrongFormat()
			throws UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {

		// set up
		VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		PowerMockito.when(OpenNebulaClientUtil.getVirtualNetwork(Mockito.any(Client.class), Mockito.anyString()))
				.thenReturn(virtualNetwork);

		String securityGroups = ID_VALUE_ZERO;
		Mockito.when(virtualNetwork.xpath(Mockito.anyString())).thenReturn(securityGroups);

		// exercise
		String securityGroupId = this.plugin.getSecurityGroupBy(virtualNetwork);

		// verify
		Mockito.verify(virtualNetwork, Mockito.times(1))
				.xpath(Mockito.eq(OpenNebulaNetworkPlugin.VNET_TEMPLATE_SECURITY_GROUPS_PATH));

		Assert.assertNull(securityGroupId);
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
		NetworkOrder networkOrder = createNetworkOrder(networkName);

		networkName = OpenNebulaNetworkPlugin.FOGBOW_NETWORK_NAME + FAKE_NETWORK_NAME;
		String networkReserveTemplate = getNetworkReserveTemplate(networkName);
		Mockito.doReturn(FAKE_NETWORK_NAME).when(this.plugin).getRandomUUID();

		BDDMockito.given(OpenNebulaClientUtil.reserveVirtualNetwork(Mockito.eq(client), Mockito.eq(defaultNetworkID),
				Mockito.eq(networkReserveTemplate))).willReturn(ID_VALUE_ONE);

		VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
		BDDMockito.given(OpenNebulaClientUtil.getVirtualNetwork(Mockito.eq(client), Mockito.eq(ID_VALUE_ONE)))
				.willReturn(virtualNetwork);

		Mockito.doReturn(ID_VALUE_ONE).when(this.plugin).createSecurityGroup(Mockito.any(Client.class),
				Mockito.anyString(), Mockito.eq(networkOrder));

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
				Mockito.anyString(), Mockito.eq(networkOrder));

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
		NetworkOrder networkOrder = createNetworkOrder(networkName);
		String networkReserveTemplate = getNetworkReserveTemplate(networkName);

		BDDMockito.given(OpenNebulaClientUtil.reserveVirtualNetwork(Mockito.eq(client), Mockito.eq(defaultNetworkID),
				Mockito.eq(networkReserveTemplate))).willReturn(ID_VALUE_ONE);

		VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
		BDDMockito.given(OpenNebulaClientUtil.getVirtualNetwork(Mockito.eq(client), Mockito.eq(ID_VALUE_ONE)))
				.willReturn(virtualNetwork);

		Mockito.doReturn(ID_VALUE_ONE).when(this.plugin).createSecurityGroup(Mockito.any(Client.class),
				Mockito.anyString(), Mockito.eq(networkOrder));

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
				Mockito.anyString(), Mockito.eq(networkOrder));

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

		// exercise
		this.plugin.deleteInstance(instanceId, cloudUser);

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

		// exercise
		this.plugin.deleteInstance(instanceId, cloudUser);

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

		Mockito.doReturn(FAKE_CIDR_ADDRESS).when(this.plugin).generateAddressCIDR(FAKE_ADDRESS, FAKE_SIZE);

		CloudUser cloudUser = createCloudUser();
		String instanceId = FAKE_INSTANCE_ID;

		// exercise
		this.plugin.getInstance(instanceId, cloudUser);

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
		int value = this.plugin.calculateCIDR(size);

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
		int value = this.plugin.calculateCIDR(size);

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
		String cidr = this.plugin.generateAddressCIDR(address, size);
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

	private NetworkOrder createNetworkOrder(String networkName) {
		String providingMember = null;
		String cloudName = null;
		String name = networkName;
		String gateway = "10.10.10.1";
		String cidr = "10.10.10.0/24";
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
				+ "    <NAME>ras-sg-pn-fake-order-id</NAME>\n"
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
