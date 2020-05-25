package cloud.fogbow.ras.core.plugins.interoperability.aws.securityrule.v2;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.ArrayList;
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
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.api.parameters.SecurityRule.Direction;
import cloud.fogbow.ras.api.parameters.SecurityRule.EtherType;
import cloud.fogbow.ras.api.parameters.SecurityRule.Protocol;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2CloudUtil;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Address;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupEgressRequest;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupEgressResponse;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupIngressResponse;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsResponse;
import software.amazon.awssdk.services.ec2.model.IpPermission;
import software.amazon.awssdk.services.ec2.model.IpRange;
import software.amazon.awssdk.services.ec2.model.RevokeSecurityGroupEgressRequest;
import software.amazon.awssdk.services.ec2.model.RevokeSecurityGroupEgressResponse;
import software.amazon.awssdk.services.ec2.model.RevokeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.RevokeSecurityGroupIngressResponse;
import software.amazon.awssdk.services.ec2.model.SecurityGroup;
import software.amazon.awssdk.services.ec2.model.Subnet;
import software.amazon.awssdk.services.ec2.model.Tag;

@PrepareForTest({ AwsV2ClientUtil.class, AwsV2CloudUtil.class, DatabaseManager.class })
public class AwsSecurityRulePluginTest extends BaseUnitTests {

    private static final String CLOUD_NAME = "amazon";
    private static final String DEFAULT_ADDRESS_RANGE = "24";
    private static final String DEFAULT_IP_ADDRESS = "0.0.0.0";
    private static final String DEFAULT_PROTOCOL = "tcp";
    
    private static final int DEFAULT_PORT_FROM = 0;
    private static final int DEFAULT_PORT_TO = 22;

    private AwsSecurityRulePlugin plugin;
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

