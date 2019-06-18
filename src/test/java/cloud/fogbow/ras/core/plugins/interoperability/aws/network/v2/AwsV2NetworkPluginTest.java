package cloud.fogbow.ras.core.plugins.interoperability.aws.network.v2;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.models.linkedlists.SynchronizedDoublyLinkedList;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.models.NetworkAllocationMode;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.models.orders.OrderState;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2StateMapper;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.AssociateRouteTableRequest;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupRequest;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupResponse;
import software.amazon.awssdk.services.ec2.model.CreateSubnetRequest;
import software.amazon.awssdk.services.ec2.model.CreateSubnetResponse;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.DeleteSecurityGroupRequest;
import software.amazon.awssdk.services.ec2.model.DeleteSubnetRequest;
import software.amazon.awssdk.services.ec2.model.DescribeRouteTablesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsResponse;
import software.amazon.awssdk.services.ec2.model.Route;
import software.amazon.awssdk.services.ec2.model.RouteTable;
import software.amazon.awssdk.services.ec2.model.Subnet;
import software.amazon.awssdk.services.ec2.model.Tag;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ AwsV2ClientUtil.class, SharedOrderHolders.class })
public class AwsV2NetworkPluginTest {

	private static final String ANOTHER_VPC_ID = "another-vpc-id";
	private static final String ANY_VALUE = "anything";
	private static final String CLOUD_NAME = "amazon";
	private static final String DEFAULT_CIDR_DESTINATION = "0.0.0.0/0";
	private static final String FAKE_CIDR_ADDRESS = "1.0.1.0/28";
	private static final String FAKE_GROUP_ID = "fake-group-id";
	private static final String FAKE_INSTANCE_NAME = "fake-instance-name";
	private static final String FAKE_ROUTE_TABLE_ID = "fake-route-table-id";
	private static final String FAKE_SUBNET_ID = "fake-subnet-id";
	private static final String FAKE_VPC_ID = "fake-vpc-id";
	
	private AwsV2NetworkPlugin plugin;
	private SharedOrderHolders sharedOrderHolders;

	@Before
	public void setUp() {
		String awsConfFilePath = HomeDir.getPath() 
				+ SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME
				+ File.separator 
				+ CLOUD_NAME 
				+ File.separator 
				+ SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

		this.plugin = Mockito.spy(new AwsV2NetworkPlugin(awsConfFilePath));
		this.sharedOrderHolders = Mockito.mock(SharedOrderHolders.class);

		PowerMockito.mockStatic(SharedOrderHolders.class);
		BDDMockito.given(SharedOrderHolders.getInstance()).willReturn(this.sharedOrderHolders);

		Mockito.when(this.sharedOrderHolders.getOrdersList(Mockito.any(OrderState.class)))
				.thenReturn(new SynchronizedDoublyLinkedList<>());

		Mockito.when(this.sharedOrderHolders.getActiveOrdersMap()).thenReturn(new HashMap<>());
	}
	
	// test case: When calling the requestInstance method, with a network order and
	// cloud user valid, a client is invoked to create a network instance, returning
	// the network ID.
	@Test
	public void testRequestInstanceSuccessful() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		String vpcId = FAKE_VPC_ID;
		mockCreateSubnets(null, client);
		mockCreateRouteTables(vpcId, client);
		mockCreateSecurityGroups(client);

		NetworkOrder networkOrder = Mockito.spy(new NetworkOrder());
		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

		String expected = FAKE_SUBNET_ID;

		// exercise
		String networkId = this.plugin.requestInstance(networkOrder, cloudUser);

