package cloud.fogbow.ras.core.plugins.interoperability.aws.securityrule.v2;

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
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.api.parameters.SecurityRule.Direction;
import cloud.fogbow.ras.api.parameters.SecurityRule.EtherType;
import cloud.fogbow.ras.api.parameters.SecurityRule.Protocol;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import software.amazon.awssdk.services.ec2.Ec2Client;

@PrepareForTest({AwsV2ClientUtil.class, AwsV2SecurityRuleUtils.class, DatabaseManager.class })
public class AwsV2SecurityRulePluginTest extends BaseUnitTests {

    private static final String CLOUD_NAME = "amazon";
    private static final String DEFAULT_IP_ADDRESS = "0.0.0.0";
    private static final String DEFAULT_ADDRESS_RANGE = "24";
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

        List<SecurityRuleInstance> instances = createSecurityRuleInstancesCollection();
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
    
    // test case: ...
    @Test
    public void test() {
        // set up
        
        // exercise
        
        // verify
    }
    
    private List<SecurityRuleInstance> createSecurityRuleInstancesCollection() {
        SecurityRuleInstance[] ruleInstances = { 
                buildSecurityRuleInstance(Direction.IN),
                buildSecurityRuleInstance(Direction.OUT) 
        };
        return Arrays.asList(ruleInstances);
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