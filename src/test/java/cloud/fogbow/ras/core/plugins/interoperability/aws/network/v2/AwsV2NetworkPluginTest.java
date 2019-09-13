package cloud.fogbow.ras.core.plugins.interoperability.aws.network.v2;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.NetworkAllocationMode;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2CloudUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2StateMapper;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.CreateSubnetRequest;
import software.amazon.awssdk.services.ec2.model.DescribeRouteTablesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsResponse;
import software.amazon.awssdk.services.ec2.model.Route;
import software.amazon.awssdk.services.ec2.model.RouteTable;
import software.amazon.awssdk.services.ec2.model.Subnet;
import software.amazon.awssdk.services.ec2.model.Tag;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ AwsV2ClientUtil.class, AwsV2CloudUtil.class, DatabaseManager.class })
public class AwsV2NetworkPluginTest extends BaseUnitTests {

	private static final String ANY_VALUE = "anything";
	private static final String CLOUD_NAME = "amazon";
	private static final String DEFAULT_AVAILABILITY_ZONE = "sa-east-1a";
	private static final String DEFAULT_CIDR_DESTINATION = "0.0.0.0/0";
	private static final String FAKE_CIDR_ADDRESS = "1.0.1.0/28";
	private static final String FAKE_GROUP_ID = "fake-group-id";
	private static final String FAKE_INSTANCE_NAME = "fake-instance-name";
	private static final String FAKE_SUBNET_ID = "fake-subnet-id";
	private static final String FAKE_VPC_ID = "fake-vpc-id";
	
	private AwsV2NetworkPlugin plugin;
	private Ec2Client client;

	@Before
	public void setUp() throws FogbowException {
		this.testUtils.mockReadOrdersFromDataBase();
	    String awsConfFilePath = HomeDir.getPath() 
				+ SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME
				+ File.separator 
				+ CLOUD_NAME 
				+ File.separator 
				+ SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

		this.plugin = Mockito.spy(new AwsV2NetworkPlugin(awsConfFilePath));
		this.client = this.testUtils.getAwsMockedClient();
	}
	
    // test case: When calling the requestInstance method, with a network order and
    // cloud user valid, a client is invoked to create a network instance, returning
    // the network ID.
    @Test
    public void testRequestInstance() throws FogbowException {
        // set up
        NetworkOrder order = this.testUtils.createLocalNetworkOrder();
        String vpcId = FAKE_VPC_ID;

        CreateSubnetRequest request = CreateSubnetRequest.builder()
                .availabilityZone(DEFAULT_AVAILABILITY_ZONE)
                .cidrBlock(order.getCidr())
                .vpcId(vpcId)
                .build();

        Mockito.doReturn(TestUtils.FAKE_INSTANCE_ID).when(this.plugin).doRequestInstance(Mockito.eq(order.getName()),
                Mockito.eq(request), Mockito.eq(this.client));

        AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

        // exercise
        this.plugin.requestInstance(order, cloudUser);

        // verify
        PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2ClientUtil.createEc2Client(Mockito.eq(cloudUser.getToken()), Mockito.anyString());

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doRequestInstance(Mockito.eq(order.getName()),
                Mockito.eq(request), Mockito.eq(this.client));
    }
    
    // test case: When calling the getInstance method, with a network order and
    // cloud user valid, a client is invoked to request a network in the cloud, and
    // mount a network instance.
    @Test
    public void testGetInstance() throws FogbowException {
        // set up
        NetworkOrder order = this.testUtils.createLocalNetworkOrder();
        AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

        NetworkInstance instance = createNetworkInstances();
        Mockito.doReturn(instance).when(this.plugin).doGetInstance(Mockito.eq(order.getInstanceId()),
                Mockito.eq(this.client));

        // exercise
        this.plugin.getInstance(order, cloudUser);

        // verify
        PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2ClientUtil.createEc2Client(Mockito.eq(cloudUser.getToken()), Mockito.anyString());

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetInstance(Mockito.eq(order.getInstanceId()),
                Mockito.eq(this.client));
    }
	
