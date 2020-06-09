package cloud.fogbow.ras.core.plugins.interoperability.aws.network.v2;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

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
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.AssociateRouteTableRequest;
import software.amazon.awssdk.services.ec2.model.AttachInternetGatewayRequest;
import software.amazon.awssdk.services.ec2.model.AttachInternetGatewayResponse;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.CreateInternetGatewayResponse;
import software.amazon.awssdk.services.ec2.model.CreateRouteRequest;
import software.amazon.awssdk.services.ec2.model.CreateRouteResponse;
import software.amazon.awssdk.services.ec2.model.CreateSubnetRequest;
import software.amazon.awssdk.services.ec2.model.CreateSubnetResponse;
import software.amazon.awssdk.services.ec2.model.CreateVpcRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpcResponse;
import software.amazon.awssdk.services.ec2.model.DeleteInternetGatewayRequest;
import software.amazon.awssdk.services.ec2.model.DeleteInternetGatewayResponse;
import software.amazon.awssdk.services.ec2.model.DeleteSubnetRequest;
import software.amazon.awssdk.services.ec2.model.DeleteSubnetResponse;
import software.amazon.awssdk.services.ec2.model.DeleteVpcRequest;
import software.amazon.awssdk.services.ec2.model.DeleteVpcResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInternetGatewaysResponse;
import software.amazon.awssdk.services.ec2.model.DescribeRouteTablesResponse;
import software.amazon.awssdk.services.ec2.model.DetachInternetGatewayRequest;
import software.amazon.awssdk.services.ec2.model.DetachInternetGatewayResponse;
import software.amazon.awssdk.services.ec2.model.InternetGateway;
import software.amazon.awssdk.services.ec2.model.InternetGatewayAttachment;
import software.amazon.awssdk.services.ec2.model.ModifyVpcAttributeRequest;
import software.amazon.awssdk.services.ec2.model.ModifyVpcAttributeResponse;
import software.amazon.awssdk.services.ec2.model.Route;
import software.amazon.awssdk.services.ec2.model.RouteTable;
import software.amazon.awssdk.services.ec2.model.Subnet;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.Vpc;

@PrepareForTest({ AwsV2ClientUtil.class, AwsV2CloudUtil.class, DatabaseManager.class })
public class AwsNetworkPluginTest extends BaseUnitTests {

    private static final String ANOTHER_VPC_ID = "another-vpc-id";
    private static final String ANY_VALUE = "anything";
    private static final String CLOUD_NAME = "amazon";
    private static final String DEFAULT_AVAILABILITY_ZONE = "sa-east-1a";
    private static final String DEFAULT_CIDR_DESTINATION = "0.0.0.0/0";
    private static final String FAKE_CIDR_ADDRESS = "1.0.1.0/28";
    private static final String FAKE_GATEWAY_ID = "fake-gateway-id";
    private static final String FAKE_GROUP_ID = "fake-group-id";
    private static final String FAKE_INSTANCE_NAME = "fake-instance-name";
    private static final String FAKE_ROUTE_TABLE_ID = "fake-route-table-id";
    private static final String FAKE_SUBNET_ID = "fake-subnet-id";
    private static final String FAKE_VPC_ID = "fake-vpc-id";
	
    private AwsNetworkPlugin plugin;
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

