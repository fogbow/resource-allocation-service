package cloud.fogbow.ras.core.plugins.interoperability.aws.securityrule.v2;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsResponse;
import software.amazon.awssdk.services.ec2.model.IpPermission;
import software.amazon.awssdk.services.ec2.model.IpRange;
import software.amazon.awssdk.services.ec2.model.RevokeSecurityGroupEgressRequest;
import software.amazon.awssdk.services.ec2.model.RevokeSecurityGroupEgressResponse;
import software.amazon.awssdk.services.ec2.model.RevokeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.RevokeSecurityGroupIngressResponse;
import software.amazon.awssdk.services.ec2.model.SecurityGroup;

@PrepareForTest({AwsV2ClientUtil.class, AwsV2SecurityRuleUtils.class, DatabaseManager.class })
public class AwsV2SecurityRulePluginTest extends BaseUnitTests {

    private static final String CLOUD_NAME = "amazon";
    private static final String DEFAULT_ADDRESS_RANGE = "24";
    private static final String DEFAULT_IP_ADDRESS = "0.0.0.0";
    private static final String DEFAULT_PROTOCOL = "tcp";
    
    private static final int DEFAULT_PORT_FROM = 0;
    private static final int DEFAULT_PORT_TO = 22;

    private AwsV2SecurityRulePlugin plugin;
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

        this.plugin = Mockito.spy(new AwsV2SecurityRulePlugin(awsConfFilePath));
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

        String expected = defineSecurityRuleId(Direction.IN);

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
        String securityRuleId = defineSecurityRuleId(Direction.IN);

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
        int position = AwsV2SecurityRulePlugin.FIRST_POSITION;

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
                Mockito.eq(Direction.IN), Mockito.eq(group.ipPermissions()));

        SecurityRuleInstance[] egressInstances = { buildSecurityRuleInstance(Direction.OUT) };
        Mockito.doReturn(Arrays.asList(egressInstances)).when(this.plugin).loadSecurityRuleInstances(Mockito.eq(instanceId),
                Mockito.eq(Direction.OUT), Mockito.eq(group.ipPermissionsEgress()));

        List<SecurityRuleInstance> expected = loadSecurityRuleInstancesCollection(
                ingressInstances[AwsV2SecurityRulePlugin.FIRST_POSITION],
                egressInstances[AwsV2SecurityRulePlugin.FIRST_POSITION]);

        // exercise
        List<SecurityRuleInstance> instancesList = this.plugin.doGetSecurityRules(instanceId, resourceType, this.client);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getSecurityGroupId(Mockito.eq(instanceId), Mockito.eq(resourceType),
                Mockito.eq(this.client));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getSecurityGroupById(Mockito.eq(groupId), Mockito.eq(this.client));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).loadSecurityRuleInstances(Mockito.eq(instanceId),
                Mockito.eq(Direction.IN), Mockito.eq(group.ipPermissions()));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).loadSecurityRuleInstances(Mockito.eq(instanceId),
                Mockito.eq(Direction.OUT), Mockito.eq(group.ipPermissionsEgress()));
        
        Assert.assertEquals(expected, instancesList);
    }
    
    // test case: When calling the loadSecurityRuleInstances method, it must verify
    // that is call was successful.
    @Test
    public void testLoadIngressRuleInstances() {
        // set up
        String instanceId = TestUtils.FAKE_INSTANCE_ID;
        Direction direction = Direction.OUT;

        IpPermission[] ipPermissions = loadIpPermissionsCollection();

        SecurityRuleInstance instance = buildSecurityRuleInstance(direction, Protocol.TCP);
        List<SecurityRuleInstance> expected = loadSecurityRuleInstancesCollection(instance);

        // exercise
        List<SecurityRuleInstance> instancesList = this.plugin.loadSecurityRuleInstances(instanceId, direction,
                Arrays.asList(ipPermissions));

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).buildSecurityRuleInstance(Mockito.eq(instanceId),
                Mockito.eq(direction), Mockito.any(IpPermission.class));

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
    
    // test case: ...
    @Test
    public void test() {
        // set up
        
        // exercise
        
        // verify
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
                .ipProtocol("tcp")
                .ipRanges(buildIpRange())
                .build();
        
        return ipPermission;
    }

    private IpRange buildIpRange() {
        IpRange ipRange = IpRange.builder()
                .cidrIp(DEFAULT_IP_ADDRESS 
                        + AwsV2SecurityRulePlugin.CIDR_SEPARATOR 
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

    private SecurityRuleInstance buildSecurityRuleInstance(Direction direction, Protocol... protocols) {
        Protocol protocol = protocols.length > 0 ? protocols[0] : Protocol.ANY;
        EtherType etherType = EtherType.IPv4;
        int portFrom = DEFAULT_PORT_FROM;
        int portTo = DEFAULT_PORT_TO;
        String instanceId = TestUtils.FAKE_INSTANCE_ID;
        String cidr = DEFAULT_IP_ADDRESS + AwsV2SecurityRulePlugin.CIDR_SEPARATOR + DEFAULT_ADDRESS_RANGE;
        return new SecurityRuleInstance(instanceId, direction, portFrom, portTo, cidr, etherType, protocol);
    }

    private String defineSecurityRuleId(Direction direction, ResourceType... types) {
        String securityRuleId = String.format(AwsV2SecurityRulePlugin.SECURITY_RULE_IDENTIFIER_FORMAT, 
                TestUtils.FAKE_INSTANCE_ID,
                DEFAULT_IP_ADDRESS,
                DEFAULT_ADDRESS_RANGE,
                DEFAULT_PORT_FROM,
                DEFAULT_PORT_TO,
                Protocol.ANY,
                direction,
                types.length > 0 ? types[0].getValue() : ResourceType.NETWORK.getValue()
        );
        return securityRuleId;
    }

    private SecurityRule createSecurityRule(Direction direction, Protocol... protocols) {
        Protocol protocol = protocols.length > 0 ? protocols[0] : Protocol.ANY;
        EtherType etherType = EtherType.IPv4;
        int portFrom = DEFAULT_PORT_FROM;
        int portTo = DEFAULT_PORT_TO;
        String cidr = DEFAULT_IP_ADDRESS + AwsV2SecurityRulePlugin.CIDR_SEPARATOR + DEFAULT_ADDRESS_RANGE;
        return new SecurityRule(direction, portFrom, portTo, cidr, etherType, protocol);
    }
    
    private Order createMajorOrder() {
        NetworkOrder majorOrder = this.testUtils.createLocalNetworkOrder();
        majorOrder.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        return majorOrder;
    }

}