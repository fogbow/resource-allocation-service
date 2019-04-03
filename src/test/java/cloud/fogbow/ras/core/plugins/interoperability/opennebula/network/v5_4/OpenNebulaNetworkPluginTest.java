package cloud.fogbow.ras.core.plugins.interoperability.opennebula.network.v5_4;

import java.io.File;

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

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.NetworkAllocationMode;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest({OpenNebulaClientUtil.class, SecurityGroup.class, VirtualNetwork.class})
public class OpenNebulaNetworkPluginTest {

	private static final String EMPTY_STRING = "";
	private static final String FAKE_ADDRESS = "fake-address";
	private static final String FAKE_GATEWAY = "fake-gateway";
	private static final String FAKE_INSTANCE_ID = "fake-instance-id";
	private static final String FAKE_NETWORK_NAME = "fake-network-name";
	private static final String FAKE_USER_ID = "fake-user-id";
	private static final String FAKE_VLAN_ID = "fake-vlan-id";
	private static final String ID_VALUE_ONE = "1";
	private static final String ID_VALUE_ZERO = "0";
	private static final String LOCAL_TOKEN_VALUE = "user:password";
	
	private OpenNebulaNetworkPlugin plugin;

	@Before
	public void setUp() {
		String opennebulaConfFilePath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME
				+ File.separator + SystemConstants.OPENNEBULA_CLOUD_NAME_DIRECTORY + File.separator
				+ SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

		this.plugin = Mockito.spy(new OpenNebulaNetworkPlugin(opennebulaConfFilePath));
	}

	// test case: When calling the getSecurityGroupBy method, with valid security
	// groups associated with the virtual network passed by parameter, it must
	// return the ID of the security group created together with this Virtual
	// Network.
	@Test
	public void testGetSecurityGroupByVirtualNetworkSucessfully()
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
	// network name and the other associated data, to allocate a network, returning
	// its instance ID.
	@Ignore
	@Test
	public void testRequestInstanceSuccessfullyWithDefaultNetworkName() throws FogbowException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		String networkName = null;
		NetworkOrder networkOrder = createNetworkOrder(networkName);
//		Mockito.doReturn(ID_VALUE_ONE).when(this.plugin).createSecurityGroup(Mockito.eq(client),
//				Mockito.eq(networkOrder));

		Mockito.doReturn(FAKE_NETWORK_NAME).when(this.plugin).getRandomUUID();

		OneResponse response = Mockito.mock(OneResponse.class);
		PowerMockito.mockStatic(VirtualNetwork.class);
		BDDMockito.given(VirtualNetwork.allocate(Mockito.eq(client), Mockito.anyString())).willReturn(response);
		Mockito.when(response.isError()).thenReturn(false);

		CloudUser cloudUser = createCloudUser();
		networkName = OpenNebulaNetworkPlugin.FOGBOW_NETWORK_NAME + FAKE_NETWORK_NAME;
		String virtualNetworkTemplate = generateNetworkTemplate(networkName);

		// exercise
		this.plugin.requestInstance(networkOrder, cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

//		Mockito.verify(this.plugin, Mockito.times(1)).createSecurityGroup(Mockito.eq(client), Mockito.eq(networkOrder));

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.allocateVirtualNetwork(Mockito.eq(client), Mockito.eq(virtualNetworkTemplate));
	}
	
	// test case: When calling the requestInstance method, with a valid client and
	// an order with a network name, a template must be generated with the
	// associated data, to allocate a network, returning its instance ID.
	@Ignore
	@Test
	public void testRequestInstanceSuccessfullyWithoutDefaultNetworkName() throws FogbowException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		String networkName = FAKE_NETWORK_NAME;
		String virtualNetworkTemplate = generateNetworkTemplate(networkName);
		NetworkOrder networkOrder = createNetworkOrder(networkName);
//		Mockito.doReturn(ID_VALUE_ONE).when(this.plugin).createSecurityGroup(Mockito.eq(client),
//				Mockito.eq(networkOrder));