        this.plugin = Mockito.spy(new AwsNetworkPlugin(awsConfFilePath));
        this.client = this.testUtils.getAwsMockedClient();
    }
	
    // test case: When calling the requestInstance method, with a network order and
    // cloud user valid, a client is invoked to create a network instance, returning
    // the network ID.
    @Test
    public void testRequestInstance() throws FogbowException {
        // set up
        NetworkOrder order = this.testUtils.createLocalNetworkOrder();
        String cidr = order.getCidr();

        String vpcId = FAKE_VPC_ID;
        Mockito.doReturn(vpcId).when(this.plugin).doCreateAndConfigureVpc(Mockito.eq(cidr),
                Mockito.eq(this.client));

        CreateSubnetRequest request = CreateSubnetRequest.builder()
                .availabilityZone(DEFAULT_AVAILABILITY_ZONE)
                .cidrBlock(cidr)
                .vpcId(vpcId)
                .build();

        String instanceName = FAKE_INSTANCE_NAME;
        Mockito.doReturn(TestUtils.FAKE_INSTANCE_ID).when(this.plugin).doRequestInstance(Mockito.eq(instanceName),
                Mockito.eq(request), Mockito.eq(this.client));

        AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

        // exercise
        this.plugin.requestInstance(order, cloudUser);

        // verify
        PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2ClientUtil.createEc2Client(Mockito.eq(cloudUser.getToken()), Mockito.anyString());

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doRequestInstance(Mockito.eq(instanceName),
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
    public void testIsReady() {
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
    public void testIsNotReady() {
        // set up
        String[] cloudStates = { ANY_VALUE, AwsV2StateMapper.PENDING_STATE,
                AwsV2StateMapper.UNKNOWN_TO_SDK_VERSION_STATE };

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

        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        PowerMockito.doReturn(subnet).when(AwsV2CloudUtil.class, TestUtils.GET_SUBNET_BY_ID_METHOD,
                Mockito.eq(subnetId), Mockito.eq(this.client));
        
        PowerMockito.doReturn(groupId).when(AwsV2CloudUtil.class, TestUtils.GET_GROUP_ID_FROM_METHOD,
                Mockito.eq(subnet.tags()));
        
        PowerMockito.doNothing().when(AwsV2CloudUtil.class, TestUtils.DO_DELETE_SECURITY_GROUP_METHOD,
                Mockito.eq(groupId), Mockito.eq(this.client));

        Mockito.doNothing().when(this.plugin).doDeleteSubnet(Mockito.eq(subnetId), Mockito.eq(this.client));

        String vpcId = FAKE_VPC_ID;
        String gatewayId = FAKE_GATEWAY_ID;
        Mockito.doReturn(gatewayId).when(this.plugin).getGatewayIdAttachedToVpc(Mockito.eq(vpcId), Mockito.eq(this.client));
        Mockito.doNothing().when(this.plugin).doRollbackAllConfigurationAndDeleteVpc(Mockito.eq(gatewayId),
                Mockito.eq(vpcId), Mockito.eq(this.client));

        // exercise
        plugin.doDeleteInstance(subnetId, this.client);

        // verify
        PowerMockito.verifyStatic(AwsV2CloudUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.getSubnetById(Mockito.eq(subnetId), Mockito.eq(this.client));
        
        PowerMockito.verifyStatic(AwsV2CloudUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.getGroupIdFrom(Mockito.eq(subnet.tags()));

        PowerMockito.verifyStatic(AwsV2CloudUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.doDeleteSecurityGroup(Mockito.eq(groupId), Mockito.eq(this.client));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doDeleteSubnet(Mockito.eq(subnetId),
                Mockito.eq(this.client));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getGatewayIdAttachedToVpc(Mockito.eq(vpcId),
                Mockito.eq(this.client));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doRollbackAllConfigurationAndDeleteVpc(Mockito.eq(gatewayId), Mockito.eq(vpcId), Mockito.eq(this.client));
    }
    
    // test case: When calling the getSubnetFrom method, it must verify that is call
    // was successful.
    @Test
    public void testDoGetInstance() throws Exception {
        // set up
        String subnetId = FAKE_SUBNET_ID;
        Subnet subnet = buildSubnet();
        
        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        PowerMockito.doReturn(subnet).when(AwsV2CloudUtil.class, TestUtils.GET_SUBNET_BY_ID_METHOD,
                Mockito.eq(subnetId), Mockito.eq(this.client));

        String vpcId = FAKE_VPC_ID;
        RouteTable routeTable = buildRouteTables();
        Mockito.doReturn(routeTable).when(this.plugin).getRouteTables(Mockito.eq(vpcId), Mockito.eq(client));

        // exercise
        this.plugin.doGetInstance(subnetId, this.client);

        // verify
        PowerMockito.verifyStatic(AwsV2CloudUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.getSubnetById(Mockito.eq(subnetId), Mockito.eq(this.client));
        
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getRouteTables(Mockito.eq(vpcId),
                Mockito.eq(client));
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
    
    // test case: When calling the doRequestInstance method, it must verify
    // that is call was successful.
    @Test
    public void testDoRequestInstance() throws FogbowException {
        // set up
        NetworkOrder order = this.testUtils.createLocalNetworkOrder();
        String cidr = FAKE_CIDR_ADDRESS;
        String subnetId = FAKE_SUBNET_ID;
        String vpcId = FAKE_VPC_ID;

        CreateSubnetRequest request = CreateSubnetRequest.builder().availabilityZone(DEFAULT_AVAILABILITY_ZONE)
                .cidrBlock(cidr)
                .vpcId(vpcId)
                .build();

        Mockito.doReturn(subnetId).when(this.plugin).doCreateSubnetResquest(Mockito.eq(order.getName()),
                Mockito.eq(request), Mockito.eq(this.client));
        Mockito.doNothing().when(this.plugin).doAssociateRouteTables(Mockito.eq(subnetId), Mockito.eq(vpcId),
                Mockito.eq(this.client));
        Mockito.doNothing().when(this.plugin).handleSecurityIssues(Mockito.eq(subnetId), Mockito.eq(vpcId),
                Mockito.eq(cidr), Mockito.eq(this.client));

        String instanceName = FAKE_INSTANCE_NAME;

        // exercise
        this.plugin.doRequestInstance(instanceName, request, this.client);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doCreateSubnetResquest(Mockito.eq(order.getName()),
                Mockito.eq(request), Mockito.eq(this.client));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doAssociateRouteTables(Mockito.eq(subnetId),
                Mockito.eq(vpcId), Mockito.eq(this.client));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).handleSecurityIssues(Mockito.eq(subnetId),
                Mockito.eq(vpcId), Mockito.eq(cidr), Mockito.eq(this.client));
    }
    
    // test case: When calling the handleSecurityIssues method, it must verify
    // that is call was successful.
    @Test
    public void testHandleSecurityIssues() throws Exception {
        // set up
        String subnetId = FAKE_SUBNET_ID;
        String cidr = FAKE_CIDR_ADDRESS;
        String descrition = AwsNetworkPlugin.SECURITY_GROUP_DESCRIPTION;
        String groupId = FAKE_GROUP_ID;
        String groupName = SystemConstants.PN_SECURITY_GROUP_PREFIX + subnetId;
        String tagKey = AwsV2CloudUtil.AWS_TAG_GROUP_ID;
        String vpcId = FAKE_VPC_ID;

        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        PowerMockito.doReturn(groupId).when(AwsV2CloudUtil.class, TestUtils.CREATE_SECURITY_GROUP_METHOD,
                Mockito.eq(vpcId), Mockito.eq(groupName), Mockito.eq(descrition), Mockito.eq(this.client));

        PowerMockito.doNothing().when(AwsV2CloudUtil.class, TestUtils.CREATE_TAGS_REQUEST_METHOD, Mockito.eq(subnetId),
                Mockito.eq(tagKey), Mockito.eq(groupId), Mockito.eq(this.client));
        
        AuthorizeSecurityGroupIngressRequest request = AuthorizeSecurityGroupIngressRequest.builder()
                .cidrIp(cidr)
                .groupId(groupId)
                .ipProtocol(AwsNetworkPlugin.ALL_PROTOCOLS)
                .build();

        PowerMockito.doNothing().when(AwsV2CloudUtil.class, TestUtils.DO_AUTHORIZE_SECURITY_GROUP_INGRESS_METHOD, Mockito.eq(request),
                Mockito.eq(this.client));
        

        // exercise
        this.plugin.handleSecurityIssues(subnetId, vpcId, cidr, this.client);

        // verify
        PowerMockito.verifyStatic(AwsV2CloudUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.createSecurityGroup(Mockito.eq(vpcId), Mockito.eq(groupName), Mockito.eq(descrition),
                Mockito.eq(this.client));

        PowerMockito.verifyStatic(AwsV2CloudUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.createTagsRequest(Mockito.eq(subnetId), Mockito.eq(tagKey), Mockito.eq(groupId),
                Mockito.eq(this.client));
        
        PowerMockito.verifyStatic(AwsV2CloudUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.doAuthorizeSecurityGroupIngress(Mockito.eq(request), Mockito.eq(this.client));
    }
    
    // test case: When calling the handleSecurityIssues method, and an
    // unexpected error occurs, it must verify if an UnexpectedException has been
    // thrown.
    @Test
    public void testHandleSecurityIssuesFail() throws Exception {
        // set up
        String subnetId = FAKE_SUBNET_ID;
        String cidr = FAKE_CIDR_ADDRESS;
        String descrition = AwsNetworkPlugin.SECURITY_GROUP_DESCRIPTION;
        String groupId = FAKE_GROUP_ID;
        String groupName = SystemConstants.PN_SECURITY_GROUP_PREFIX + subnetId;
        String tagKey = AwsV2CloudUtil.AWS_TAG_GROUP_ID;
        String vpcId = FAKE_VPC_ID;
        
        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        PowerMockito.doReturn(groupId).when(AwsV2CloudUtil.class, TestUtils.CREATE_SECURITY_GROUP_METHOD,
                Mockito.eq(vpcId), Mockito.eq(groupName), Mockito.eq(descrition), Mockito.eq(this.client));

        PowerMockito.doNothing().when(AwsV2CloudUtil.class, TestUtils.CREATE_TAGS_REQUEST_METHOD, Mockito.eq(subnetId),
                Mockito.eq(tagKey), Mockito.eq(groupId), Mockito.eq(this.client));
        
        AuthorizeSecurityGroupIngressRequest request = AuthorizeSecurityGroupIngressRequest.builder()
                .cidrIp(cidr)
                .groupId(groupId)
                .ipProtocol(AwsNetworkPlugin.ALL_PROTOCOLS)
                .build();

        UnexpectedException exception = new UnexpectedException();
        PowerMockito.doThrow(exception).when(AwsV2CloudUtil.class, TestUtils.DO_AUTHORIZE_SECURITY_GROUP_INGRESS_METHOD,
                Mockito.eq(request), Mockito.eq(this.client));
        
        String expected = String.format(Messages.Exception.GENERIC_EXCEPTION, exception);

        String gatewayId = FAKE_GATEWAY_ID;
        Mockito.doReturn(gatewayId).when(this.plugin).getGatewayIdAttachedToVpc(Mockito.eq(vpcId), Mockito.eq(this.client));
        Mockito.doNothing().when(this.plugin).doRollbackAllConfigurationAndDeleteVpc(Mockito.eq(gatewayId),
                Mockito.eq(vpcId), Mockito.eq(this.client));

        try {
            // exercise
            this.plugin.handleSecurityIssues(subnetId, vpcId, cidr, this.client);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
            Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doDeleteSubnet(Mockito.eq(subnetId),
                    Mockito.eq(this.client));
            Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getGatewayIdAttachedToVpc(Mockito.eq(vpcId),
                    Mockito.eq(this.client));
            Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                    .doRollbackAllConfigurationAndDeleteVpc(Mockito.eq(gatewayId), Mockito.eq(vpcId), Mockito.eq(this.client));
        }
    }
    
    // test case: When calling the doAssociateRouteTables method, it must verify
    // that is call was successful.
    @Test
    public void testDoAssociateRouteTables() throws FogbowException {
        // set up
        String vpcId = FAKE_VPC_ID;
        String subnetId = FAKE_SUBNET_ID;
        String routeTableId = FAKE_ROUTE_TABLE_ID;
        
        RouteTable routeTable = buildRouteTables();
        Mockito.doReturn(routeTable).when(this.plugin).getRouteTables(Mockito.eq(vpcId), Mockito.eq(this.client));
        
        AssociateRouteTableRequest request = AssociateRouteTableRequest.builder()
                .routeTableId(routeTableId )
                .subnetId(subnetId)
                .build();
        
        // exercise
        this.plugin.doAssociateRouteTables(subnetId, vpcId , this.client);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).associateRouteTable(Mockito.eq(request));
    }
    
    // test case: When calling the doAssociateRouteTables method, and an
    // unexpected error occurs, it must verify if an UnexpectedException has been
    // thrown.
    @Test
    public void testDoAssociateRouteTablesFail() throws FogbowException {
        // set up
        String vpcId = FAKE_VPC_ID;
        String subnetId = FAKE_SUBNET_ID;

        RouteTable routeTable = buildRouteTables();
        Mockito.doReturn(routeTable).when(this.plugin).getRouteTables(Mockito.eq(vpcId), Mockito.eq(this.client));

        SdkClientException exception = SdkClientException.builder().build();
        Mockito.doThrow(exception).when(this.client).associateRouteTable(Mockito.any(AssociateRouteTableRequest.class));
        Mockito.doNothing().when(this.plugin).doDeleteSubnet(Mockito.eq(subnetId), Mockito.eq(this.client));

        String expected = String.format(Messages.Exception.GENERIC_EXCEPTION, exception);

        String gatewayId = FAKE_GATEWAY_ID;
        Mockito.doReturn(gatewayId).when(this.plugin).getGatewayIdAttachedToVpc(Mockito.eq(vpcId), Mockito.eq(this.client));
        Mockito.doNothing().when(this.plugin).doRollbackAllConfigurationAndDeleteVpc(Mockito.eq(gatewayId),
                Mockito.eq(vpcId), Mockito.eq(this.client));

        try {
            // exercise
            this.plugin.doAssociateRouteTables(subnetId, vpcId, this.client);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
            Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doDeleteSubnet(Mockito.eq(subnetId),
                    Mockito.eq(this.client));
            Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getGatewayIdAttachedToVpc(Mockito.eq(vpcId),
                    Mockito.eq(this.client));
            Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                    .doRollbackAllConfigurationAndDeleteVpc(Mockito.eq(gatewayId), Mockito.eq(vpcId), Mockito.eq(this.client));
        }
    }
    
    // test case: When calling the getGatewayIdAttachedToVpc method, with a
    // gateway attachment corresponding to the VPC ID, it must verify that the
    // gateway ID has been returned.
    @Test
    public void testGetGatewayIdAttachedToVpcSuccessfully() throws FogbowException {
        // set up
        String vpcId = FAKE_VPC_ID;

        DescribeInternetGatewaysResponse response = buildDescribeInternetGatewaysResponse();
        Mockito.when(this.client.describeInternetGateways()).thenReturn(response);

        String expected = FAKE_GATEWAY_ID;

        // exercise
        String gatewayId = this.plugin.getGatewayIdAttachedToVpc(vpcId, this.client);

        // verify
        Assert.assertEquals(expected, gatewayId);
    }

    // test case: When calling the getGatewayIdAttachedToVpc method, and not
    // find a gateway attachment corresponding to the VPC ID, it must verify
    // than an InstanceNotFoundException has been thrown.
    @Test
    public void testGetGatewayIdAttachedToVpcFail() throws FogbowException {
        // set up
        String vpcId = ANOTHER_VPC_ID;

        DescribeInternetGatewaysResponse response = buildDescribeInternetGatewaysResponse();
        Mockito.when(this.client.describeInternetGateways()).thenReturn(response);

        String expected = Messages.Exception.INSTANCE_NOT_FOUND;

        try {
            // exercise
            this.plugin.getGatewayIdAttachedToVpc(vpcId, this.client);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }

    // test case: When calling the doDeleteSubnet method, it must verify
    // that is call was successful.
    @Test
    public void testDoDeleteSubnet() throws FogbowException {
        // set up
        String subnetId = FAKE_SUBNET_ID;

        DeleteSubnetRequest request = DeleteSubnetRequest.builder()
                .subnetId(subnetId)
                .build();

        DeleteSubnetResponse response = DeleteSubnetResponse.builder().build();
        Mockito.doReturn(response).when(this.client).deleteSubnet(Mockito.eq(request));

        // exercise
        this.plugin.doDeleteSubnet(subnetId, this.client);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).deleteSubnet(Mockito.eq(request));
    }
    
    // test case: When calling the doDeleteSubnet method, and an
    // unexpected error occurs, it must verify if an UnexpectedException has been
    // thrown.
    @Test
    public void testDoDeleteSubnetFail() throws FogbowException {
        // set up
        SdkClientException exception = SdkClientException.builder().build();
        Mockito.doThrow(exception).when(this.client).deleteSubnet(Mockito.any(DeleteSubnetRequest.class));

        String subnetId = FAKE_SUBNET_ID;
        String expected = String.format(Messages.Error.ERROR_WHILE_REMOVING_RESOURCE,
                AwsNetworkPlugin.SUBNET_RESOURCE, subnetId);
        try {
            // exercise
            this.plugin.doDeleteSubnet(subnetId, this.client);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the getRouteTables method, it must verify
    // that is call was successful.
    @Test
    public void testGetRouteTables() throws FogbowException {
        // set up
        DescribeRouteTablesResponse response = buildDescribeRouteTablesResponse();
        Mockito.doReturn(response).when(this.plugin).doDescribeRouteTables(Mockito.eq(this.client));

        String vpcId = FAKE_VPC_ID;

        // exercise
        this.plugin.getRouteTables(vpcId , this.client);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doDescribeRouteTables(Mockito.eq(this.client));
    }
    
    // test case: When calling the getRouteTables method, without a list of route
    // tables, it must verify if an UnexpectedException has been thrown.
    @Test
    public void testGetRouteTablesFail() throws FogbowException {
        // set up
        DescribeRouteTablesResponse response = DescribeRouteTablesResponse.builder().build();
        Mockito.doReturn(response).when(this.plugin).doDescribeRouteTables(Mockito.eq(this.client));

        String vpcId = FAKE_VPC_ID;
        String expected = Messages.Exception.INSTANCE_NOT_FOUND;

        try {
            // exercise
            this.plugin.getRouteTables(vpcId, this.client);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
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
    
    // test case: When calling the doCreateSubnetResquest method, it must verify
    // that is call was successful.
    @Test
    public void testDoCreateSubnetResquest() throws FogbowException {
        // set up
        String cidr = FAKE_CIDR_ADDRESS;
        String instanceName = TestUtils.FAKE_INSTANCE_NAME;
        String vpcId = FAKE_VPC_ID;

        CreateSubnetRequest request = CreateSubnetRequest.builder()
                .availabilityZone(DEFAULT_AVAILABILITY_ZONE)
                .cidrBlock(cidr)
                .vpcId(vpcId)
                .build();

        CreateSubnetResponse response = buildCreateSubnetsResponse();
        Mockito.doReturn(response).when(this.client).createSubnet(Mockito.eq(request));

        // exercise
        this.plugin.doCreateSubnetResquest(instanceName, request, this.client);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).createSubnet(Mockito.eq(request));
    }
    
    // test case: When calling the doCreateSubnetResquest method, and an unexpected
    // error occurs, it must verify if an UnexpectedException has been thrown.
    @Test
    public void testDoCreateSubnetResquestFail() throws FogbowException {
        // set up
        String cidr = FAKE_CIDR_ADDRESS;
        String instanceName = TestUtils.FAKE_INSTANCE_NAME;
        String vpcId = FAKE_VPC_ID;

        CreateSubnetRequest request = CreateSubnetRequest.builder()
                .availabilityZone(DEFAULT_AVAILABILITY_ZONE)
                .cidrBlock(cidr)
                .vpcId(vpcId)
                .build();

        SdkClientException exception = SdkClientException.builder().build();
        Mockito.doThrow(exception).when(this.client).createSubnet(Mockito.eq(request));
        
        String expected = String.format(Messages.Exception.GENERIC_EXCEPTION, exception);

        try {
            // exercise
            this.plugin.doCreateSubnetResquest(instanceName, request, this.client);
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }

        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).createSubnet(Mockito.eq(request));
    }
    
    // test case: When calling the doCreateAndConfigureVpc method, it must
    // verify that is call was successful.
    @Test
    public void testDoCreateAndConfigureVpcSuccessfully() throws FogbowException {
        // set up
        String cidr = FAKE_CIDR_ADDRESS;
        String vpcId = FAKE_VPC_ID;

        CreateVpcResponse response = buildCreateVpcResponse();
        Mockito.when(this.client.createVpc(Mockito.any(CreateVpcRequest.class))).thenReturn(response);

        Mockito.doNothing().when(this.plugin).doModifyVpcAttributes(Mockito.eq(vpcId), Mockito.eq(this.client));

        String gatewayId = FAKE_GATEWAY_ID;
        Mockito.doReturn(gatewayId).when(this.plugin).doCreateInternetGateway(Mockito.eq(vpcId),
                Mockito.eq(this.client));

        Mockito.doNothing().when(this.plugin).doAttachInternetGateway(Mockito.eq(gatewayId), Mockito.eq(vpcId),
                Mockito.eq(this.client));
        Mockito.doNothing().when(this.plugin).doCreateRouteTables(Mockito.eq(cidr), Mockito.eq(gatewayId),
                Mockito.eq(vpcId), Mockito.eq(this.client));

        // exercise
        this.plugin.doCreateAndConfigureVpc(cidr, this.client);
                // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).createVpc(Mockito.any(CreateVpcRequest.class));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doModifyVpcAttributes(Mockito.eq(vpcId),
                Mockito.eq(this.client));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doCreateInternetGateway(Mockito.eq(vpcId),
                Mockito.eq(this.client));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doAttachInternetGateway(Mockito.eq(gatewayId),
                Mockito.eq(vpcId), Mockito.eq(this.client));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doCreateRouteTables(Mockito.eq(cidr),
                Mockito.eq(gatewayId), Mockito.eq(vpcId), Mockito.eq(this.client));
    }

    // test case: When calling the doCreateAndConfigureVpc method, and an
    // unexpected error occurs, it must verify if an UnexpectedException has
    // been thrown.
    @Test
    public void testDoCreateAndConfigureVpcFail() throws FogbowException {
        // set up
        String cidr = FAKE_CIDR_ADDRESS;

        Exception exception = SdkException.builder().build();
        Mockito.when(this.client.createVpc(Mockito.any(CreateVpcRequest.class))).thenThrow(exception);

        String expected = String.format(Messages.Exception.GENERIC_EXCEPTION, exception);

        try {
            // exercise
            this.plugin.doCreateAndConfigureVpc(cidr, this.client);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }

    // test case: When calling the doCreateRouteTables method, it must
    // verify that is call was successful.
    @Test
    public void testDoCreateRouteTablesSuccessfully() throws FogbowException {
        // set up
        String cidr = FAKE_CIDR_ADDRESS;
        String gatewayId = FAKE_GATEWAY_ID;
        String vpcId = FAKE_VPC_ID;

        RouteTable routeTable = buildRouteTables();
        Mockito.doReturn(routeTable).when(this.plugin).getRouteTables(Mockito.eq(vpcId),
                Mockito.eq(this.client));

        CreateRouteResponse response = CreateRouteResponse.builder().build();
        Mockito.when(this.client.createRoute(Mockito.any(CreateRouteRequest.class))).thenReturn(response);

        // exercise
        this.plugin.doCreateRouteTables(cidr, gatewayId, vpcId, this.client);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getRouteTables(Mockito.eq(vpcId),
                Mockito.eq(this.client));
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE))
                .createRoute(Mockito.any(CreateRouteRequest.class));
    }

    // test case: When calling the doCreateRouteTables method, and an
    // unexpected error occurs, it must verify if an UnexpectedException has
    // been thrown.
    @Test
    public void testDoCreateRouteTablesFail() throws FogbowException {
        // set up
        String cidr = FAKE_CIDR_ADDRESS;
        String gatewayId = FAKE_GATEWAY_ID;
        String vpcId = FAKE_VPC_ID;

        RouteTable routeTable = buildRouteTables();
        Mockito.doReturn(routeTable).when(this.plugin).getRouteTables(Mockito.eq(vpcId),
                Mockito.eq(this.client));

        SdkClientException exception = SdkClientException.builder().build();
        Mockito.when(this.client.createRoute(Mockito.any(CreateRouteRequest.class))).thenThrow(exception);

        String expected = String.format(Messages.Exception.GENERIC_EXCEPTION, exception);

        Mockito.doNothing().when(this.plugin).doRollbackAllConfigurationAndDeleteVpc(Mockito.eq(gatewayId),
                Mockito.eq(vpcId), Mockito.eq(this.client));

        try {
            // exercise
            this.plugin.doCreateRouteTables(cidr, gatewayId, vpcId, this.client);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());

            Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                    .doRollbackAllConfigurationAndDeleteVpc(Mockito.eq(gatewayId), Mockito.eq(vpcId),
                    Mockito.eq(this.client));
        }
    }

    // test case: When calling the doRollbackAllConfigurationAndDeleteVpc
    // method, it must verify that is call was successful.
    @Test
    public void testDoRollbackAllConfigurationAndDeleteVpcSuccessfully() throws FogbowException {
        // set up
        String gatewayId = FAKE_GATEWAY_ID;
        String vpcId = FAKE_VPC_ID;

        Mockito.doNothing().when(this.plugin).doDetachInternetGateway(Mockito.eq(gatewayId),
                Mockito.eq(vpcId), Mockito.eq(this.client));
        Mockito.doNothing().when(this.plugin).doDeleteInternetGateway(Mockito.eq(gatewayId),
                Mockito.eq(this.client));
        Mockito.doNothing().when(this.plugin).doDeleteVpc(Mockito.eq(vpcId), Mockito.eq(this.client));

        // exercise
        this.plugin.doRollbackAllConfigurationAndDeleteVpc(gatewayId, vpcId, this.client);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doDetachInternetGateway(Mockito.eq(gatewayId),
                Mockito.eq(vpcId), Mockito.eq(this.client));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doDeleteInternetGateway(Mockito.eq(gatewayId),
                Mockito.eq(this.client));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doDeleteVpc(Mockito.eq(vpcId),
                Mockito.eq(this.client));
    }

    // test case: When calling the doDetachInternetGateway method, it must
    // verify that is call was successful.
    @Test
    public void testDoDetachInternetGatewaySuccessfully() throws FogbowException {
        // set up
        String gatewayId = FAKE_GATEWAY_ID;
        String vpcId = FAKE_VPC_ID;

        DetachInternetGatewayResponse response = DetachInternetGatewayResponse.builder().build();
        Mockito.when(this.client.detachInternetGateway(Mockito.any(DetachInternetGatewayRequest.class)))
                .thenReturn(response);

        // exercise
        this.plugin.doDetachInternetGateway(gatewayId, vpcId, this.client);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE))
                .detachInternetGateway(Mockito.any(DetachInternetGatewayRequest.class));
    }

    // test case: When calling the doDetachInternetGateway method, and an
    // unexpected error occurs, it must verify if an UnexpectedException has
    // been thrown.
    @Test
    public void testDoDetachInternetGatewayFail() throws FogbowException {
        // set up
        String gatewayId = FAKE_GATEWAY_ID;
        String vpcId = FAKE_VPC_ID;

        SdkClientException exception = SdkClientException.builder().build();
        Mockito.when(this.client.detachInternetGateway(Mockito.any(DetachInternetGatewayRequest.class)))
                .thenThrow(exception);

        String expected = String.format(Messages.Exception.GENERIC_EXCEPTION, exception);

        try {
            // exercise
            this.plugin.doDetachInternetGateway(gatewayId, vpcId, this.client);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }

    // test case: When calling the doAttachInternetGateway method, it must
    // verify that is call was successful.
    @Test
    public void testDoAttachInternetGatewaySuccessfully() throws FogbowException {
        // set up
        String gatewayId = FAKE_GATEWAY_ID;
        String vpcId = FAKE_VPC_ID;

        AttachInternetGatewayResponse response = AttachInternetGatewayResponse.builder().build();
        Mockito.when(this.client.attachInternetGateway(Mockito.any(AttachInternetGatewayRequest.class)))
                .thenReturn(response);

        // exercise
        this.plugin.doAttachInternetGateway(gatewayId, vpcId, this.client);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE))
                .attachInternetGateway(Mockito.any(AttachInternetGatewayRequest.class));
    }

    // test case: When calling the doAttachInternetGateway method, and an
    // unexpected error occurs, it must verify if an UnexpectedException has
    // been thrown.
    @Test
    public void testDoAttachInternetGatewayFail() throws FogbowException {
        // set up
        String gatewayId = FAKE_GATEWAY_ID;
        String vpcId = FAKE_VPC_ID;

        SdkClientException exception = SdkClientException.builder().build();
        Mockito.when(this.client.attachInternetGateway(Mockito.any(AttachInternetGatewayRequest.class)))
                .thenThrow(exception);

        String expected = String.format(Messages.Exception.GENERIC_EXCEPTION, exception);

        Mockito.doNothing().when(this.plugin).doDeleteInternetGateway(Mockito.eq(gatewayId), Mockito.eq(this.client));
        Mockito.doNothing().when(this.plugin).doDeleteVpc(Mockito.eq(vpcId), Mockito.eq(this.client));

        try {
            // exercise
            this.plugin.doAttachInternetGateway(gatewayId, vpcId, this.client);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());

            Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doDeleteInternetGateway(Mockito.eq(gatewayId),
                    Mockito.eq(this.client));
            Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doDeleteVpc(Mockito.eq(vpcId),
                    Mockito.eq(this.client));
        }
    }

    // test case: When calling the doDeleteInternetGateway method, it must
    // verify that is call was successful.
    @Test
    public void testDoDeleteInternetGatewaySuccessFully() throws FogbowException {
        // set up
        String gatewayId = FAKE_GATEWAY_ID;

        DeleteInternetGatewayResponse response = DeleteInternetGatewayResponse.builder().build();
        Mockito.when(this.client.deleteInternetGateway(Mockito.any(DeleteInternetGatewayRequest.class)))
                .thenReturn(response);

        // exercise
        this.plugin.doDeleteInternetGateway(gatewayId, this.client);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE))
                .deleteInternetGateway(Mockito.any(DeleteInternetGatewayRequest.class));
    }

    // test case: When calling the doDeleteInternetGateway method, and an
    // unexpected error occurs, it must verify if an UnexpectedException has
    // been thrown.
    @Test
    public void testDoDeleteInternetGatewayFail() throws FogbowException {
        // set up
        String gatewayId = FAKE_GATEWAY_ID;

        SdkClientException exception =  SdkClientException.builder().build();
        Mockito.when(this.client.deleteInternetGateway(Mockito.any(DeleteInternetGatewayRequest.class)))
                .thenThrow(exception);

        String expected = String.format(Messages.Error.ERROR_WHILE_REMOVING_RESOURCE,
                AwsNetworkPlugin.GATEWAY_RESOURCE, gatewayId);

        try {
            // exercise
            this.plugin.doDeleteInternetGateway(gatewayId, this.client);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }

    // test case: When calling the doCreateInternetGateway method, it must
    // verify that is call was successful.
    @Test
    public void testDoCreateInternetGatewaySuccessfully() throws FogbowException {
        // set up
        String vpcId = FAKE_VPC_ID;

        CreateInternetGatewayResponse response = buildCreateInternetGatewayResponse();
        Mockito.when(this.client.createInternetGateway()).thenReturn(response);

        String expected = FAKE_GATEWAY_ID;

        // exercise
        String gatewayId = this.plugin.doCreateInternetGateway(vpcId, this.client);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).createInternetGateway();

        Assert.assertEquals(expected, gatewayId);
    }

    // test case: When calling the doCreateInternetGateway method, and an
    // unexpected error occurs, it must verify if an UnexpectedException has
    // been thrown.
    @Test
    public void testDoCreateInternetGatewayFail() throws FogbowException {
        // set up
        String vpcId = FAKE_VPC_ID;

        SdkClientException exception = SdkClientException.builder().build();
        Mockito.when(this.client.createInternetGateway()).thenThrow(exception);

        String expected = String.format(Messages.Exception.GENERIC_EXCEPTION, exception);

        Mockito.doNothing().when(this.plugin).doDeleteVpc(Mockito.eq(vpcId), Mockito.eq(this.client));

        try {
            // exercise
            this.plugin.doCreateInternetGateway(vpcId, this.client);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());

            Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                    .doDeleteVpc(Mockito.eq(vpcId), Mockito.eq(this.client));
        }
    }

    // test case: When calling the doModifyVpcAttributes method, it must
    // verify that is call was successful.
    @Test
    public void testDoModifyVpcAttributesSuccessfully() throws FogbowException {
        // set up
        String vpcId = FAKE_VPC_ID;

        ModifyVpcAttributeResponse response = ModifyVpcAttributeResponse.builder().build();
        Mockito.when(this.client.modifyVpcAttribute(Mockito.any(ModifyVpcAttributeRequest.class))).thenReturn(response);

        // exercise
        this.plugin.doModifyVpcAttributes(vpcId, this.client);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_TWICE))
                .modifyVpcAttribute(Mockito.any(ModifyVpcAttributeRequest.class));
    }

    // test case: When calling the doModifyVpcAttributes method, and an
    // unexpected error occurs, it must verify if an UnexpectedException
    // has been thrown.
    @Test
    public void testDoModifyVpcAttributesFail() throws FogbowException {
        // set up
        String vpcId = FAKE_VPC_ID;

        SdkClientException exception = SdkClientException.builder().build();
        Mockito.when(this.client.modifyVpcAttribute(Mockito.any(ModifyVpcAttributeRequest.class)))
                .thenThrow(exception);

        String expected = String.format(Messages.Exception.GENERIC_EXCEPTION, exception);

        Mockito.doNothing().when(this.plugin).doDeleteVpc(Mockito.eq(vpcId), Mockito.eq(this.client));

        try {
            // exercise
            this.plugin.doModifyVpcAttributes(vpcId, this.client);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());

            Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                    .doDeleteVpc(Mockito.eq(vpcId), Mockito.eq(this.client));
        }
    }

    // test case: When calling the doDeleteVpc method, it must verify that is
    // call was successful.
    @Test
    public void testDoDeleteVpcSuccessfully() throws FogbowException {
        // set up
        String vpcId = FAKE_VPC_ID;

        DeleteVpcResponse response = DeleteVpcResponse.builder().build();
        Mockito.when(this.client.deleteVpc(Mockito.any(DeleteVpcRequest.class)))
                .thenReturn(response );

        // exercise
        this.plugin.doDeleteVpc(vpcId, this.client);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE))
                .deleteVpc(Mockito.any(DeleteVpcRequest.class));
    }

    // test case: When calling the doDeleteVpc method, and an unexpected error
    // occurs, it must verify if an UnexpectedException has been thrown.
    @Test
    public void testDoDeleteVpcFail() throws FogbowException {
        // set up
        String vpcId = FAKE_VPC_ID;

        SdkClientException exception = SdkClientException.builder().build();
        Mockito.when(this.client.deleteVpc(Mockito.any(DeleteVpcRequest.class)))
                .thenThrow(exception);

        String expected = String.format(Messages.Error.ERROR_WHILE_REMOVING_RESOURCE,
                AwsNetworkPlugin.VPC_RESOURCE, vpcId);

        try {
            // exercise
            this.plugin.doDeleteVpc(vpcId, this.client);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }

    private CreateInternetGatewayResponse buildCreateInternetGatewayResponse() {
        InternetGateway internetGateway = buildInternetGateway(null);

        CreateInternetGatewayResponse response = CreateInternetGatewayResponse.builder()
                .internetGateway(internetGateway)
                .build();

        return response;
    }

    private CreateVpcResponse buildCreateVpcResponse() {
        Vpc vpc = Vpc.builder()
                .vpcId(FAKE_VPC_ID)
                .build();

        CreateVpcResponse response = CreateVpcResponse.builder()
                .vpc(vpc)
                .build();

        return response;
    }

    private DescribeInternetGatewaysResponse buildDescribeInternetGatewaysResponse() {
        InternetGatewayAttachment attachment = InternetGatewayAttachment.builder()
                .vpcId(FAKE_VPC_ID)
                .build();

        InternetGateway internetGateway = buildInternetGateway(attachment);

        DescribeInternetGatewaysResponse response = DescribeInternetGatewaysResponse.builder()
                .internetGateways(internetGateway)
                .build();

        return response;
    }

    private InternetGateway buildInternetGateway(InternetGatewayAttachment attachment) {
        InternetGateway internetGateway = InternetGateway.builder()
                .attachments(attachment)
                .internetGatewayId(FAKE_GATEWAY_ID)
                .build();

        return internetGateway;
    }

    private CreateSubnetResponse buildCreateSubnetsResponse() {
        CreateSubnetResponse response = CreateSubnetResponse.builder()
                .subnet(buildSubnet())
                .build();
        
        return response;
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
	
	private DescribeRouteTablesResponse buildDescribeRouteTablesResponse() {
	    DescribeRouteTablesResponse response = DescribeRouteTablesResponse.builder()
	            .routeTables(buildRouteTables())
	            .build();
	    
	    return response;
	}

    private RouteTable buildRouteTables() {
        RouteTable routeTable = RouteTable.builder()
	            .routeTableId(FAKE_ROUTE_TABLE_ID)
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
                .vpcId(FAKE_VPC_ID)
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