        this.plugin = Mockito.spy(new AwsSecurityRulePlugin(awsConfFilePath));
        this.client = this.testUtils.getAwsMockedClient();
    }
    
    // test case: When calling the requestSecurityRule method, it must verify
    // that is call was successful and return the expected security rule id.
    @Test
    public void testRequestSecurityRule() throws FogbowException {
        // set up
        Order majorOrder = createMajorOrder();

        String groupId = TestUtils.FAKE_SECURITY_GROUP_ID;
        Mockito.doReturn(groupId).when(this.plugin).getSecurityGroupId(Mockito.eq(majorOrder.getInstanceId()),
                Mockito.eq(majorOrder.getType()), Mockito.eq(this.client));

        SecurityRule securityRule = createSecurityRule(Direction.IN);
        Mockito.doNothing().when(this.plugin).addRuleToSecurityGroup(Mockito.eq(groupId), Mockito.eq(securityRule),
                Mockito.eq(this.client));

        AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

        String expected = defineSecurityRuleId(Direction.IN, ResourceType.NETWORK);

        // exercise
        String securityRuleId = this.plugin.requestSecurityRule(securityRule, majorOrder, cloudUser);

        // verify
        PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2ClientUtil.createEc2Client(Mockito.eq(cloudUser.getToken()), Mockito.anyString());

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getSecurityGroupId(
                Mockito.eq(majorOrder.getInstanceId()), Mockito.eq(majorOrder.getType()), Mockito.eq(this.client));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).addRuleToSecurityGroup(Mockito.eq(groupId),
                Mockito.eq(securityRule), Mockito.eq(this.client));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doPackingSecurityRuleId(
                Mockito.eq(majorOrder.getInstanceId()), Mockito.eq(securityRule), Mockito.eq(majorOrder.getType()));

        Assert.assertEquals(expected, securityRuleId);
    }
    
    // test case: When calling the getSecurityRules method, it must verify
    // that is call was successful.
    @Test
    public void testGetSecurityRules() throws FogbowException {
        // set up
        Order majorOrder = createMajorOrder();

        List<SecurityRuleInstance> instances = loadSecurityRuleInstancesCollection();
        Mockito.doReturn(instances).when(this.plugin).doGetSecurityRules(Mockito.eq(majorOrder.getInstanceId()),
                Mockito.eq(majorOrder.getType()), Mockito.eq(this.client));

        AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

        // exercise
        this.plugin.getSecurityRules(majorOrder, cloudUser);

        // verify
        PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2ClientUtil.createEc2Client(Mockito.eq(cloudUser.getToken()), Mockito.anyString());

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetSecurityRules(
                Mockito.eq(majorOrder.getInstanceId()), Mockito.eq(majorOrder.getType()), Mockito.eq(this.client));
    }
    
    // test case: When calling the deleteSecurityRule method, it must verify
    // that is call was successful.
    @Test
    public void testDeleteSecurityRule() throws FogbowException {
        // set up
        String securityRuleId = defineSecurityRuleId(Direction.IN, ResourceType.PUBLIC_IP);

        Mockito.doNothing().when(this.plugin).doDeleteSecurityRule(Mockito.eq(securityRuleId), Mockito.eq(this.client));

        AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

        // exercise
        this.plugin.deleteSecurityRule(securityRuleId, cloudUser);

        // verify
        PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2ClientUtil.createEc2Client(Mockito.eq(cloudUser.getToken()), Mockito.anyString());

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doDeleteSecurityRule(Mockito.eq(securityRuleId),
                Mockito.eq(this.client));
    }
    
    // test case: When calling the doDeleteSecurityRule method with an ingress rule
    // to network resource type, it must verify that is call was successful.
    @Test
    public void testDoDeleteIngressSecurityRule() throws FogbowException {
        // set up
        SecurityRule rule = createSecurityRule(Direction.IN);
        ResourceType type = ResourceType.NETWORK;
        String instanceId = TestUtils.FAKE_INSTANCE_ID;
        String groupId = TestUtils.FAKE_SECURITY_GROUP_ID;
        String securityRuleId = defineSecurityRuleId(Direction.IN, type);

        Mockito.doReturn(groupId).when(this.plugin).getSecurityGroupId(Mockito.eq(instanceId), Mockito.eq(type),
                Mockito.eq(this.client));

        Mockito.doNothing().when(this.plugin).revokeIngressRule(Mockito.eq(groupId), Mockito.eq(rule),
                Mockito.eq(this.client));

        // exercise
        this.plugin.doDeleteSecurityRule(securityRuleId, this.client);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getSecurityGroupId(Mockito.eq(instanceId),
                Mockito.eq(type), Mockito.eq(this.client));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doUnpackingSecurityRuleId(Mockito.eq(securityRuleId));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).revokeIngressRule(Mockito.eq(groupId),
                Mockito.eq(rule), Mockito.eq(this.client));
    }
    
    // test case: When calling the doDeleteSecurityRule method with an egress rule
    // to public IP resource type, it must verify that is call was successful.
    @Test
    public void testDoEgressDeleteSecurityRule() throws FogbowException {
        // set up
        SecurityRule rule = createSecurityRule(Direction.OUT);
        ResourceType type = ResourceType.PUBLIC_IP;
        String instanceId = TestUtils.FAKE_INSTANCE_ID;
        String groupId = TestUtils.FAKE_SECURITY_GROUP_ID;
        String securityRuleId = defineSecurityRuleId(Direction.OUT, type);
        
        Mockito.doReturn(groupId).when(this.plugin).getSecurityGroupId(Mockito.eq(instanceId), Mockito.eq(type),
                Mockito.eq(this.client));

        Mockito.doNothing().when(this.plugin).revokeEgressRule(Mockito.eq(groupId), Mockito.eq(rule),
                Mockito.eq(this.client));

        // exercise
        this.plugin.doDeleteSecurityRule(securityRuleId, this.client);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getSecurityGroupId(Mockito.eq(instanceId),
                Mockito.eq(type), Mockito.eq(this.client));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doUnpackingSecurityRuleId(Mockito.eq(securityRuleId));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).revokeEgressRule(Mockito.eq(groupId),
                Mockito.eq(rule), Mockito.eq(this.client));
    }

    // test case: When calling the revokeEgressRule method, it must verify
    // that is call was successful.
    @Test
    public void testRevokeEgressRule() throws FogbowException {
        // set up
        String groupId = TestUtils.FAKE_SECURITY_GROUP_ID;
        SecurityRule rule = createSecurityRule(Direction.OUT);

        IpPermission ipPermission = buildIpPermission();
        Mockito.doReturn(ipPermission).when(this.plugin).buildIpPermission(Mockito.eq(rule));

        RevokeSecurityGroupEgressRequest request = RevokeSecurityGroupEgressRequest.builder()
                .ipPermissions(ipPermission)
                .groupId(groupId)
                .build();

        RevokeSecurityGroupEgressResponse response = RevokeSecurityGroupEgressResponse.builder().build();
        Mockito.doReturn(response).when(this.client).revokeSecurityGroupEgress(Mockito.eq(request));

        // exercise
        this.plugin.revokeEgressRule(groupId, rule, this.client);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).buildIpPermission(Mockito.eq(rule));
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).revokeSecurityGroupEgress(Mockito.eq(request));
    }
    
    // test case: When calling the revokeEgressRule method, and an unexpected
    // error occurs, it must verify if an UnexpectedException has been thrown.
    @Test
    public void testRevokeEgressRuleFail() throws FogbowException {
        // set up
        String groupId = TestUtils.FAKE_SECURITY_GROUP_ID;
        SecurityRule rule = createSecurityRule(Direction.OUT);

        IpPermission ipPermission = buildIpPermission();
        Mockito.doReturn(ipPermission).when(this.plugin).buildIpPermission(Mockito.eq(rule));

        SdkClientException exception = SdkClientException.builder().build();
        Mockito.doThrow(exception).when(this.client)
                .revokeSecurityGroupEgress(Mockito.any(RevokeSecurityGroupEgressRequest.class));

        String expected = String.format(Messages.Exception.GENERIC_EXCEPTION, exception);

        try {
            // exercise
            this.plugin.revokeEgressRule(groupId, rule, this.client);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the revokeIngressRule method, it must verify
    // that is call was successful.
    @Test
    public void testRevokeIngressRule() throws FogbowException {
        // set up
        String groupId = TestUtils.FAKE_SECURITY_GROUP_ID;
        SecurityRule rule = createSecurityRule(Direction.IN);

        IpPermission ipPermission = buildIpPermission();
        Mockito.doReturn(ipPermission).when(this.plugin).buildIpPermission(Mockito.eq(rule));

        RevokeSecurityGroupIngressRequest request = RevokeSecurityGroupIngressRequest.builder()
                .ipPermissions(ipPermission)
                .groupId(groupId)
                .build();

        RevokeSecurityGroupIngressResponse response = RevokeSecurityGroupIngressResponse.builder().build();
        Mockito.doReturn(response).when(this.client).revokeSecurityGroupIngress(Mockito.eq(request));

        // exercise
        this.plugin.revokeIngressRule(groupId, rule, client);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).buildIpPermission(Mockito.eq(rule));
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).revokeSecurityGroupIngress(Mockito.eq(request));
    }
    
    // test case: When calling the revokeIngressRule method, and an unexpected
    // error occurs, it must verify if an UnexpectedException has been thrown.
    @Test
    public void testRevokeIngressRuleFail() throws FogbowException {
        // set up
        String groupId = TestUtils.FAKE_SECURITY_GROUP_ID;
        SecurityRule rule = createSecurityRule(Direction.OUT);

        IpPermission ipPermission = buildIpPermission();
        Mockito.doReturn(ipPermission).when(this.plugin).buildIpPermission(Mockito.eq(rule));

        SdkClientException exception = SdkClientException.builder().build();
        Mockito.doThrow(exception).when(this.client)
                .revokeSecurityGroupIngress(Mockito.any(RevokeSecurityGroupIngressRequest.class));

        String expected = String.format(Messages.Exception.GENERIC_EXCEPTION, exception);

        try {
            // exercise
            this.plugin.revokeIngressRule(groupId, rule, this.client);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the doUnpackingSecurityRuleId method with a invalid
    // security rule id, it must verify if an InvalidParameterException has been
    // thrown.
    @Test
    public void testDoUnpackingSecurityRuleIdFail() {
        // set up
        String securityRuleId = TestUtils.ANY_VALUE;

        String expected = String.format(Messages.Exception.INVALID_PARAMETER_S, securityRuleId);

        try {
            // exercise
            this.plugin.doUnpackingSecurityRuleId(securityRuleId);
            Assert.fail();
        } catch (InvalidParameterException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the extractFieldFrom method with a invalid security
    // rule id, it must verify if an InvalidParameterException has been thrown.
    @Test
    public void testExtractFieldFromSecurityRuleIdFail() {
        // set up
        String securityRuleId = TestUtils.ANY_VALUE;
        int position = AwsSecurityRulePlugin.FIRST_POSITION;

        String expected = String.format(Messages.Exception.INVALID_PARAMETER_S, securityRuleId);

        try {
            // exercise
            this.plugin.extractFieldFrom(securityRuleId, position);
            Assert.fail();
        } catch (InvalidParameterException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the doGetSecurityRules method, it must verify
    // that is call was successful.
    @Test
    public void testDoGetSecurityRules() throws FogbowException {
        // set up
        ResourceType resourceType = ResourceType.NETWORK;
        String instanceId = TestUtils.FAKE_INSTANCE_ID;

        String groupId = TestUtils.FAKE_SECURITY_GROUP_ID;
        Mockito.doReturn(groupId).when(this.plugin).getSecurityGroupId(Mockito.eq(instanceId), Mockito.eq(resourceType),
                Mockito.eq(this.client));

        SecurityGroup group = SecurityGroup.builder()
                .ipPermissions(buildIpPermission())
                .ipPermissionsEgress(buildIpPermission())
                .build();
        
        Mockito.doReturn(group).when(this.plugin).getSecurityGroupById(Mockito.eq(groupId), Mockito.eq(this.client));

        SecurityRuleInstance[] ingressInstances = { buildSecurityRuleInstance(Direction.IN) };
        Mockito.doReturn(Arrays.asList(ingressInstances)).when(this.plugin).loadSecurityRuleInstances(Mockito.eq(instanceId),
                Mockito.eq(resourceType), Mockito.eq(Direction.IN), Mockito.eq(group.ipPermissions()));

        SecurityRuleInstance[] egressInstances = { buildSecurityRuleInstance(Direction.OUT) };
        Mockito.doReturn(Arrays.asList(egressInstances)).when(this.plugin).loadSecurityRuleInstances(Mockito.eq(instanceId),
                Mockito.eq(resourceType), Mockito.eq(Direction.OUT), Mockito.eq(group.ipPermissionsEgress()));

        List<SecurityRuleInstance> expected = loadSecurityRuleInstancesCollection(
                ingressInstances[AwsSecurityRulePlugin.FIRST_POSITION],
                egressInstances[AwsSecurityRulePlugin.FIRST_POSITION]);

        // exercise
        List<SecurityRuleInstance> instancesList = this.plugin.doGetSecurityRules(instanceId, resourceType, this.client);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getSecurityGroupId(Mockito.eq(instanceId), Mockito.eq(resourceType),
                Mockito.eq(this.client));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getSecurityGroupById(Mockito.eq(groupId), Mockito.eq(this.client));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).loadSecurityRuleInstances(Mockito.eq(instanceId),
                Mockito.eq(resourceType), Mockito.eq(Direction.IN), Mockito.eq(group.ipPermissions()));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).loadSecurityRuleInstances(Mockito.eq(instanceId),
                Mockito.eq(resourceType), Mockito.eq(Direction.OUT), Mockito.eq(group.ipPermissionsEgress()));
        
        Assert.assertEquals(expected, instancesList);
    }
    
    // test case: When calling the loadSecurityRuleInstances method, it must verify
    // that is call was successful.
    @Test
    public void testLoadIngressRuleInstances() {
        // set up
        ResourceType resourceType = ResourceType.NETWORK;
        String instanceId = TestUtils.FAKE_INSTANCE_ID;
        Direction direction = Direction.OUT;

        IpPermission[] ipPermissions = loadIpPermissionsCollection();

        SecurityRuleInstance instance = buildSecurityRuleInstance(direction);
        List<SecurityRuleInstance> expected = loadSecurityRuleInstancesCollection(instance);

        // exercise
        List<SecurityRuleInstance> instancesList = this.plugin.loadSecurityRuleInstances(instanceId, resourceType,
                direction, Arrays.asList(ipPermissions));

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).buildSecurityRuleInstance(Mockito.eq(instanceId),
                Mockito.eq(resourceType), Mockito.eq(direction), Mockito.any(IpPermission.class));

        Assert.assertEquals(expected, instancesList);
    }

    // test case: When calling the getSecurityGroupById method, it must verify
    // that is call was successful.
    @Test
    public void testGetSecurityGroupById() throws FogbowException {
        // set up
        String groupId = TestUtils.FAKE_SECURITY_GROUP_ID;

        DescribeSecurityGroupsRequest request = DescribeSecurityGroupsRequest.builder()
                .groupIds(groupId)
                .build();

        DescribeSecurityGroupsResponse response = DescribeSecurityGroupsResponse.builder().build();
        Mockito.doReturn(response).when(this.plugin).doDescribeSecurityGroupsRequest(Mockito.eq(request),
                Mockito.eq(this.client));

        SecurityGroup group = SecurityGroup.builder().build();
        Mockito.doReturn(group).when(this.plugin).getSecurityGroupFrom(Mockito.eq(response));

        // exercise
        this.plugin.getSecurityGroupById(groupId, this.client);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doDescribeSecurityGroupsRequest(Mockito.eq(request), Mockito.eq(this.client));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getSecurityGroupFrom(Mockito.eq(response));
    }
    
    // test case: When calling the getSecurityGroupFrom method, it must verify
    // that is call was successful.
    @Test
    public void testGetSecurityGroupFromResponse() throws FogbowException {
        SecurityGroup expected = buildSecurityGroup();
        DescribeSecurityGroupsResponse response = buildSecurityGroupsResponse(expected);

        // exercise
        SecurityGroup group = this.plugin.getSecurityGroupFrom(response);

        // verify
        Assert.assertEquals(expected, group);
    }

    
    // test case: When calling the getSecurityGroupFrom method and return a null
    // response, it must verify that an InstanceNotFoundException has been thrown.
    @Test
    public void testGetSecurityGroupFromNullResponse() throws FogbowException {
        DescribeSecurityGroupsResponse response = null;

        String expected = Messages.Exception.INSTANCE_NOT_FOUND;

        try {
            // exercise
            this.plugin.getSecurityGroupFrom(response);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }

    // test case: When calling the getSecurityGroupFrom method and return a response
    // with a empty security groups, it must verify that an
    // InstanceNotFoundException has been thrown.
    @Test
    public void testGetSecurityGroupFromEmptyResponse() throws FogbowException {
        DescribeSecurityGroupsResponse response = buildSecurityGroupsResponse();

        String expected = Messages.Exception.INSTANCE_NOT_FOUND;

        try {
            // exercise
            this.plugin.getSecurityGroupFrom(response);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the doDescribeSecurityGroupsRequest method, it must
    // verify that is call was successful.
    @Test
    public void testDoDescribeSecurityGroupsRequest() throws FogbowException {
        // set up
        DescribeSecurityGroupsRequest request = DescribeSecurityGroupsRequest.builder()
                .groupIds(TestUtils.FAKE_SECURITY_GROUP_ID).build();

        DescribeSecurityGroupsResponse response = buildSecurityGroupsResponse();
        Mockito.doReturn(response).when(this.client).describeSecurityGroups(Mockito.eq(request));

        // exercise
        this.plugin.doDescribeSecurityGroupsRequest(request, client);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).describeSecurityGroups(Mockito.eq(request));
    }
    
    // test case: When calling the doDescribeSecurityGroupsRequest method, and an unexpected
    // error occurs, it must verify if an UnexpectedException has been thrown.
    @Test
    public void testDoDescribeSecurityGroupsRequestFail() throws FogbowException {
     // set up
        DescribeSecurityGroupsRequest request = DescribeSecurityGroupsRequest.builder()
                .groupIds(TestUtils.FAKE_SECURITY_GROUP_ID).build();

        SdkClientException exception = SdkClientException.builder().build();
        Mockito.doThrow(exception).when(this.client).describeSecurityGroups(Mockito.eq(request));

        String expected = String.format(Messages.Exception.GENERIC_EXCEPTION, exception);
        try {
            // exercise
            this.plugin.doDescribeSecurityGroupsRequest(request, client);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).describeSecurityGroups(Mockito.eq(request));
    }
    
    // test case: When calling the addRuleToSecurityGroup method with a ingress
    // rule, it must verify that is call was successful.
    @Test
    public void testAddIngressRuleToSecurityGroup() throws FogbowException {
        // set up
        String groupId = TestUtils.FAKE_SECURITY_GROUP_ID;
        SecurityRule securityRule = createSecurityRule(Direction.IN);

        Mockito.doNothing().when(this.plugin).addIngressRule(Mockito.eq(groupId), Mockito.any(IpPermission.class),
                Mockito.eq(this.client));
        
        // exercise
        this.plugin.addRuleToSecurityGroup(groupId, securityRule, this.client);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).buildIpPermission(Mockito.eq(securityRule));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).addIngressRule(Mockito.eq(groupId),
                Mockito.any(IpPermission.class), Mockito.eq(this.client));
    }
    
    // test case: When calling the addRuleToSecurityGroup method with a egress
    // rule, it must verify that is call was successful.
    @Test
    public void testAddEgressRuleToSecurityGroup() throws FogbowException {
     // set up
        String groupId = TestUtils.FAKE_SECURITY_GROUP_ID;
        SecurityRule securityRule = createSecurityRule(Direction.OUT);

        Mockito.doNothing().when(this.plugin).addEgressRule(Mockito.eq(groupId), Mockito.any(IpPermission.class),
                Mockito.eq(this.client));
        
        // exercise
        this.plugin.addRuleToSecurityGroup(groupId, securityRule, this.client);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).buildIpPermission(Mockito.eq(securityRule));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).addEgressRule(Mockito.eq(groupId),
                Mockito.any(IpPermission.class), Mockito.eq(this.client));
    }
    
    // test case: When calling the addEgressRule method, it must
    // verify that is call was successful.
    @Test
    public void testAddEgressRule() throws FogbowException {
        // set up
        String groupId = TestUtils.FAKE_SECURITY_GROUP_ID;
        IpPermission ipPermission = buildIpPermission();

        AuthorizeSecurityGroupEgressRequest request = AuthorizeSecurityGroupEgressRequest.builder()
                .groupId(groupId)
                .ipPermissions(ipPermission)
                .build();

        AuthorizeSecurityGroupEgressResponse response = AuthorizeSecurityGroupEgressResponse.builder().build();
        Mockito.doReturn(response).when(this.client).authorizeSecurityGroupEgress(Mockito.eq(request));

        // exercise
        this.plugin.addEgressRule(groupId, ipPermission, client);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE))
                .authorizeSecurityGroupEgress(Mockito.eq(request));
    }
    
    // test case: When calling the addEgressRule method, and an unexpected
    // error occurs, it must verify if an UnexpectedException has been thrown.
    @Test
    public void testAddEgressRuleFail() throws FogbowException {
        // set up
        String groupId = TestUtils.FAKE_SECURITY_GROUP_ID;
        IpPermission ipPermission = buildIpPermission();

        AuthorizeSecurityGroupEgressRequest request = AuthorizeSecurityGroupEgressRequest.builder()
                .groupId(groupId)
                .ipPermissions(ipPermission)
                .build();

        SdkClientException exception = SdkClientException.builder().build();
        Mockito.doThrow(exception).when(this.client).authorizeSecurityGroupEgress(Mockito.eq(request));

        String expected = String.format(Messages.Exception.GENERIC_EXCEPTION, exception);

        try {
            // exercise
            this.plugin.addEgressRule(groupId, ipPermission, client);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the addIngressRule method, it must
    // verify that is call was successful.
    @Test
    public void testAddIngressRule() throws FogbowException {
        // set up
        String groupId = TestUtils.FAKE_SECURITY_GROUP_ID;
        IpPermission ipPermission = buildIpPermission();

        AuthorizeSecurityGroupIngressRequest request = AuthorizeSecurityGroupIngressRequest.builder()
                .groupId(groupId)
                .ipPermissions(ipPermission)
                .build();

        AuthorizeSecurityGroupIngressResponse response = AuthorizeSecurityGroupIngressResponse.builder().build();
        Mockito.doReturn(response).when(this.client).authorizeSecurityGroupIngress(Mockito.eq(request));

        // exercise
        this.plugin.addIngressRule(groupId, ipPermission, client);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE))
                .authorizeSecurityGroupIngress(Mockito.eq(request));
    }
    
    // test case: When calling the addIngressRule method, and an unexpected
    // error occurs, it must verify if an UnexpectedException has been thrown.
    @Test
    public void testAddIngressRuleFail() throws FogbowException {
        // set up
        String groupId = TestUtils.FAKE_SECURITY_GROUP_ID;
        IpPermission ipPermission = buildIpPermission();

        AuthorizeSecurityGroupIngressRequest request = AuthorizeSecurityGroupIngressRequest.builder()
                .groupId(groupId)
                .ipPermissions(ipPermission)
                .build();

        SdkClientException exception = SdkClientException.builder().build();
        Mockito.doThrow(exception).when(this.client).authorizeSecurityGroupIngress(Mockito.eq(request));

        String expected = String.format(Messages.Exception.GENERIC_EXCEPTION, exception);

        try {
            // exercise
            this.plugin.addIngressRule(groupId, ipPermission, client);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the getSecurityGroupId method with NETWORK resource
    // type, it must verify that is call was successful.
    @Test
    public void testGetSecurityGroupIdWithNetworkResourceType() throws Exception {
        // set up
        String subnetId = TestUtils.FAKE_INSTANCE_ID;
        ResourceType resourceType = ResourceType.NETWORK;

        Subnet subnet = buildSubnet();
        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        PowerMockito.doReturn(subnet).when(AwsV2CloudUtil.class, TestUtils.GET_SUBNET_BY_ID_METHOD,
                Mockito.eq(subnetId), Mockito.eq(this.client));

        PowerMockito.doReturn(TestUtils.FAKE_SECURITY_GROUP_ID).when(AwsV2CloudUtil.class,
                TestUtils.GET_GROUP_ID_FROM_METHOD, Mockito.eq(subnet.tags()));

        // exercise
        this.plugin.getSecurityGroupId(subnetId, resourceType, this.client);

        // verify
        PowerMockito.verifyStatic(AwsV2CloudUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.getSubnetById(Mockito.eq(subnetId), Mockito.eq(this.client));

        PowerMockito.verifyStatic(AwsV2CloudUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.getGroupIdFrom(Mockito.eq(subnet.tags()));
    }
    
    // test case: When calling the getSecurityGroupId method with PUBLIC_IP resource
    // type, it must verify that is call was successful.
    @Test
    public void testGetSecurityGroupIdWithPublicIpResourceType() throws Exception {
        // set up
        String allocationId = TestUtils.FAKE_INSTANCE_ID;
        ResourceType resourceType = ResourceType.PUBLIC_IP;

        Address address = buildAddress();
        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        PowerMockito.doReturn(address).when(AwsV2CloudUtil.class, TestUtils.GET_ADDRESS_BY_ID_METHOD,
                Mockito.eq(allocationId), Mockito.eq(this.client));

        PowerMockito.doReturn(TestUtils.FAKE_SECURITY_GROUP_ID).when(AwsV2CloudUtil.class,
                TestUtils.GET_GROUP_ID_FROM_METHOD, Mockito.eq(address.tags()));

        // exercise
        this.plugin.getSecurityGroupId(allocationId, resourceType, this.client);

        // verify
        PowerMockito.verifyStatic(AwsV2CloudUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.getAddressById(Mockito.eq(allocationId), Mockito.eq(this.client));

        PowerMockito.verifyStatic(AwsV2CloudUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.getGroupIdFrom(Mockito.eq(address.tags()));
    }

    // test case: When calling the getSecurityGroupId method with a invalid resource
    // type, it must verify that an InvalidParameterException has been thrown.
    @Test
    public void testgetSecurityGroupIdWithInvalidResourceType() throws FogbowException {
        // set up
        String instanceId = TestUtils.FAKE_INSTANCE_ID;
        ResourceType resourceType = ResourceType.INVALID_RESOURCE;
        
        String expected = String.format(Messages.Exception.INVALID_PARAMETER_S, resourceType);

        try {
            // exercise
            this.plugin.getSecurityGroupId(instanceId, resourceType, client);
            Assert.fail();
        } catch (InvalidParameterException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the getProtocolFrom method for all protocols, the ANY
    // type from Protocol must be returned.
    @Test
    public void testGetProtocolFromAllProtocols() {
        // set up
        String ipProtocol = AwsSecurityRulePlugin.ALL_PROTOCOLS;

        Protocol expected = Protocol.ANY;

        // exercise
        Protocol protocol = this.plugin.getProtocolFrom(ipProtocol);

        // verify
        Assert.assertEquals(expected, protocol);
    }
    
    // test case: When calling the getProtocolFrom method for other protocols, the
    // corresponding type of each protocol must be returned.
    @Test
    public void testGetProtocolFromOtherProtocols() {
        // set up
        String[] ipProtocol = { "icmp", "tcp", "udp" };

        Protocol[] expected = { Protocol.ICMP, Protocol.TCP, Protocol.UDP };

        for (int i = 0; i < ipProtocol.length; i++) {
            // exercise
            Protocol protocol = this.plugin.getProtocolFrom(ipProtocol[i]);

            // verify
            Assert.assertEquals(expected[i], protocol);
        }
    }
    
    // test case: When calling the defineIpProtocolFrom method for Protocol of type
    // ANY, it must return the representation value for all protocols.
    @Test
    public void testDefineIpProtocolFromAnyProtocol() {
        // set up
        String expected = AwsSecurityRulePlugin.ALL_PROTOCOLS;
        
        // exercise
        String ipProtocol = this.plugin.defineIpProtocolFrom(Protocol.ANY);

        // verify
        Assert.assertEquals(expected, ipProtocol);
    }
    
    // test case: When calling the defineIpProtocolFrom method for other protocols, the
    // corresponding value of each protocol must be returned.
    @Test
    public void testdefineIpProtocolFromOtherProtocols() {
        // set up
        Protocol[] protocols = { Protocol.ICMP, Protocol.TCP, Protocol.UDP };

        String[] expected = { "icmp", "tcp", "udp" };

        for (int i = 0; i < protocols.length; i++) {
            // exercise
            String value = this.plugin.defineIpProtocolFrom(protocols[i]);

            // verify
            Assert.assertEquals(expected[i], value);
        }
    }
    
    // test case: When calling the validateIpAddress method with a invalid CIDR IP,
    // it must verify that an InvalidParameterException has been thrown.
    @Test
    public void testValidateIpAddressFail() {
        // set up
        String cidrIp = TestUtils.ANY_VALUE;
        
        String expected = String.format(Messages.Exception.INVALID_CIDR_FORMAT, cidrIp);

        try {
            // exercise
            this.plugin.validateIpAddress(cidrIp);
            Assert.fail();
        } catch (InvalidParameterException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    private Address buildAddress() {
        Tag[] tags = { buildTagGroupId() };
        
        Address address = Address.builder()
                .tags(Arrays.asList(tags))
                .build();
        
        return address;
    }
    
    private Subnet buildSubnet() {
        Tag[] tags = { buildTagGroupId() };
        
        Subnet subnet = Subnet.builder()
                .tags(Arrays.asList(tags))
                .build();
        
        return subnet;
    }

    private Tag buildTagGroupId() {
        Tag tag = Tag.builder()
                .key(AwsV2CloudUtil.AWS_TAG_GROUP_ID)
                .value(TestUtils.FAKE_SECURITY_GROUP_ID)
                .build();
        
        return tag;
    }
    
    private DescribeSecurityGroupsResponse buildSecurityGroupsResponse(SecurityGroup... groups) {
        List<SecurityGroup> securityGroups = new ArrayList<>();
        for (int i = 0; i < groups.length; i++) {
            securityGroups.add(groups[i]);
        }
        
        DescribeSecurityGroupsResponse response = DescribeSecurityGroupsResponse.builder()
                .securityGroups(securityGroups)
                .build();
        
        return response;
    }
    
    private SecurityGroup buildSecurityGroup() {
        SecurityGroup securityGroup = SecurityGroup.builder()
                .groupId(TestUtils.FAKE_SECURITY_GROUP_ID)
                .build();
        
        return securityGroup;
    }
    
    private IpPermission[] loadIpPermissionsCollection() {
        IpPermission[] ipPermissions = { 
                buildIpPermission(), 
                IpPermission.builder().build(),
                IpPermission.builder().fromPort(DEFAULT_PORT_FROM).build(),
                IpPermission.builder().fromPort(DEFAULT_PORT_FROM).toPort(DEFAULT_PORT_TO).build(),
                IpPermission.builder().fromPort(DEFAULT_PORT_FROM).toPort(DEFAULT_PORT_TO).ipProtocol(DEFAULT_PROTOCOL).build(),
        };
        return ipPermissions;
    }
    
    private IpPermission buildIpPermission() {
        IpPermission ipPermission = IpPermission.builder()
                .fromPort(DEFAULT_PORT_FROM)
                .toPort(DEFAULT_PORT_TO)
                .ipProtocol(AwsSecurityRulePlugin.ALL_PROTOCOLS)
                .ipRanges(buildIpRange())
                .build();
        
        return ipPermission;
    }

    private IpRange buildIpRange() {
        IpRange ipRange = IpRange.builder()
                .cidrIp(DEFAULT_IP_ADDRESS 
                        + AwsSecurityRulePlugin.CIDR_SEPARATOR
                        + DEFAULT_ADDRESS_RANGE)
                .build();

        return ipRange;
    }
    
    private List<SecurityRuleInstance> loadSecurityRuleInstancesCollection(SecurityRuleInstance... instances) {
        List<SecurityRuleInstance> securityRuleInstances = new ArrayList<>();
        for (int i = 0; i < instances.length; i++) {
            securityRuleInstances.add(instances[i]);
        }
        if (securityRuleInstances.isEmpty()) {
            securityRuleInstances.add(buildSecurityRuleInstance(Direction.IN));
            securityRuleInstances.add(buildSecurityRuleInstance(Direction.OUT));
        }
        return securityRuleInstances;
    }

    private SecurityRuleInstance buildSecurityRuleInstance(Direction direction) {
        Protocol protocol = Protocol.ANY;
        EtherType etherType = EtherType.IPv4;
        int portFrom = DEFAULT_PORT_FROM;
        int portTo = DEFAULT_PORT_TO;
        String instanceId = TestUtils.FAKE_INSTANCE_ID;
        String cidr = DEFAULT_IP_ADDRESS + AwsSecurityRulePlugin.CIDR_SEPARATOR + DEFAULT_ADDRESS_RANGE;
        return new SecurityRuleInstance(instanceId, direction, portFrom, portTo, cidr, etherType, protocol);
    }

    private String defineSecurityRuleId(Direction direction, ResourceType resourceType) {
        String securityRuleId = String.format(AwsSecurityRulePlugin.SECURITY_RULE_IDENTIFIER_FORMAT,
                TestUtils.FAKE_INSTANCE_ID,
                DEFAULT_IP_ADDRESS,
                DEFAULT_ADDRESS_RANGE,
                DEFAULT_PORT_FROM,
                DEFAULT_PORT_TO,
                Protocol.ANY,
                direction,
                resourceType.getValue()
        );
        return securityRuleId;
    }

    private SecurityRule createSecurityRule(Direction direction) {
        Protocol protocol = Protocol.ANY;
        EtherType etherType = EtherType.IPv4;
        int portFrom = DEFAULT_PORT_FROM;
        int portTo = DEFAULT_PORT_TO;
        String cidr = DEFAULT_IP_ADDRESS + AwsSecurityRulePlugin.CIDR_SEPARATOR + DEFAULT_ADDRESS_RANGE;
        return new SecurityRule(direction, portFrom, portTo, cidr, etherType, protocol);
    }
    
    private Order createMajorOrder() {
        NetworkOrder majorOrder = this.testUtils.createLocalNetworkOrder();
        majorOrder.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        return majorOrder;
    }

}