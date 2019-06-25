package cloud.fogbow.ras.core.plugins.interoperability.aws.securityrule.v2;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.models.linkedlists.SynchronizedDoublyLinkedList;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.parameters.Network;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.image.v2.AwsV2ImagePlugin;
import org.h2.security.Fog;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AwsV2ClientUtil.class, AwsV2SecurityRuleUtils.class,
        AuthorizeSecurityGroupIngressRequest.class, RevokeSecurityGroupEgressRequest.class, SharedOrderHolders.class})
public class AwsV2SecurityRulePluginTest {

    private static final String CLOUD_NAME = "amazon";
    private static final String FAKE_GROUP_ID ="groupId";
    private static final String DEFAULT_CIDR = "0.0.0.0/24";
    private static final String INSTANCE_ID = "instanceId";
    private static final int DEFAULT_PORT_FROM = 0;
    private static final int DEFAULT_PORT_TO = 22;
    private static final String DEFAULT_RULE_ID = INSTANCE_ID + "@" + "0.0.0.0" + "@" + "24" + "@" + DEFAULT_PORT_FROM + "@" + DEFAULT_PORT_TO + "@" + "%s@%s@%s";

    private AwsV2SecurityRulePlugin plugin;
    private AwsV2SecurityRuleUtils utils;
    private SharedOrderHolders sharedOrderHolders;
    @Before
    public void setup() {
        String awsConfFilePath = HomeDir.getPath()
            + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME
            + File.separator
            + CLOUD_NAME
            + File.separator
            + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

        this.plugin = Mockito.spy(new AwsV2SecurityRulePlugin(awsConfFilePath));

        this.sharedOrderHolders = Mockito.mock(SharedOrderHolders.class);

        PowerMockito.mockStatic(SharedOrderHolders.class);
        BDDMockito.given(SharedOrderHolders.getInstance()).willReturn(this.sharedOrderHolders);

        Mockito.when(this.sharedOrderHolders.getOrdersList(Mockito.any(OrderState.class)))
                .thenReturn(new SynchronizedDoublyLinkedList<>());

        Mockito.when(this.sharedOrderHolders.getActiveOrdersMap()).thenReturn(new HashMap<>());
    }

    public void mockUtils() {
        String awsConfFilePath = HomeDir.getPath()
            + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME
            + File.separator
            + CLOUD_NAME
            + File.separator
            + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

        utils = Mockito.mock(AwsV2SecurityRuleUtils.class);
        PowerMockito.mockStatic(AwsV2SecurityRuleUtils.class);
        BDDMockito.given(AwsV2SecurityRuleUtils.getInstance()).willReturn(utils);
        this.plugin = Mockito.spy(new AwsV2SecurityRulePlugin(awsConfFilePath));
    }

    // test case: Test if the request method return the expected rule id
    @Test
    public void testRequestIngressSecurityRule() throws FogbowException {
        // setup
        mockUtils();
        Ec2Client client = Mockito.mock(Ec2Client.class);
        PowerMockito.mockStatic(AwsV2ClientUtil.class);
        BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

        SecurityRule rule = createRule(SecurityRule.Direction.IN, 0, 22, SecurityRule.Protocol.TCP);
        Order order = createOrder(ResourceType.NETWORK);
        AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

        BDDMockito.given(utils.getSecurityGroup(
                order.getInstanceId(), order.getType(), client)).willReturn(createGroup(FAKE_GROUP_ID, null));
        BDDMockito.given(utils.getId(
                rule, order)).willReturn(String.format(DEFAULT_RULE_ID, "TCP", "IN", "NETWORK"));

        AuthorizeSecurityGroupIngressRequest request = Mockito.mock(AuthorizeSecurityGroupIngressRequest.class);
        PowerMockito.mockStatic(AuthorizeSecurityGroupIngressRequest.class);
        AuthorizeSecurityGroupIngressRequest.Builder builder = Mockito.mock(AuthorizeSecurityGroupIngressRequest.Builder.class);
        PowerMockito.mockStatic(AuthorizeSecurityGroupIngressRequest.Builder.class);
        BDDMockito.given(AuthorizeSecurityGroupIngressRequest.builder()).willReturn(builder);
        BDDMockito.given(builder.build()).willReturn(request);
        BDDMockito.given(client.authorizeSecurityGroupIngress(request)).willReturn(null);

        //exercise
        String id = this.plugin.requestSecurityRule(rule, order, cloudUser);

        //verify
        Assert.assertEquals(String.format(DEFAULT_RULE_ID, "TCP", "IN", "NETWORK"), id);

    }