		OneResponse response = Mockito.mock(OneResponse.class);
		PowerMockito.mockStatic(VirtualNetwork.class);
		BDDMockito.given(VirtualNetwork.allocate(Mockito.eq(client), Mockito.anyString())).willReturn(response);
		Mockito.when(response.isError()).thenReturn(false);

		CloudUser cloudUser = createCloudUser();

		// exercise
		this.plugin.requestInstance(networkOrder, cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

//		Mockito.verify(this.plugin, Mockito.times(1)).createSecurityGroup(Mockito.eq(client), Mockito.eq(networkOrder));

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.allocateVirtualNetwork(Mockito.eq(client), Mockito.eq(virtualNetworkTemplate));
	}
	
	// test case: When calling the requestInstance method, and an error occurs while
	// attempting to allocate a virtual network, an InvalidParameterException will
	// be thrown.
	@Ignore
	@Test(expected = InvalidParameterException.class)
	public void testRequestInstanceThrowInvalidParameterException() throws FogbowException {
		// set up
		OneResponse response = Mockito.mock(OneResponse.class);
		PowerMockito.mockStatic(SecurityGroup.class);
		BDDMockito.given(SecurityGroup.allocate(Mockito.any(Client.class), Mockito.anyString())).willReturn(response);
		Mockito.when(response.isError()).thenReturn(false);
		Mockito.when(response.getMessage()).thenReturn(ID_VALUE_ONE);

		response = Mockito.mock(OneResponse.class);
		PowerMockito.mockStatic(VirtualNetwork.class);
		BDDMockito.given(VirtualNetwork.allocate(Mockito.any(Client.class), Mockito.anyString())).willReturn(response);
		Mockito.when(response.isError()).thenReturn(true);

		CloudUser cloudUser = createCloudUser();
		NetworkOrder networkOrder = createNetworkOrder(FAKE_NETWORK_NAME);

		// exercise
		this.plugin.requestInstance(networkOrder, cloudUser);
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
	// token value and
	// instance ID, it must returned a instance of a virtual network.
	@Ignore
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
		Mockito.doReturn(FAKE_ADDRESS).when(virtualNetwork).xpath(Mockito.anyString());
		Mockito.doReturn(FAKE_GATEWAY).when(virtualNetwork).xpath(Mockito.anyString());
		Mockito.doReturn(FAKE_VLAN_ID).when(virtualNetwork).xpath(Mockito.anyString());
		Mockito.doReturn(1).when(virtualNetwork).state();

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
	
	// test case: When calling the sliceCIDR method, an invalid CIDR, it must throw
	// an InvalidParameterException.
	@Ignore
	@Test(expected = InvalidParameterException.class) // verify
	public void testSliceCIDRThrowInvalidParameterException() throws InvalidParameterException {
		// set up
		String cidr = null;

		// exercise
//		this.plugin.sliceCIDR(cidr);
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
	
	private String generateNetworkTemplate(String networkName) {
		String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + 
				"<TEMPLATE>\n" + 
				"    <AR>\n" + 
				"        <IP>10.10.10.0</IP>\n" + 
				"        <SIZE>256</SIZE>\n" + 
				"        <TYPE>IP4</TYPE>\n" + 
				"    </AR>\n" + 
				"    <BRIDGE>br0</BRIDGE>\n" + 
				"    <VN_MAD>fw</VN_MAD>\n" + 
				"    <DESCRIPTION>Virtual network created by fake-user-id</DESCRIPTION>\n" +
				"    <NAME>%s</NAME>\n" + 
				"    <NETWORK_ADDRESS>10.10.10.0</NETWORK_ADDRESS>\n" + 
				"    <NETWORK_GATEWAY>10.10.10.1</NETWORK_GATEWAY>\n" + 
				"    <SECURITY_GROUPS>0,1</SECURITY_GROUPS>\n" + 
				"    <TYPE>RANGED</TYPE>\n" + 
				"</TEMPLATE>\n";
		
		return String.format(template, networkName);
	}
	
}