		// verify
		PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(1));
		AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString());

		Mockito.verify(client, Mockito.times(1)).createSubnet(Mockito.any(CreateSubnetRequest.class));
		Mockito.verify(client, Mockito.times(1)).associateRouteTable(Mockito.any(AssociateRouteTableRequest.class));
		Mockito.verify(client, Mockito.times(1)).createSecurityGroup(Mockito.any(CreateSecurityGroupRequest.class));
		Mockito.verify(client, Mockito.times(2)).createTags(Mockito.any(CreateTagsRequest.class));
		Mockito.verify(client, Mockito.times(1))
				.authorizeSecurityGroupIngress(Mockito.any(AuthorizeSecurityGroupIngressRequest.class));

		Assert.assertEquals(expected, networkId);
	}
	
	// test case: When calling the deleteInstance method, with a network order and
	// cloud user valid, the network instance in the cloud must be deleted.
	@Test
	public void testDeleteInstanceSuccessful() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		mockDescribeSubnets(client);

		NetworkOrder networkOrder = Mockito.spy(new NetworkOrder());
		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

		// exercise
		this.plugin.deleteInstance(networkOrder, cloudUser);

		// verify
		PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(1));
		AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString());
		
		Mockito.verify(client, Mockito.times(1)).describeSubnets(Mockito.any(DescribeSubnetsRequest.class));
		Mockito.verify(client, Mockito.times(1)).deleteSecurityGroup(Mockito.any(DeleteSecurityGroupRequest.class));
		Mockito.verify(client, Mockito.times(1)).deleteSubnet(Mockito.any(DeleteSubnetRequest.class));
	}
	
	// test case: When calling the getInstance method, with a network order and
	// cloud user valid, a client is invoked to request a network in the cloud, and
	// mount a network instance.
	@Test
	public void testGetInstanceSucessfull() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		String vpcId = FAKE_VPC_ID;
		mockCreateRouteTables(vpcId, client);
		mockDescribeSubnets(client);

		NetworkOrder networkOrder = Mockito.spy(new NetworkOrder());
		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

		NetworkInstance expected = createNetworkInstances();

		// exercise
		NetworkInstance instance = this.plugin.getInstance(networkOrder, cloudUser);

		// verify
		PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(1));
		AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString());

		Mockito.verify(client, Mockito.times(1)).describeSubnets(Mockito.any(DescribeSubnetsRequest.class));
		Mockito.verify(client, Mockito.times(1)).describeRouteTables();

		Assert.assertEquals(expected, instance);
	}

	// test case: When calling the isReady method with the cloud state AVAILABLE,
	// this means that the state of network is READY and it must return true.
	@Test
	public void testIsReadySuccessful() {
		// set up
		String cloudState = AwsV2StateMapper.AVAILABLE_STATE;

		// exercise
		boolean status = this.plugin.isReady(cloudState);

		// verify
		Assert.assertTrue(status);
	}

	// test case: When calling the isReady method with the cloud states different
	// than AVAILABLE, this means that the state of compute is not READY and it must
	// return false.
	@Test
	public void testIsReadyUnsuccessful() {
		// set up
		String[] cloudStates = { ANY_VALUE, AwsV2StateMapper.PENDING_STATE, AwsV2StateMapper.UNKNOWN_TO_SDK_VERSION_STATE };

		for (String cloudState : cloudStates) {
			// exercise
			boolean status = this.plugin.isReady(cloudState);

			// verify
			Assert.assertFalse(status);
		}
	}
		
	// test case: Whenever you call the hasFailed method, no matter the value, it
	// must return false.
	@Test
	public void testHasFailed() {
		// set up
		String cloudState = ANY_VALUE;

		// exercise
		boolean status = this.plugin.hasFailed(cloudState);

		// verify
		Assert.assertFalse(status);
	}
	
	// test case: When calling the getGatewayFromRouteTables method, and a route
	// list with a gateway ID different of local, it must return a default
	// destination CIDR block.
	@Test
	public void testGetGatewayFromRouteTablesSuccessful() {
		// set up
		String gatewayId = ANY_VALUE;
		String expected = DEFAULT_CIDR_DESTINATION;

		Route route = Route.builder()
				.gatewayId(gatewayId)
				.destinationCidrBlock(expected)
				.build();

		List<Route> routes = new ArrayList<>();
		routes.add(route);

		// exercise
		String gateway = this.plugin.getGatewayFromRouteTables(routes);

		// verify
		Assert.assertEquals(expected, gateway);
	}
	
	// test case: When calling the getGatewayFromRouteTables method, and a route
	// list with a gateway ID equals to local, it must return a null value.
	@Test
	public void testGetGatewayFromRouteTablesUnsuccessful() {
		// set up
		String gatewayId = AwsV2NetworkPlugin.LOCAL_GATEWAY_DESTINATION;
		String expected = null;

		Route route = Route.builder()
				.gatewayId(gatewayId)
				.build();

		List<Route> routes = new ArrayList<>();
		routes.add(route);

		// exercise
		String gateway = this.plugin.getGatewayFromRouteTables(routes);

		// verify
		Assert.assertEquals(expected, gateway);
	}
	
	// test case: When calling the getGroupIdBySubnet method, with a valid subnet ID
	// and client, and a key tag different of groupId, the UnexpectedException will
	// be thrown.
	@Test(expected = UnexpectedException.class)
	public void testGetGroupIdBySubnetUnsuccessful()
			throws InvalidParameterException, UnexpectedException, InstanceNotFoundException {

		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		String subnetId = FAKE_SUBNET_ID;
		String key = ANY_VALUE;
		String value = ANY_VALUE;
		Tag tag = buildTags(key, value);
		Subnet subnet = buildSubnet(subnetId, tag);
		Mockito.doReturn(subnet).when(this.plugin).getSubnetById(subnetId, client);

		// exercise
		this.plugin.getGroupIdBySubnet(subnetId, client);
	}

	// test case: When calling the getSubnetById method, with a valid client, and a
	// subnet ID invalid, the InstanceNotFoundException will be thrown.
	@Test(expected = InstanceNotFoundException.class) // verify
	public void testGetSubnetByIdUnsuccessful()
			throws InvalidParameterException, UnexpectedException, InstanceNotFoundException {
		
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		String subnetId = FAKE_SUBNET_ID;

		// exercise
		this.plugin.getSubnetById(subnetId, client);
	}
	
	// test case: When calling the doDescribeSubnets method, and an error occurs
	// during the request, the UnexpectedException will be thrown.
	@Test(expected = UnexpectedException.class) // verify
	public void testDoDescribeSubnetsUnsuccessful() throws InvalidParameterException, UnexpectedException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		Mockito.when(client.describeSubnets(Mockito.any(DescribeSubnetsRequest.class)))
				.thenThrow(SdkClientException.builder().build());

		String subnetId = FAKE_SUBNET_ID;

		// exercise
		this.plugin.doDescribeSubnets(subnetId, client);
	}
	
	// test case: When calling the handleSecurityIssues method, without a valid
	// security group request, the UnexpectedException will be thrown.
	@Test(expected = UnexpectedException.class) // verify
	public void testHandleSecurityIssuesUnsuccessful()
			throws InvalidParameterException, UnexpectedException, InstanceNotFoundException {
		
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		String cidr = FAKE_CIDR_ADDRESS;
		String subnetId = FAKE_SUBNET_ID;

		// exercise
		this.plugin.handleSecurityIssues(cidr, subnetId, client);
	}
	
	// test case: When calling the doAuthorizeSecurityGroupIngress method, and an
	// error occurs during the request, the UnexpectedException will be thrown.
	@Test(expected = UnexpectedException.class) // verify
	public void testDoAuthorizeSecurityGroupIngressUnsuccessful() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		Mockito.when(client.authorizeSecurityGroupIngress(Mockito.any(AuthorizeSecurityGroupIngressRequest.class)))
				.thenThrow(SdkClientException.builder().build());

		String cidr = FAKE_CIDR_ADDRESS;
		String subnetId = FAKE_SUBNET_ID;
		String groupId = FAKE_GROUP_ID;

		// exercise
		this.plugin.doAuthorizeSecurityGroupIngress(cidr, subnetId, groupId, client);
	}
	
	// test case: When calling the doDeleteSecurityGroups method, and an error
	// occurs during the request, the UnexpectedException will be thrown.
	@Test(expected = UnexpectedException.class) // verify
	public void testDoDeleteSecurityGroupsUnsuccessful() throws InvalidParameterException, UnexpectedException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		Mockito.when(client.deleteSecurityGroup(Mockito.any(DeleteSecurityGroupRequest.class)))
				.thenThrow(SdkClientException.builder().build());

		String groupId = FAKE_GROUP_ID;

		// exercise
		this.plugin.doDeleteSecurityGroups(groupId, client);
	}
	
	// test case: When calling the doDeleteSubnets method, and an error
	// occurs during the request, the UnexpectedException will be thrown.
	@Test(expected = UnexpectedException.class) // verify
	public void testDoDeleteSubnetUnsuccessful()
			throws InvalidParameterException, UnexpectedException, InstanceNotFoundException {

		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		Mockito.when(client.deleteSubnet(Mockito.any(DeleteSubnetRequest.class)))
				.thenThrow(SdkClientException.builder().build());

		String subnetId = FAKE_SUBNET_ID;

		// exercise
		this.plugin.doDeleteSubnets(subnetId, client);
	}
	
	// test case: When calling the doAssociateRouteTables method, and an error
	// occurs during the request, the UnexpectedException will be thrown.
	@Test(expected = UnexpectedException.class) // verify
	public void testdoAssociateRouteTablesUnsuccessful()
			throws InvalidParameterException, UnexpectedException, InstanceNotFoundException {

		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		String vpcId = FAKE_VPC_ID;
		mockCreateRouteTables(vpcId, client);
		Mockito.when(client.associateRouteTable(Mockito.any(AssociateRouteTableRequest.class)))
				.thenThrow(SdkClientException.builder().build());

		String subnetId = FAKE_SUBNET_ID;

		// exercise
		this.plugin.doAssociateRouteTables(subnetId, client);
	}
	
	// test case: When calling the getRouteTables method, and not find a route with
	// VPC ID equal to the default, the UnexpectedException will be thrown.
	@Test(expected = InstanceNotFoundException.class) // verify
	public void testGetRouteTablesUnsuccessful()
			throws InvalidParameterException, UnexpectedException, InstanceNotFoundException {
		
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		String vpcId = ANOTHER_VPC_ID;
		mockCreateRouteTables(vpcId, client);

		// exercise
		this.plugin.getRouteTables(client);
	}
	
	// test case: When calling the doDescribeRouteTables method, and an error
	// occurs during the request, the UnexpectedException will be thrown.
	@Test(expected = UnexpectedException.class) // verify
	public void testDoDescribeRouteTablesUnsuccessful() throws InvalidParameterException, UnexpectedException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		Mockito.when(client.describeRouteTables()).thenThrow(SdkClientException.builder().build());

		// exercise
		this.plugin.doDescribeRouteTables(client);
	}
	
	// test case: When calling the doCreateSubnetResquests method, without a valid
	// subnet request, the UnexpectedException will be thrown.
	@Test(expected = UnexpectedException.class) // verify
	public void testDoCreateSubnetResquestsUnsuccessful() throws InvalidParameterException, UnexpectedException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		Mockito.when(client.createSubnet(Mockito.any(CreateSubnetRequest.class)))
				.thenThrow(SdkClientException.builder().build());

		String name = null;
		CreateSubnetRequest request = null;

		// exercise
		this.plugin.doCreateSubnetResquests(name, request, client);
	}
	
	// test case: When calling the doCreateTagsRequests method, without a valid
	// tag request, the UnexpectedException will be thrown.
	@Test(expected = UnexpectedException.class) // verify
	public void testDoCreateTagsRequests() throws InvalidParameterException, UnexpectedException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		Mockito.when(client.createTags(Mockito.any(CreateTagsRequest.class)))
				.thenThrow(SdkClientException.builder().build());

		String key = null;
		String value = null;
		String resourceId = null;

		// exercise
		this.plugin.doCreateTagsRequests(key, value, resourceId, client);
	}
	
	// test case: When calling the defineInstanceName method passing the instance
	// name by parameter, it must return this same name.
	@Test
	public void testDefineInstanceNameByParameterRequested() {
		// set up
		String expected = FAKE_INSTANCE_NAME;

		// exercise
		String instanceName = this.plugin.defineInstanceName(FAKE_INSTANCE_NAME);

		// verify
		Assert.assertEquals(expected, instanceName);
	}
	
	private NetworkInstance createNetworkInstances() {
		Mockito.doReturn(FAKE_INSTANCE_NAME).when(this.plugin).getRandomUUID();

		String id = FAKE_SUBNET_ID;
		String cloudState = AwsV2StateMapper.AVAILABLE_STATE;
		String name = SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + FAKE_INSTANCE_NAME;
		String cidr = FAKE_CIDR_ADDRESS;
		String gateway = DEFAULT_CIDR_DESTINATION;
		String vLAN = null;
		NetworkAllocationMode networkAllocationMode = NetworkAllocationMode.DYNAMIC;
		String networkInterface = null;
		String MACInterface = null;
		String interfaceState = null;
		return new NetworkInstance(id, cloudState, name, cidr, gateway, vLAN, networkAllocationMode, networkInterface,
				MACInterface, interfaceState);
	}
	
	private void mockDescribeSubnets(Ec2Client client) {
		String key = AwsV2NetworkPlugin.AWS_TAG_GROUP_ID;
		String value = FAKE_GROUP_ID;
		Tag tag = buildTags(key, value);
		
		String cidrBlock = FAKE_CIDR_ADDRESS;
		String state = AwsV2StateMapper.AVAILABLE_STATE;
		String subnetId = FAKE_SUBNET_ID;
		String vpcId = FAKE_VPC_ID;
		
		Subnet subnet = Subnet.builder()
				.cidrBlock(cidrBlock)
				.state(state)
				.subnetId(subnetId)
				.tags(tag)
				.vpcId(vpcId)
				.build();
		
		DescribeSubnetsResponse response = DescribeSubnetsResponse.builder()
				.subnets(subnet)
				.build();
		
		Mockito.when(client.describeSubnets(Mockito.any(DescribeSubnetsRequest.class))).thenReturn(response);
	}

	private Tag buildTags(String key, String value) {
		Tag tag = Tag.builder()
				.key(key)
				.value(value)
				.build();

		return tag;
	}

	private void mockCreateSecurityGroups(Ec2Client client) {
		String groupId = FAKE_GROUP_ID;
		
		CreateSecurityGroupResponse response = CreateSecurityGroupResponse.builder()
				.groupId(groupId)
				.build();
		
		Mockito.when(client.createSecurityGroup(Mockito.any(CreateSecurityGroupRequest.class))).thenReturn(response);
	}

	private void mockCreateRouteTables(String vpcId, Ec2Client client) {
		String routeTableId = FAKE_ROUTE_TABLE_ID;
		
		RouteTable routeTable = RouteTable.builder()
				.routeTableId(routeTableId)
				.vpcId(vpcId)
				.build();
		
		DescribeRouteTablesResponse response = DescribeRouteTablesResponse.builder()
				.routeTables(routeTable)
				.build();
		
		Mockito.when(client.describeRouteTables()).thenReturn(response);
	}

	private void mockCreateSubnets(Tag tag, Ec2Client client) {
		String subnetId = FAKE_SUBNET_ID;
		Subnet subnet = buildSubnet(subnetId, tag);
		
		CreateSubnetResponse response = CreateSubnetResponse.builder()
				.subnet(subnet)
				.build();
		
		Mockito.when(client.createSubnet(Mockito.any(CreateSubnetRequest.class))).thenReturn(response);
	}

	private Subnet buildSubnet(String subnetId, Tag tag) {
		Subnet subnet = Subnet.builder()
				.subnetId(subnetId)
				.tags(tag)
				.build();
		
		return subnet;
	}
	
}