    // test case: test if the get method returns a list with the expected size
    // and if the right aux methods are called.
    @Test
    public void getSecurityRules() throws FogbowException{
        //setup
        mockUtils();
        Ec2Client client = Mockito.mock(Ec2Client.class);
        PowerMockito.mockStatic(AwsV2ClientUtil.class);
        BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

        IpRange ipRange = IpRange.builder()
            .cidrIp(DEFAULT_CIDR)
            .build();

        List<IpPermission> ipPermissions = new ArrayList<>();
        ipPermissions.add(IpPermission.builder()
                .fromPort(0)
                .toPort(22)
                .ipProtocol("tcp")
                .ipRanges(ipRange)
                .build());
        ipPermissions.add(IpPermission.builder()
                .fromPort(0)
                .toPort(22)
                .ipProtocol("icmp")
                .ipRanges(ipRange)
                .build());

        BDDMockito.given(utils.getSecurityGroup(
                Mockito.anyString(), Mockito.any(ResourceType.class), Mockito.any(Ec2Client.class))).willReturn(createGroup(FAKE_GROUP_ID, ipPermissions));
        BDDMockito.given(utils.getRules(Mockito.any(Order.class), Mockito.any(List.class), Mockito.any(SecurityRule.Direction.class))).willCallRealMethod();
        BDDMockito.given(utils.validateIpPermission(Mockito.any(IpPermission.class))).willCallRealMethod();

        Order order = createOrder(ResourceType.PUBLIC_IP);
        order.setInstanceId(INSTANCE_ID);
        AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

        // exercise
        List<SecurityRuleInstance> result = this.plugin.getSecurityRules(order, cloudUser);

        // verify
        Mockito.verify(utils, Mockito.times(ipPermissions.size())).validateIpPermission(Mockito.any(IpPermission.class));
        Mockito.verify(utils, Mockito.times(2)).getRules(Mockito.any(Order.class), Mockito.any(List.class), Mockito.any(SecurityRule.Direction.class));
        Assert.assertTrue(result.size() == ipPermissions.size());

    }

    // test case: Test if the delete method call the expected methods.
    @Test
    public void testDeleteSecurityRule() throws FogbowException{
        // setup
        mockUtils();
        Ec2Client client = Mockito.mock(Ec2Client.class);
        PowerMockito.mockStatic(AwsV2ClientUtil.class);
        BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

        BDDMockito.given(utils.getSecurityGroup(
                Mockito.anyString(), Mockito.any(ResourceType.class), Mockito.any(Ec2Client.class))).willReturn(createGroup(FAKE_GROUP_ID, null));
        BDDMockito.given(utils.getRuleFromId(Mockito.anyString())).willCallRealMethod();
        Mockito.doCallRealMethod().when(utils).revokeIngressRule(Mockito.any(SecurityRule.class), Mockito.any(SecurityGroup.class), Mockito.any(Ec2Client.class));
        Mockito.doCallRealMethod().when(utils).revokeEgressRule(Mockito.any(SecurityRule.class), Mockito.any(SecurityGroup.class), Mockito.any(Ec2Client.class));

        AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

        //exercise
        this.plugin.deleteSecurityRule(String.format(DEFAULT_RULE_ID, "TCP", "IN", "NETWORK"), cloudUser);
        this.plugin.deleteSecurityRule(String.format(DEFAULT_RULE_ID, "TCP", "OUT", "NETWORK"), cloudUser);

        //verify
        Mockito.verify(client, Mockito.times(1)).revokeSecurityGroupIngress(Mockito.any(RevokeSecurityGroupIngressRequest.class));
        Mockito.verify(client, Mockito.times(1)).revokeSecurityGroupEgress(Mockito.any(RevokeSecurityGroupEgressRequest.class));
    }

    // test case: Check if the getId is returning the rule id in the right format
    @Test
    public void testGetId() throws FogbowException {
        // setup
        AwsV2SecurityRuleUtils utils = AwsV2SecurityRuleUtils.getInstance();
        SecurityRule rule = createRule(SecurityRule.Direction.IN, 0, 22, SecurityRule.Protocol.TCP);
        Order order = createOrder(ResourceType.NETWORK);

        // exercise
        String id = utils.getId(rule, order);

        // verify
        Assert.assertEquals(String.format(DEFAULT_RULE_ID, "TCP", "IN", "NETWORK"), id);
    }