    // test case: When calling the deleteInstance method, with a network order and
    // cloud user valid, the network instance in the cloud must be deleted.
    @Test
    public void testDeleteInstance() throws FogbowException {
        // set up
        NetworkOrder order = this.testUtils.createLocalNetworkOrder();
        AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

        Mockito.doNothing().when(this.plugin).doDeleteInstance(Mockito.eq(order.getInstanceId()),
                Mockito.eq(this.client));

        // exercise
        this.plugin.deleteInstance(order, cloudUser);

        // verify
        PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2ClientUtil.createEc2Client(Mockito.eq(cloudUser.getToken()), Mockito.anyString());

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doDeleteInstance(Mockito.eq(order.getInstanceId()), Mockito.eq(this.client));
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
	
    // test case: When calling the doDeleteInstance method, it must verify
    // that is call was successful.
    @Test
    public void testDoDeleteInstance() throws Exception {
        // setup
        String groupId = FAKE_GROUP_ID;
        String subnetId = FAKE_SUBNET_ID;
        Subnet subnet = buildSubnet();

        Mockito.doReturn(subnet).when(this.plugin).getSubnetById(Mockito.eq(subnetId), Mockito.eq(this.client));
        Mockito.doReturn(groupId).when(this.plugin).getGroupIdFrom(Mockito.eq(subnet));

        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        PowerMockito.doNothing().when(AwsV2CloudUtil.class, TestUtils.DO_DELETE_SECURITY_GROUP_METHOD,
                Mockito.eq(groupId), Mockito.eq(this.client));

        Mockito.doNothing().when(this.plugin).doDeleteSubnet(Mockito.eq(subnetId), Mockito.eq(this.client));

        // exercise
        plugin.doDeleteInstance(subnetId, this.client);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getSubnetById(Mockito.eq(subnetId),
                Mockito.eq(this.client));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getGroupIdFrom(Mockito.eq(subnet));

        PowerMockito.verifyStatic(AwsV2CloudUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.doDeleteSecurityGroup(Mockito.eq(groupId), Mockito.eq(this.client));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doDeleteSubnet(Mockito.eq(subnetId),
                Mockito.eq(this.client));
    }
    
    // test case: When calling the getGroupIdFrom method with a valid sub-net, it
    // must return the group ID of this sub-net.
    @Test
    public void testGetGroupIdFromValidSubnet() throws FogbowException {
        // set up
        Subnet subnet = buildSubnet();

        String expected = FAKE_GROUP_ID;

        // exercise
        String groupId = this.plugin.getGroupIdFrom(subnet);

        // verify
        Assert.assertEquals(expected, groupId);
    }
    
    // test case: When calling the getGroupIdFrom method with a invalid sub-net, the
    // UnexpectedException will be thrown.
    @Test
    public void testGetGroupIdFromInvalidSubnet() {
        // set up
        Tag tag = buildTags(ANY_VALUE, ANY_VALUE);
        Subnet subnet = buildSubnet(tag);

        String expected = Messages.Exception.UNEXPECTED_ERROR;

        try {
            // exercise
            this.plugin.getGroupIdFrom(subnet);
            Assert.fail();
        } catch (Exception e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the doDescribeRouteTables method, it must verify
    // that is call was successful.
    @Test
    public void testDoDescribeRouteTables() throws FogbowException {
        // set up
        DescribeRouteTablesResponse response = buildDescribeRouteTablesResponse();
        Mockito.doReturn(response).when(this.client).describeRouteTables();
        
        // exercise
        this.plugin.doDescribeRouteTables(this.client);
        
        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).describeRouteTables();
    }
    
    // test case: When calling the doDescribeRouteTables method, and an unexpected error
    // occurs, it must verify if an UnexpectedException has been thrown.
    @Test
    public void testDoDescribeRouteTablesFail() throws FogbowException {
        // set up
        SdkClientException exception = SdkClientException.builder().build();
        Mockito.doThrow(SdkClientException.builder().build()).when(this.client).describeRouteTables();

        String expected = String.format(Messages.Exception.GENERIC_EXCEPTION, exception);
        
        try {
            // exercise
            this.plugin.doDescribeRouteTables(this.client);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the getSubnetFrom method with a valid response, it
    // must verify that is call was successful.
    @Test
    public void testGetSubnetFrom() throws FogbowException {
        // set up
        DescribeSubnetsResponse response = buildDescribeSubnetsResponse();

        Subnet expected = buildSubnet();

        // exercise
        Subnet subnet = this.plugin.getSubnetFrom(response);

        // verify
        Assert.assertEquals(expected, subnet);
    }
    
    // test case: When calling the getSubnetFrom method with a null response, it
    // must verify that an InstanceNotFoundException has been thrown.
    @Test
    public void testGetSubnetFromNullResponse() throws FogbowException {
        // set up
        DescribeSubnetsResponse response = null;

        String expected = Messages.Exception.INSTANCE_NOT_FOUND;

        try {
            // exercise
            this.plugin.getSubnetFrom(response);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the getSubnetFrom method with a empty response, it
    // must verify that an InstanceNotFoundException has been thrown.
    @Test
    public void testGetSubnetFromEmptyResponse() throws FogbowException {
        // set up
        DescribeSubnetsResponse response = DescribeSubnetsResponse.builder().build();

        String expected = Messages.Exception.INSTANCE_NOT_FOUND;

        try {
            // exercise
            this.plugin.getSubnetFrom(response);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the getSubnetFrom method, it must verify that is call
    // was successful.
    @Test
    public void testDoGetInstance() throws FogbowException {
        // set up
        String subnetId = FAKE_SUBNET_ID;
        Subnet subnet = buildSubnet();
        Mockito.doReturn(subnet).when(this.plugin).getSubnetById(Mockito.eq(subnetId), Mockito.eq(this.client));

        RouteTable routeTable = buildRouteTables();
        Mockito.doReturn(routeTable).when(this.plugin).getRouteTables(Mockito.eq(client));

        // exercise
        this.plugin.doGetInstance(subnetId, this.client);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getSubnetById(Mockito.eq(subnetId),
                Mockito.eq(this.client));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getRouteTables(Mockito.eq(client));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).buildNetworkInstance(Mockito.eq(subnet),
                Mockito.eq(routeTable));
    }
    
    // test case: When calling the getGatewayFromRouteTables method, it must verify
    // that is call was successful.
    @Test
    public void testGetGatewayFromRouteTables() {
        // set up
        List<Route> routes = createRouteCollection();

        String expected = DEFAULT_CIDR_DESTINATION;

        // exercise
        String destinationAddress = this.plugin.getGatewayFromRouteTables(routes);

        // verify
        Assert.assertEquals(expected, destinationAddress);
    }
    
    // test case: When calling the getGatewayFromRouteTables method, it must verify
    // that is call was successful.
    @Test
    public void testGetSubnetById() throws FogbowException {
        // set up
        String subnetId = FAKE_SUBNET_ID;

        DescribeSubnetsRequest request = DescribeSubnetsRequest.builder()
                .subnetIds(subnetId)
                .build();

        DescribeSubnetsResponse response = buildDescribeSubnetsResponse();
        Mockito.doReturn(response).when(this.plugin).doDescribeSubnetsRequest(Mockito.eq(request),
                Mockito.eq(this.client));
        
        

        // exercise
        this.plugin.getSubnetById(subnetId, this.client);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doDescribeSubnetsRequest(Mockito.eq(request),
                Mockito.eq(this.client));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getSubnetFrom(Mockito.eq(response));
    }
    
    // test case: When calling the doDescribeSubnetsRequest method, it must verify
    // that is call was successful.
    @Test
    public void testDoDescribeSubnetsRequest() throws FogbowException {
        // set up
        String subnetId = FAKE_SUBNET_ID;

        DescribeSubnetsRequest request = DescribeSubnetsRequest.builder()
                .subnetIds(subnetId)
                .build();

        DescribeSubnetsResponse response = buildDescribeSubnetsResponse();
        Mockito.doReturn(response).when(this.client).describeSubnets(Mockito.eq(request));

        // exercise
        this.plugin.doDescribeSubnetsRequest(request, this.client);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).describeSubnets(Mockito.eq(request));
    }
    
    // test case: When calling the doDescribeSubnetsRequest method, and an
    // unexpected error occurs, it must verify if an UnexpectedException has been
    // thrown.
    @Test
    public void testDoDescribeSubnetsRequestFail() throws FogbowException {
        // set up
        String subnetId = FAKE_SUBNET_ID;

        DescribeSubnetsRequest request = DescribeSubnetsRequest.builder()
                .subnetIds(subnetId)
                .build();

        SdkClientException exception = SdkClientException.builder().build();
        Mockito.doThrow(exception).when(this.client).describeSubnets(Mockito.eq(request));

        String expected = String.format(Messages.Exception.GENERIC_EXCEPTION, exception);
        
        try {
            // exercise
            this.plugin.doDescribeSubnetsRequest(request, this.client);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the doRequestInstance method, it must verify
    // that is call was successful.
    @Test
    public void testDoRequestInstance() throws FogbowException {
        // set up
        String cidr = FAKE_CIDR_ADDRESS;
        String instanceName = TestUtils.FAKE_INSTANCE_NAME;
        String subnetId = FAKE_SUBNET_ID;

        CreateSubnetRequest request = CreateSubnetRequest.builder().availabilityZone(DEFAULT_AVAILABILITY_ZONE)
                .cidrBlock(cidr).vpcId(FAKE_VPC_ID).build();

        Mockito.doReturn(subnetId).when(this.plugin).doCreateSubnetResquest(Mockito.eq(instanceName),
                Mockito.eq(request), Mockito.eq(this.client));
        Mockito.doNothing().when(this.plugin).doAssociateRouteTables(Mockito.eq(subnetId), Mockito.eq(this.client));
        Mockito.doNothing().when(this.plugin).handleSecurityIssues(Mockito.eq(subnetId), Mockito.eq(cidr),
                Mockito.eq(this.client));

        // exercise
        this.plugin.doRequestInstance(instanceName, request, this.client);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doCreateSubnetResquest(Mockito.eq(instanceName),
                Mockito.eq(request), Mockito.eq(this.client));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doAssociateRouteTables(Mockito.eq(subnetId),
                Mockito.eq(this.client));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).handleSecurityIssues(Mockito.eq(subnetId),
                Mockito.eq(cidr), Mockito.eq(this.client));
    }
    
    // test case: When calling the handleSecurityIssues method, it must verify
    // that is call was successful.
    @Test
    public void testHandleSecurityIssues() throws Exception {
        // set up
        String subnetId = FAKE_SUBNET_ID;
        String cidr = FAKE_CIDR_ADDRESS;
        String descrition = AwsV2NetworkPlugin.SECURITY_GROUP_DESCRIPTION;
        String groupId = FAKE_GROUP_ID;
        String groupName = SystemConstants.PN_SECURITY_GROUP_PREFIX + subnetId;
        String tagKey = AwsV2CloudUtil.AWS_TAG_GROUP_ID;
        String vpcId = FAKE_VPC_ID;

        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        PowerMockito.doReturn(groupId).when(AwsV2CloudUtil.class, TestUtils.CREATE_SECURITY_GROUP_METHOD,
                Mockito.eq(vpcId), Mockito.eq(groupName), Mockito.eq(descrition), Mockito.eq(this.client));

        AuthorizeSecurityGroupIngressRequest request = AuthorizeSecurityGroupIngressRequest.builder()
                .cidrIp(cidr)
                .groupId(groupId)
                .ipProtocol(AwsV2NetworkPlugin.ALL_PROTOCOLS)
                .build();

        PowerMockito.doNothing().when(AwsV2CloudUtil.class, TestUtils.DO_AUTHORIZE_SECURITY_GROUP_INGRESS_METHOD, Mockito.eq(request),
                Mockito.eq(this.client));
        
        PowerMockito.doNothing().when(AwsV2CloudUtil.class, TestUtils.CREATE_TAGS_REQUEST_METHOD, Mockito.eq(subnetId),
                Mockito.eq(tagKey), Mockito.eq(groupId), Mockito.eq(this.client));

        // exercise
        this.plugin.handleSecurityIssues(subnetId, cidr, this.client);

        // verify
        PowerMockito.verifyStatic(AwsV2CloudUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.createSecurityGroup(Mockito.eq(vpcId), Mockito.eq(groupName), Mockito.eq(descrition),
                Mockito.eq(this.client));

        PowerMockito.verifyStatic(AwsV2CloudUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.doAuthorizeSecurityGroupIngress(Mockito.eq(request), Mockito.eq(this.client));

        PowerMockito.verifyStatic(AwsV2CloudUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.createTagsRequest(Mockito.eq(subnetId), Mockito.eq(tagKey), Mockito.eq(groupId),
                Mockito.eq(this.client));
    }

    private List<Route> createRouteCollection() {
        Route[] routes = { 
                Route.builder()
                    .gatewayId(TestUtils.FAKE_GATEWAY)
                    .destinationCidrBlock(DEFAULT_CIDR_DESTINATION)
                    .build() 
        };
        return Arrays.asList(routes);
    }
	
	private DescribeSubnetsResponse buildDescribeSubnetsResponse() {
        DescribeSubnetsResponse response = DescribeSubnetsResponse.builder()
                .subnets(buildSubnet())
                .build();
        
        return response;
    }

	private DescribeRouteTablesResponse buildDescribeRouteTablesResponse() {
	    DescribeRouteTablesResponse response = DescribeRouteTablesResponse.builder()
	            .routeTables(buildRouteTables())
	            .build();
	    
	    return response;
	}

    private RouteTable buildRouteTables() {
        RouteTable routeTable = RouteTable.builder()
	            .routeTableId(AwsV2NetworkPlugin.LOCAL_GATEWAY_DESTINATION)
	            .vpcId(FAKE_VPC_ID)
	            .build();
        
        return routeTable;
    }
	
	private Subnet buildSubnet() {
	    return buildSubnet(null);
	}

    private Subnet buildSubnet(Tag tag) {
        String key = AwsV2CloudUtil.AWS_TAG_GROUP_ID;
        String value = FAKE_GROUP_ID;

        Subnet subnet = Subnet.builder()
                .subnetId(FAKE_SUBNET_ID)
                .tags(tag == null ? buildTags(key, value) : tag)
                .build();

        return subnet;
    }
	
    private Tag buildTags(String key, String value) {
        Tag tag = Tag.builder()
                .key(key)
                .value(value)
                .build();

        return tag;
    }
    
    private NetworkInstance createNetworkInstances() {
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
	
}