    // test case: Test if the validateIpPermission works properly
    @Test
    public void testValidateIpPermission() {
        // setup
        AwsV2SecurityRuleUtils utils = AwsV2SecurityRuleUtils.getInstance();

        IpRange ipRange = IpRange.builder()
            .cidrIp(DEFAULT_CIDR)
            .build();

        IpPermission ipPermission = IpPermission.builder()
            .fromPort(0)
            .toPort(22)
            .ipProtocol("tcp")
            .ipRanges(ipRange)
            .build();

        // exercise and verify
        Assert.assertTrue(utils.validateIpPermission(ipPermission));

        // setup
        ipPermission = IpPermission.builder()
                .fromPort(0)
                .toPort(22)
                .ipRanges(ipRange)
                .build();

        // exercise and verify
        Assert.assertFalse(utils.validateIpPermission(ipPermission));

        // setup
        ipPermission = IpPermission.builder()
                .fromPort(0)
                .ipProtocol("tcp")
                .ipRanges(ipRange)
                .build();

        // exercise and verify
        Assert.assertFalse(utils.validateIpPermission(ipPermission));

        // setup
        ipPermission = IpPermission.builder()
                .toPort(22)
                .ipProtocol("tcp")
                .ipRanges(ipRange)
                .build();

        // exercise and verify
        Assert.assertFalse(utils.validateIpPermission(ipPermission));
    }

    //test case: test if the method iterates over the tags properly to get the right groupId
    // and if the expected methods are called
    @Test
    public void testGetSecurityGroupBySubnetId() throws FogbowException {
        // setup
        mockUtils();

        Ec2Client client = Mockito.mock(Ec2Client.class);
        PowerMockito.mockStatic(AwsV2ClientUtil.class);
        BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

        Tag tag = Tag.builder()
            .key("groupId")
            .value(FAKE_GROUP_ID)
            .build();

        Subnet subnet = Subnet.builder()
            .tags(tag)
            .build();

        DescribeSubnetsResponse response = DescribeSubnetsResponse.builder()
            .subnets(subnet)
            .build();

        BDDMockito.given(client.describeSubnets(Mockito.any(DescribeSubnetsRequest.class))).willReturn(response);

        List<SecurityGroup> groups = new ArrayList<>();
        groups.add(createGroup(FAKE_GROUP_ID, null));

        DescribeSecurityGroupsResponse groupsResponse = DescribeSecurityGroupsResponse.builder()
            .securityGroups(groups)
            .build();

        BDDMockito.given(client.describeSecurityGroups(Mockito.any(DescribeSecurityGroupsRequest.class))).willReturn(groupsResponse);
        BDDMockito.given(utils.getSecurityGroupBySubnetId(Mockito.anyString(), Mockito.any(Ec2Client.class))).willCallRealMethod();
        BDDMockito.given(utils.getGroupIdBySubnet(Mockito.anyString(), Mockito.any(Ec2Client.class))).willCallRealMethod();
        BDDMockito.given(utils.getSubnetById(Mockito.anyString(), Mockito.any(Ec2Client.class))).willCallRealMethod();
        BDDMockito.given(utils.doDescribeSubnetsRequests(Mockito.anyString(), Mockito.any(Ec2Client.class))).willCallRealMethod();

        // exercise
        SecurityGroup group = utils.getSecurityGroupBySubnetId(INSTANCE_ID, client);

        // verify
        Assert.assertEquals(groups.get(0), group);
        Mockito.verify(client, Mockito.times(1)).describeSecurityGroups(Mockito.any(DescribeSecurityGroupsRequest.class));
        Mockito.verify(client, Mockito.times(1)).describeSubnets(Mockito.any(DescribeSubnetsRequest.class));

    }

    // test case: test if the method iterates over the tags properly
    // and if it calls the expected methods.
    @Test
    public void testGetSecurityGroupByAllocationId() throws FogbowException{
        // setup
        mockUtils();

        Ec2Client client = Mockito.mock(Ec2Client.class);
        PowerMockito.mockStatic(AwsV2ClientUtil.class);
        BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

        Tag tag = Tag.builder()
            .key("groupId")
            .value(FAKE_GROUP_ID)
            .build();

        Address address = Address.builder().tags(tag).build();

        DescribeAddressesResponse addressesResponse = DescribeAddressesResponse.builder()
            .addresses(address)
            .build();

        BDDMockito.given(client.describeAddresses(Mockito.any(DescribeAddressesRequest.class))).willReturn(addressesResponse);
        BDDMockito.when(utils.getGroupById(FAKE_GROUP_ID, client)).thenReturn(createGroup(FAKE_GROUP_ID, null));
        BDDMockito.given(utils.getAddress(Mockito.anyString(), Mockito.any(Ec2Client.class))).willCallRealMethod();
        BDDMockito.given(utils.getSecurityGroupByAllocationId(Mockito.anyString(), Mockito.any(Ec2Client.class))).willCallRealMethod();

        // exercise
        SecurityGroup group = utils.getSecurityGroupByAllocationId("", client);

        // verify
        Assert.assertEquals(createGroup(FAKE_GROUP_ID, null), group);
        Mockito.verify(client, Mockito.times(1)).describeAddresses(Mockito.any(DescribeAddressesRequest.class));
        Mockito.verify(utils, Mockito.times(1)).getGroupById(FAKE_GROUP_ID, client);
    }

    // test case: test if the addRule method calls the expected aux methods.
    @Test
    public void testAddIngressRule() throws FogbowException{
        // setup
        utils = AwsV2SecurityRuleUtils.getInstance();

        Ec2Client client = Mockito.mock(Ec2Client.class);
        PowerMockito.mockStatic(AwsV2ClientUtil.class);
        BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

        AuthorizeSecurityGroupIngressResponse authorizeGroupIngressResponse = AuthorizeSecurityGroupIngressResponse.builder()
            .build();

        BDDMockito.given(client.authorizeSecurityGroupIngress(Mockito.any(AuthorizeSecurityGroupIngressRequest.class))).willReturn(authorizeGroupIngressResponse);

        SecurityRule rule = createRule(SecurityRule.Direction.IN, 0, 22, SecurityRule.Protocol.TCP);

        //exercise
        utils.addIngressRule(createGroup(FAKE_GROUP_ID, null), rule, client);

        //verify
        Mockito.verify(client, Mockito.times(1)).authorizeSecurityGroupIngress(Mockito.any(AuthorizeSecurityGroupIngressRequest.class));
    }

    // test case: test if the addRule method calls the expected aux methods.
    @Test
    public void testAddEgressRule() throws FogbowException{
        // setup
        utils = AwsV2SecurityRuleUtils.getInstance();

        Ec2Client client = Mockito.mock(Ec2Client.class);
        PowerMockito.mockStatic(AwsV2ClientUtil.class);
        BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

        AuthorizeSecurityGroupEgressResponse authorizeGroupEgressResponse = AuthorizeSecurityGroupEgressResponse.builder()
                .build();

        BDDMockito.given(client.authorizeSecurityGroupEgress(Mockito.any(AuthorizeSecurityGroupEgressRequest.class))).willReturn(authorizeGroupEgressResponse);

        SecurityRule rule = createRule(SecurityRule.Direction.OUT, 0, 22, SecurityRule.Protocol.TCP);

        // exercise
        utils.addEgressRule(createGroup(FAKE_GROUP_ID, null), rule, client);

        // verify
        Mockito.verify(client, Mockito.times(1)).authorizeSecurityGroupEgress(Mockito.any(AuthorizeSecurityGroupEgressRequest.class));
    }

    private SecurityGroup createGroup(String groupId, List<IpPermission> ipPermissions) {
        SecurityGroup group;
        if(ipPermissions != null) {
            group = SecurityGroup.builder()
                .ipPermissions(ipPermissions)
                .groupId(groupId)
                .build();
        } else {
            group = SecurityGroup.builder()
                .groupId(groupId)
                .build();
        }
        return group;
    }

    private SecurityRule createRule(SecurityRule.Direction direction, int portFrom, int portTo, SecurityRule.Protocol protocol) {
        return new SecurityRule(direction, portFrom, portTo, DEFAULT_CIDR, SecurityRule.EtherType.IPv4, protocol);
    }

    private Order createOrder(ResourceType type) throws FogbowException{
        switch (type) {
            case NETWORK:
                NetworkOrder networkOrder = new NetworkOrder();
                networkOrder.setInstanceId(INSTANCE_ID);
                return networkOrder;
            case PUBLIC_IP:
                PublicIpOrder piOrder = new PublicIpOrder();
                piOrder.setInstanceId(INSTANCE_ID);
                return piOrder;
            default:
                throw new FogbowException();
        }
    }


}