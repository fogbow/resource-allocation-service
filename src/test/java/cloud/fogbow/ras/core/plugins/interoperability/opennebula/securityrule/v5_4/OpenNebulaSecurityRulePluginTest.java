package cloud.fogbow.ras.core.plugins.interoperability.opennebula.securityrule.v5_4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opennebula.client.OneResponse;
import org.opennebula.client.secgroup.SecurityGroup;
import org.opennebula.client.vnet.VirtualNetwork;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.api.parameters.SecurityRule.Direction;
import cloud.fogbow.ras.api.parameters.SecurityRule.EtherType;
import cloud.fogbow.ras.api.parameters.SecurityRule.Protocol;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaBaseTests;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;

@PrepareForTest({ DatabaseManager.class, GetSecurityGroupResponse.class, OpenNebulaClientUtil.class, VirtualNetwork.class })
public class OpenNebulaSecurityRulePluginTest extends OpenNebulaBaseTests {

    private static final String ANOTHER_SECURITY_GROUP_ID = "another-security-group-id";
    private static final String CIDR_FULL_ADDRESS = "192.168.0.1/28";
    private static final String CIDR_IP_ADDRESS = "192.168.0.1";
    private static final String CIDR_SIZE_VALUE = "14";
    private static final String FAKE_SECURITY_RULE_INSTANCE_ID = "fake-security-group-id@@INBOUND@192.168.0.1@14@@ALL";
    private static final String OPENNEBULA_ENDPOINT = "http://localhost:2633/RPC2";
    private static final String RANGE_PORTS_VALUE = "8080:8081";
    private static final String SECURITY_GROUP_CONTENT_FORMAT = "%s,%s";
    private static final String SECURITY_RULE_IDENTIFIER_FORMAT = "%s@%s@%s@%s@%s@%s@%s";
    
    private static final int DEFAULT_SSH_PORT = 22;
    
    private OpenNebulaSecurityRulePlugin plugin;
    private String endpoint;

    @Before
    public void setUp() throws FogbowException {
        super.setUp();
        this.plugin = Mockito.spy(new OpenNebulaSecurityRulePlugin(this.openNebulaConfFilePath));
        this.endpoint = OPENNEBULA_ENDPOINT;
    }
    
    // test case: When calling the requestSecurityRule method, it must verify
    // that is call was successful.
    @Test
    public void testRequestSecurityRule() throws FogbowException {
        // set up
        Order majorOrder = createMajorOrder(ResourceType.NETWORK);
        SecurityRule securityRule = createSecurityRule(Direction.IN);
        
        SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);
        Mockito.doReturn(securityGroup).when(this.plugin).getSecurityGroup(Mockito.eq(this.client),
                Mockito.eq(majorOrder));
        
        String securiryRuleId = defineSecurityRuleId(Direction.IN);
        Mockito.doReturn(securiryRuleId).when(this.plugin).doRequestSecurityRule(Mockito.any(SecurityGroup.class),
                Mockito.any(Rule.class));
        
        // exercise
        this.plugin.requestSecurityRule(securityRule, majorOrder, this.cloudUser);

        // verify
        PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenNebulaClientUtil.createClient(Mockito.eq(this.endpoint), Mockito.eq(this.cloudUser.getToken()));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getSecurityGroup(Mockito.eq(this.client),
                Mockito.eq(majorOrder));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .createSecurityRuleRequest(Mockito.eq(securityRule), Mockito.eq(securityGroup));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doRequestSecurityRule(Mockito.eq(securityGroup),
                Mockito.any(Rule.class));
    }

    // test case: When calling the getSecurityRules method, it must verify
    // that is call was successful.
    @Test
    public void testGetSecurityRules() throws FogbowException {
        // set up
        Order majorOrder = createMajorOrder(ResourceType.NETWORK);
        SecurityGroup securityGroup = getSecurityGroupMocked(majorOrder);

        List<SecurityRuleInstance> instances = createSecurityRuleInstancesCollection();
        Mockito.doReturn(instances).when(this.plugin).doGetSecurityRules(Mockito.eq(securityGroup));

        // exercise
        this.plugin.getSecurityRules(majorOrder, this.cloudUser);

        // verify
        PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenNebulaClientUtil.createClient(Mockito.eq(this.endpoint), Mockito.eq(this.cloudUser.getToken()));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getSecurityGroup(Mockito.eq(this.client),
                Mockito.eq(majorOrder));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetSecurityRules(Mockito.eq(securityGroup));
    }

    // test case: When calling the deleteSecurityRule method, it must verify
    // that is call was successful.
    @Test
    public void testDeleteSecurityRule() throws FogbowException {
        // set up
        String securityRuleId = defineSecurityRuleId(Direction.IN);
        String securityGroupId = TestUtils.FAKE_SECURITY_GROUP_ID;

        Rule rule = buildRule();
        Mockito.doReturn(rule).when(this.plugin).doUnpakingSecurityRuleId(Mockito.eq(securityRuleId));
        Mockito.doNothing().when(this.plugin).doDeleteSecurityRule(Mockito.eq(this.client), Mockito.eq(rule),
                Mockito.eq(securityGroupId));

        // exercise
        this.plugin.deleteSecurityRule(securityRuleId, this.cloudUser);

        // verify
        PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenNebulaClientUtil.createClient(Mockito.eq(this.endpoint), Mockito.eq(this.cloudUser.getToken()));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doUnpakingSecurityRuleId(Mockito.eq(securityRuleId));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doDeleteSecurityRule(Mockito.eq(this.client),
                Mockito.eq(rule), Mockito.eq(securityGroupId));
    }
    
    // test case: When calling the doDeleteSecurityRule method, it must verify
    // that is call was successful.
    @Test
    public void testDoDeleteSecurityRule() throws FogbowException {
        // set up
        Rule rule = buildRule();
        String securityGroupId = TestUtils.FAKE_SECURITY_GROUP_ID;

        SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);
        Mockito.when(OpenNebulaClientUtil.getSecurityGroup(Mockito.eq(this.client), Mockito.eq(securityGroupId)))
                .thenReturn(securityGroup);

        List<Rule> rules = buildRulesCollection(rule);
        GetSecurityGroupResponse response = buildSecurityGroupResponse(rules);
        Mockito.doReturn(response).when(this.plugin).doGetSecurityGroupResponse(Mockito.eq(securityGroup));

        Mockito.doNothing().when(this.plugin).updateSecurityGroup(Mockito.eq(securityGroup), Mockito.anyString());

        // exercise
        this.plugin.doDeleteSecurityRule(this.client, rule, securityGroupId);

        // verify
        PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenNebulaClientUtil.getSecurityGroup(Mockito.eq(this.client), Mockito.eq(securityGroupId));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doGetSecurityGroupResponse(Mockito.eq(securityGroup));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .generateUpdateRequest(Mockito.eq(response), Mockito.anyList());
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).updateSecurityGroup(Mockito.eq(securityGroup),
                Mockito.anyString());
    }

    // test case: When calling the doDeleteSecurityRule method with a rule other
    // than the rules contained in the response, it must verify that an
    // InvalidParameterException has been thrown.
    @Test
    public void testDoDeleteSecurityRuleFail() throws FogbowException {
        // set up
        Rule anotherRule = Rule.builder().build();
        String securityGroupId = TestUtils.FAKE_SECURITY_GROUP_ID;

        SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);
        Mockito.when(OpenNebulaClientUtil.getSecurityGroup(Mockito.eq(this.client), Mockito.eq(securityGroupId)))
                .thenReturn(securityGroup);

        Rule rule = buildRule();
        List<Rule> rules = buildRulesCollection(rule);
        GetSecurityGroupResponse response = buildSecurityGroupResponse(rules);
        Mockito.doReturn(response).when(this.plugin).doGetSecurityGroupResponse(Mockito.eq(securityGroup));

        String expected = Messages.Exception.RULE_NOT_AVAILABLE;

        try {
            // exercise
            this.plugin.doDeleteSecurityRule(this.client, anotherRule, securityGroupId);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the doUnpakingSecurityRuleId method with a valid
    // security rule ID, it must return an expected rule.
    @Test
    public void testDoUnpakingSecurityRuleId() throws FogbowException {
        // set up
        String securityRuleId = defineSecurityRuleId(Direction.IN);
        
        Rule expected = buildRule();
        
        // exercise
        Rule rule = this.plugin.doUnpakingSecurityRuleId(securityRuleId);
        
        // verify
        Assert.assertEquals(expected, rule);
    }
    
    // test case: When calling the doUnpakingSecurityRuleId method with an
    // invalid security rule ID, it must verify that an
    // InvalidParameterException has been thrown.
    @Test
    public void testDoUnpakingSecurityRuleIdFail() throws FogbowException {
     // set up
        String securityRuleId = TestUtils.ANY_VALUE;
        
        String expected = String.format(Messages.Exception.INVALID_PARAMETER_S, securityRuleId);
        
        try {
            // exercise
            this.plugin.doUnpakingSecurityRuleId(securityRuleId);
            Assert.fail();
        } catch (InvalidParameterException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the doGetSecurityRules method, it must verify
    // that is call was successful.
    @Test
    public void testDoGetSecurityRules() {
        // set up
        SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);
        Rule rule = buildRule();
        List<Rule> rules = buildRulesCollection(rule);

        GetSecurityGroupResponse response = buildSecurityGroupResponse(rules);
        Mockito.doReturn(response).when(this.plugin).doGetSecurityGroupResponse(securityGroup);
        
        // exercise
        this.plugin.doGetSecurityRules(securityGroup);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doGetSecurityGroupResponse(Mockito.eq(securityGroup));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).buildSecurityRule(Mockito.eq(rule));
    }
    
    // test case: When calling the doRequestSecurityRule method, it must verify
    // that is call was successful.
    @Test
    public void testDoRequestSecurityRule() throws FogbowException {
        // set up
        SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);
        Rule rule = buildRule();

        GetSecurityGroupResponse response = buildSecurityGroupResponse(new ArrayList<Rule>());
        Mockito.doReturn(response).when(this.plugin).doGetSecurityGroupResponse(Mockito.eq(securityGroup));

        List<Rule> rules = buildRulesCollection(rule);
        String template = generateRequestTemplate(rules);
        Mockito.doNothing().when(this.plugin).updateSecurityGroup(Mockito.any(SecurityGroup.class),
                Mockito.anyString());

        // exercise
        this.plugin.doRequestSecurityRule(securityGroup, rule);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doGetSecurityGroupResponse(Mockito.eq(securityGroup));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).updateSecurityGroup(Mockito.eq(securityGroup),
                Mockito.eq(template));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doPackingSecurityRuleId(Mockito.eq(rule));
    }

    // test case: When calling the updateSecurityGroup method, it must verify
    // that is call was successful.
    @Test
    public void testUpdateSecurityGroup() throws FogbowException {
        // set up
        Rule rule = buildRule();
        List<Rule> rules = buildRulesCollection(rule);
        String template = generateRequestTemplate(rules);
        
        SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);
        OneResponse response = Mockito.mock(OneResponse.class);
        Mockito.when(securityGroup.update(Mockito.eq(template))).thenReturn(response);
        Mockito.when(response.isError()).thenReturn(false);

        // exercise
        this.plugin.updateSecurityGroup(securityGroup, template);

        // verify
        Mockito.verify(securityGroup, Mockito.times(TestUtils.RUN_ONCE)).update(Mockito.eq(template));
        Mockito.verify(response, Mockito.times(TestUtils.RUN_ONCE)).isError();
    }
    
    // test case: When calling the updateSecurityGroup method with a invalid
    // template, it must verify that an InternalServerErrorException has been thrown.
    @Test
    public void testUpdateSecurityGroupFail() throws FogbowException {
        // set up
        String template = TestUtils.ANY_VALUE;

        SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);
        OneResponse response = Mockito.mock(OneResponse.class);
        Mockito.when(securityGroup.update(Mockito.eq(template))).thenReturn(response);
        Mockito.when(response.isError()).thenReturn(true);

        String expected = String.format(Messages.Log.ERROR_WHILE_UPDATING_SECURITY_GROUPS_S, template);

        try {
            // exercise
            this.plugin.updateSecurityGroup(securityGroup, template);
            Assert.fail();
        } catch (InternalServerErrorException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the doGetSecurityGroupResponse method, it must
    // verify that is call was successful and return the expected response.
    @Test
    public void testDoGetSecurityGroupResponse() {
        // set up
        SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);
        OneResponse oneResponse = Mockito.mock(OneResponse.class);
        
        Rule rule = buildRule();
        List<Rule> rules = buildRulesCollection(rule);
        
        String xml = generateTemplateResponse(rules);
        Mockito.when(securityGroup.info()).thenReturn(oneResponse);
        Mockito.when(oneResponse.getMessage()).thenReturn(xml);
        
        GetSecurityGroupResponse expected = GetSecurityGroupResponse.unmarshaller()
                .response(xml)
                .unmarshal();
        
        // exercise
        GetSecurityGroupResponse response = this.plugin.doGetSecurityGroupResponse(securityGroup);

        // verify
        Mockito.verify(securityGroup, Mockito.times(TestUtils.RUN_ONCE)).info();
        Mockito.verify(oneResponse, Mockito.times(TestUtils.RUN_ONCE)).getMessage();
        
        Assert.assertEquals(expected.getId(), response.getId());
        Assert.assertEquals(expected.getName(), response.getName());
        Assert.assertEquals(expected.getTemplate().getRules(), response.getTemplate().getRules());
    }

    // test case: When calling the getSecurityGroup method, it must
    // verify that is call was successful.
    @Test
    public void testGetSecurityGroup() throws FogbowException {
        // set up
        Order majorOrder = createMajorOrder(ResourceType.NETWORK);
        String securityGroupId = TestUtils.FAKE_SECURITY_GROUP_ID;

        String securityGroupName = SystemConstants.PN_SECURITY_GROUP_PREFIX + TestUtils.FAKE_INSTANCE_ID;
        Mockito.doReturn(securityGroupName).when(this.plugin).retrieveSecurityGroupName(Mockito.eq(majorOrder));

        VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
        PowerMockito.when(OpenNebulaClientUtil.getVirtualNetwork(Mockito.eq(this.client), 
                Mockito.eq(majorOrder.getInstanceId()))).thenReturn(virtualNetwork);

        String content = String.format(SECURITY_GROUP_CONTENT_FORMAT, securityGroupId, ANOTHER_SECURITY_GROUP_ID);
        Mockito.doReturn(content).when(this.plugin).getSecurityGroupContentFrom(Mockito.eq(virtualNetwork));

        SecurityGroup securityGroup = mockSecurityGroupFromNetwork(securityGroupId, securityGroupName);
        Mockito.doReturn(securityGroup).when(this.plugin).findSecurityGroupByName(Mockito.eq(this.client),
                Mockito.eq(content), Mockito.eq(securityGroupName));

        // exercise
        this.plugin.getSecurityGroup(client, majorOrder);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .retrieveSecurityGroupName(Mockito.eq(majorOrder));

        PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenNebulaClientUtil.getVirtualNetwork(Mockito.eq(this.client), Mockito.eq(majorOrder.getInstanceId()));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .getSecurityGroupContentFrom(Mockito.eq(virtualNetwork));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).findSecurityGroupByName(Mockito.eq(this.client),
                Mockito.eq(content), Mockito.eq(securityGroupName));
    }

    // test case: When calling the findSecurityGroupByName method, it must
    // verify that is call was successful.
    @Test
    public void testFindSecurityGroupByName() throws FogbowException {
        // set up
        String securityGroupId = TestUtils.FAKE_SECURITY_GROUP_ID;
        String securityGroupName = SystemConstants.PN_SECURITY_GROUP_PREFIX + TestUtils.FAKE_INSTANCE_ID;
        String content = String.format(SECURITY_GROUP_CONTENT_FORMAT, securityGroupId, ANOTHER_SECURITY_GROUP_ID);
        
        SecurityGroup securityGroup = mockSecurityGroupFromNetwork(securityGroupId, securityGroupName);
        PowerMockito.when(OpenNebulaClientUtil.getSecurityGroup(Mockito.eq(this.client), 
                Mockito.eq(securityGroupId))).thenReturn(securityGroup);

        // exercise
        this.plugin.findSecurityGroupByName(this.client, content, securityGroupName);

        // verify
        PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenNebulaClientUtil.getSecurityGroup(Mockito.eq(this.client), Mockito.eq(securityGroupId));
    }
    
    // test case: When calling the findSecurityGroupByName method with a security
    // group name incompatible, it must verify that an InstanceNotFoundException has
    // been thrown.
    @Test
    public void testFindSecurityGroupByNameFail() throws FogbowException {
        // set up
        String securityGroupId = TestUtils.FAKE_SECURITY_GROUP_ID;
        String securityGroupName = SystemConstants.PN_SECURITY_GROUP_PREFIX + TestUtils.FAKE_INSTANCE_ID;
        SecurityGroup securityGroup = mockSecurityGroupFromNetwork(securityGroupId, securityGroupName);
        PowerMockito.when(OpenNebulaClientUtil.getSecurityGroup(Mockito.eq(this.client), 
                Mockito.eq(securityGroupId))).thenReturn(securityGroup);
        
        String anotherSecurityGroupName = SystemConstants.PIP_SECURITY_GROUP_PREFIX + TestUtils.FAKE_INSTANCE_ID;
        String content = securityGroupId;
        
        String expected = Messages.Exception.INSTANCE_NOT_FOUND;

        try {
            // exercise
            this.plugin.findSecurityGroupByName(this.client, content, anotherSecurityGroupName);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the getSecurityGroupContentFrom method with a virtual
    // network containing valid security group content, it must verify that is call
    // was successful.
    @Test
    public void testGetSecurityGroupContentFromVirtualNetwork() throws FogbowException {
        // set up
        String content = TestUtils.FAKE_SECURITY_GROUP_ID;
        VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
        Mockito.when(virtualNetwork.xpath(OpenNebulaSecurityRulePlugin.SECURITY_GROUPS_PATH)).thenReturn(content);

        // exercise
        this.plugin.getSecurityGroupContentFrom(virtualNetwork);

        // verify
        Mockito.verify(virtualNetwork, Mockito.times(TestUtils.RUN_ONCE))
                .xpath(OpenNebulaSecurityRulePlugin.SECURITY_GROUPS_PATH);
    }
    
    // test case: When calling the getSecurityGroupContentFrom method with a virtual
    // network containing a null security group content, it must verify that an
    // InstanceNotFoundException has been thrown.
    @Test
    public void testGetSecurityGroupContentFromVirtualNetworkWithNullContent() throws FogbowException {
        // set up
        String content = null;
        VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
        Mockito.when(virtualNetwork.xpath(OpenNebulaSecurityRulePlugin.SECURITY_GROUPS_PATH)).thenReturn(content);
        
        String expected = Messages.Log.CONTENT_SECURITY_GROUP_NOT_DEFINED;

        try {
            // exercise
            this.plugin.getSecurityGroupContentFrom(virtualNetwork);
            Assert.fail();
        } catch (InternalServerErrorException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the getSecurityGroupContentFrom method with a virtual
    // network containing a empty security group content, it must verify that an
    // InstanceNotFoundException has been thrown.
    @Test
    public void testGetSecurityGroupContentFromVirtualNetworkWithEmptyContent() throws FogbowException {
        // set up
        String content = TestUtils.EMPTY_STRING;
        VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
        Mockito.when(virtualNetwork.xpath(OpenNebulaSecurityRulePlugin.SECURITY_GROUPS_PATH)).thenReturn(content);
        
        String expected = Messages.Log.CONTENT_SECURITY_GROUP_NOT_DEFINED;

        try {
            // exercise
            this.plugin.getSecurityGroupContentFrom(virtualNetwork);
            Assert.fail();
        } catch (InternalServerErrorException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the retrieveSecurityGroupName method with a NETWORk
    // resource type, it must return an expected security group name.
    @Test
    public void testRetrieveSecurityGroupNameFromNetworkOrder() throws FogbowException {
        // set up
        Order majorOrder = createMajorOrder(ResourceType.NETWORK);
        String expected = SystemConstants.PN_SECURITY_GROUP_PREFIX + majorOrder.getInstanceId();

        // exercise
        String securityGroupName = this.plugin.retrieveSecurityGroupName(majorOrder);

        // verify
        Assert.assertEquals(expected, securityGroupName);
    }
    
    // test case: When calling the retrieveSecurityGroupName method with a PUBLIC_IP
    // resource type, it must return an expected security group name.
    @Test
    public void testRetrieveSecurityGroupNameFromPublicIpOrder() throws FogbowException {
        // set up
        Order majorOrder = createMajorOrder(ResourceType.PUBLIC_IP);
        String expected = SystemConstants.PIP_SECURITY_GROUP_PREFIX + majorOrder.getInstanceId();

        // exercise
        String securityGroupName = this.plugin.retrieveSecurityGroupName(majorOrder);

        // verify
        Assert.assertEquals(expected, securityGroupName);
    }
    
    // test case: When calling the retrieveSecurityGroupName method with a different
    // resource type from NETWORK or PUBLIC_IP, it must verify that an
    // InvalidParameterException has been thrown.
    @Test
    public void testRetrieveSecurityGroupNameFail() throws FogbowException {
        // set up
        Order majorOrder = this.testUtils.createLocalComputeOrder();
        String expected = Messages.Exception.INVALID_RESOURCE;

        try {
            // exercise
            this.plugin.retrieveSecurityGroupName(majorOrder);
            Assert.fail();
        } catch (InvalidParameterException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the removeRule method with a
    // compatible rule, it must returned a true result.
    @Test
    public void testRemoveRule() {
        // set up
        Rule rule = buildRule();
        List<Rule> rules = buildRulesCollection(rule);

        // exercise
        boolean result = this.plugin.removeRule(rules, rule);

        // verify
        Assert.assertTrue(result);
    }
    
    // test case: When calling the removeRule method with a
    // null rule, it must returned a false result.
    @Test
    public void testRemoveRuleWithNullList() {
        // set up
        List<Rule> rules = null;
        Rule ruleToRemove = new Rule();

        // exercise
        boolean result = this.plugin.removeRule(rules, ruleToRemove);

        // verify
        Assert.assertFalse(result);
    }
    
    // test case: When calling the removeRule method with a
    // empty rule, it must returned a false result.
    @Test
    public void testRemoveRuleWithEmptyList() {
        // set up
        List<Rule> rules = new ArrayList<>();
        Rule ruleToRemove = new Rule();

        // exercise
        boolean result = this.plugin.removeRule(rules, ruleToRemove);

        // verify
        Assert.assertFalse(result);
    }
    
    // test case: When calling the removeRule method with a
    // incompatible rule, it must returned a false result.
    @Test
    public void testRemoveRuleWithIncompatibleRule() {
        // set up
        Rule rule = buildRule();
        List<Rule> rules = buildRulesCollection(rule);
        Rule ruleToRemove = new Rule();

        // exercise
        boolean result = this.plugin.removeRule(rules, ruleToRemove);

        // verify
        Assert.assertFalse(result);
    }
    
    // test case: When calling the buildSecurityRule method with a rule containing
    // an null range, it must returned a expected security rule instance.
    @Test
    public void testBuildSecurityRuleWithNullRange() {
        // set up
        Rule rule = Rule.builder()
                .protocol(SecurityRuleUtil.ALL_TEMPLATE_VALUE)
                .ip(CIDR_IP_ADDRESS)
                .size(CIDR_SIZE_VALUE)
                .range(null)
                .type(SecurityRuleUtil.INBOUND_TEMPLATE_VALUE)
                .groupId(TestUtils.FAKE_SECURITY_GROUP_ID)
                .build();

        SecurityRuleInstance expected = new SecurityRuleInstance(FAKE_SECURITY_RULE_INSTANCE_ID,
                Direction.IN, 
                OpenNebulaSecurityRulePlugin.MINIMUM_RANGE_PORT, 
                OpenNebulaSecurityRulePlugin.MAXIMUM_RANGE_PORT, 
                CIDR_FULL_ADDRESS, 
                EtherType.IPv4, 
                Protocol.ANY);

        // exercise
        SecurityRuleInstance instance = this.plugin.buildSecurityRule(rule);

        // verify
        Assert.assertEquals(expected.getId(), instance.getId());
        Assert.assertEquals(expected.getDirection(), instance.getDirection());
        Assert.assertEquals(expected.getPortFrom(), instance.getPortFrom());
        Assert.assertEquals(expected.getPortTo(), instance.getPortTo());
        Assert.assertEquals(expected.getCidr(), instance.getCidr());
        Assert.assertEquals(expected.getEtherType(), instance.getEtherType());
        Assert.assertEquals(expected.getProtocol(), instance.getProtocol());
    }
    
    // test case: When calling the buildSecurityRule method with a rule containing
    // an empty range, it must returned a expected security rule instance.
    @Test
    public void testBuildSecurityRuleWithemptyRange() {
        // set up
        Rule rule = Rule.builder()
                .protocol(SecurityRuleUtil.ALL_TEMPLATE_VALUE)
                .ip(CIDR_IP_ADDRESS)
                .size(CIDR_SIZE_VALUE)
                .range(TestUtils.EMPTY_STRING)
                .type(SecurityRuleUtil.INBOUND_TEMPLATE_VALUE)
                .groupId(TestUtils.FAKE_SECURITY_GROUP_ID)
                .build();

        SecurityRuleInstance expected = new SecurityRuleInstance(FAKE_SECURITY_RULE_INSTANCE_ID,
                Direction.IN,
                OpenNebulaSecurityRulePlugin.MINIMUM_RANGE_PORT, 
                OpenNebulaSecurityRulePlugin.MAXIMUM_RANGE_PORT, 
                CIDR_FULL_ADDRESS, 
                EtherType.IPv4, 
                Protocol.ANY);

        // exercise
        SecurityRuleInstance instance = this.plugin.buildSecurityRule(rule);

        // verify
        Assert.assertEquals(expected.getId(), instance.getId());
        Assert.assertEquals(expected.getDirection(), instance.getDirection());
        Assert.assertEquals(expected.getPortFrom(), instance.getPortFrom());
        Assert.assertEquals(expected.getPortTo(), instance.getPortTo());
        Assert.assertEquals(expected.getCidr(), instance.getCidr());
        Assert.assertEquals(expected.getEtherType(), instance.getEtherType());
        Assert.assertEquals(expected.getProtocol(), instance.getProtocol());
    }
    
    // test case: When calling the getRuleTypeBy method with an IN direction, it
    // must returned an inbound rule type.
    @Test
    public void testGetRuleTypeByInDirection() {
        // set up
        String expected = SecurityRuleUtil.INBOUND_TEMPLATE_VALUE;

        // exercise
        String type = this.plugin.getRuleTypeBy(Direction.IN);

        // verify
        Assert.assertEquals(expected, type);
    }
    
    // test case: When calling the getRuleTypeBy method with an OUT direction, it
    // must returned an outbound rule type.
    @Test
    public void testGetRuleTypeByOutDirection() {
        // set up
        String expected = SecurityRuleUtil.OUTBOUND_TEMPLATE_VALUE;

        // exercise
        String type = this.plugin.getRuleTypeBy(Direction.OUT);

        // verify
        Assert.assertEquals(expected, type);
    }
    
    // test case: When calling the getCidrFrom method with a valid CIDR, it must
    // return a expected address sliced. 
    @Test
    public void testGetCidrFromSecurityRuleWithValidCidr() {
        // set up
        SecurityRule securityRule = new SecurityRule();
        securityRule.setCidr(CIDR_FULL_ADDRESS);
        
        String[] expected = { CIDR_IP_ADDRESS, CIDR_SIZE_VALUE };

        // exercise
        String[] result = this.plugin.getCidrFrom(securityRule);

        // verify
        Assert.assertArrayEquals(expected, result);
    }
    
    // test case: When calling the getCidrFrom method with an invalid CIDR, it must
    // return a null result.
    @Test
    public void testGetCidrFromSecurityRuleWithInvalidCidr() {
        // set up
        SecurityRule securityRule = new SecurityRule();
        securityRule.setCidr(OpenNebulaSecurityRulePlugin.ALL_ADDRESSES_REMOTE_PREFIX);

        String[] expected = null;

        // exercise
        String[] result = this.plugin.getCidrFrom(securityRule);

        // verify
        Assert.assertArrayEquals(expected, result);
    }
    
    // test case: When calling the getRulesFrom method with a valid response, it
    // must return an expected rules list.
    @Test
    public void testGetRulesFromResponse() {
        // set up
        Rule rule = buildRule();
        List<Rule> expected = buildRulesCollection(rule);
        GetSecurityGroupResponse response = buildSecurityGroupResponse(expected);

        // exercise
        List<Rule> rules = this.plugin.getRulesFrom(response);

        // verify
        Assert.assertEquals(expected, rules);
    }
    
    // test case: When calling the getRulesFrom method with a null response, it must
    // return an empty rules list.
    @Test
    public void testGetRulesFromNullResponse() {
        // set up
        List<Rule> rules = null;
        GetSecurityGroupResponse response = buildSecurityGroupResponse(rules);

        // exercise
        rules = this.plugin.getRulesFrom(response);

        // verify
        Assert.assertTrue(rules.isEmpty());
    }
    
    // test case: When calling the getNetworkIdFrom method with rule containing a
    // valid network ID, it must returned this network ID.
    @Test
    public void testGetNetworkIdFromRule() {
        // set up
        String expected = TestUtils.FAKE_NETWORK_ID;
        Rule rule = Rule.builder()
                .networkId(expected)
                .build();

        // exercise
        String networkId = this.plugin.getNetworkIdFrom(rule);

        // verify
        Assert.assertEquals(expected, networkId);
    }
    
    // test case: When calling the getNetworkIdFrom method with rule containing a
    // null network ID, it must returned an empty string.
    @Test
    public void testGetNetworkIdFromRuleWithNullID() {
        // set up
        Rule rule = Rule.builder()
                .networkId(null)
                .build();

        String expected = TestUtils.EMPTY_STRING;
        
        // exercise
        String networkId = this.plugin.getNetworkIdFrom(rule);

        // verify
        Assert.assertEquals(expected, networkId);
    }
    
    // test case: When calling the getIpAddress method with a security rule
    // containing a valid CIDR, it must returned an expected IP address. 
    @Test
    public void testGetIpAddress() {
        // set up
        SecurityRule securityRule = createSecurityRule(Direction.IN);
        
        String expected = CIDR_IP_ADDRESS;

        // exercise
        String ipAddress = this.plugin.getIpAddress(securityRule);

        // verify
        Assert.assertEquals(expected, ipAddress);
    }
    
    // test case: When calling the getIpAddress method with a security rule
    // containing an invalid CIDR, it must returned a null IP address.
    @Test
    public void testGetIpAddressReturnedNullIpAddress() {
        // set up
        SecurityRule securityRule = createSecurityRule(Direction.IN);
        securityRule.setCidr(OpenNebulaSecurityRulePlugin.ALL_ADDRESSES_REMOTE_PREFIX);

        String expected = null;

        // exercise
        String ipAddress = this.plugin.getIpAddress(securityRule);

        // verify
        Assert.assertEquals(expected, ipAddress);
    }

    // test case: When calling the getAddressSize method with a security rule
    // containing a valid CIDR, it must returned an expected address size value.
    @Test
    public void testGetAddressSize() {
     // set up
        SecurityRule securityRule = createSecurityRule(Direction.IN);
        
        String expected = CIDR_SIZE_VALUE;

        // exercise
        String addressSize = this.plugin.getAddressSize(securityRule);

        // verify
        Assert.assertEquals(expected, addressSize);
    }
    
    // test case: When calling the getAddressSize method with a security rule
    // containing an invalid CIDR, it must returned a null address size value.
    @Test
    public void testGetAddressReturnedNullAddressSize() {
     // set up
        SecurityRule securityRule = createSecurityRule(Direction.IN);
        securityRule.setCidr(OpenNebulaSecurityRulePlugin.ALL_ADDRESSES_REMOTE_PREFIX);

        String expected = null;

        // exercise
        String addressSize = this.plugin.getAddressSize(securityRule);

        // verify
        Assert.assertEquals(expected, addressSize);
    }
    
    // test case: When calling the createSecurityRuleRequest method with security
    // rule containing the same value from ports, it must verify that is call was
    // successful and returned an expected rule with specific range port.
    @Test
    public void testCreateSecurityRuleRequestWithSameValueFromPorts() {
        // set up
        Direction direction = Direction.IN;
        SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);
        SecurityRule securityRule = createSecurityRule(direction);
        securityRule.setPortFrom(DEFAULT_SSH_PORT);
        securityRule.setPortTo(DEFAULT_SSH_PORT);
        
        Rule expected = Rule.builder()
                .protocol(SecurityRuleUtil.TCP_TEMPLATE_VALUE)
                .ip(CIDR_IP_ADDRESS)
                .size(CIDR_SIZE_VALUE)
                .range(String.valueOf(DEFAULT_SSH_PORT))
                .type(SecurityRuleUtil.INBOUND_TEMPLATE_VALUE)
                .groupId(TestUtils.FAKE_SECURITY_GROUP_ID)
                .build();
        
        // exercise
        Rule rule = this.plugin.createSecurityRuleRequest(securityRule, securityGroup);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getIpAddress(Mockito.eq(securityRule));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getAddressSize(Mockito.eq(securityRule));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getRuleTypeBy(direction);
        
        Assert.assertEquals(expected.getRange(), rule.getRange());
    }
    
    private String generateTemplateResponse(List<Rule> rules) {
        GetSecurityGroupResponse template = buildSecurityGroupResponse(rules);
        return template.marshalTemplate();
    }
    
    private SecurityGroup mockSecurityGroupFromNetwork(String securityGroupId, String securityGroupName) {
        SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);
        Mockito.when(securityGroup.getId()).thenReturn(securityGroupId);
        Mockito.when(securityGroup.getName())
                .thenReturn(securityGroupName);
        
        return securityGroup;
    }
    
    private String generateRequestTemplate(List<Rule> rules) {
        CreateSecurityGroupRequest request = CreateSecurityGroupRequest.builder()
                .id(TestUtils.FAKE_SECURITY_GROUP_ID)
                .name(SystemConstants.PN_SECURITY_GROUP_PREFIX + TestUtils.FAKE_ORDER_ID)
                .rules(rules)
                .build();
        
        return request.marshalTemplate();
    }
    
    private GetSecurityGroupResponse buildSecurityGroupResponse(List<Rule> rules) {
        GetSecurityGroupResponse response = GetSecurityGroupResponse.builder()
                .id(TestUtils.FAKE_SECURITY_GROUP_ID)
                .name(SystemConstants.PN_SECURITY_GROUP_PREFIX + TestUtils.FAKE_ORDER_ID)
                .template(GetSecurityRulesTemplate.builder()
                        .rules(rules)
                        .build())
                .build();
        
        return response;
    }
    
    private List<Rule> buildRulesCollection(Rule...rules) {
        List<Rule> rulesList = new ArrayList<>();
        for (Rule rule : rules) {
            rulesList.add(rule);
        }
        return rulesList;
    }
    
    private String defineSecurityRuleId(Direction direction) {
        String securityRuleId = String.format(SECURITY_RULE_IDENTIFIER_FORMAT,
                TestUtils.FAKE_SECURITY_GROUP_ID, 
                TestUtils.EMPTY_STRING,
                direction.equals(Direction.IN) ? SecurityRuleUtil.INBOUND_TEMPLATE_VALUE : SecurityRuleUtil.OUTBOUND_TEMPLATE_VALUE,
                CIDR_IP_ADDRESS, 
                CIDR_SIZE_VALUE,
                RANGE_PORTS_VALUE,
                Protocol.TCP.name());

        return securityRuleId;
    }
    
    private List<SecurityRuleInstance> createSecurityRuleInstancesCollection() {
        SecurityRuleInstance[] instances = { 
                createSeurityRuleInstance(Direction.IN),
                createSeurityRuleInstance(Direction.OUT) 
        };
        return Arrays.asList(instances);
    }

    private SecurityRuleInstance createSeurityRuleInstance(Direction direction) {
        String id = defineSecurityRuleId(direction);
        int portFrom = OpenNebulaSecurityRulePlugin.MINIMUM_RANGE_PORT;
        int portTo = OpenNebulaSecurityRulePlugin.MAXIMUM_RANGE_PORT;
        String cidr = CIDR_FULL_ADDRESS;
        EtherType etherType = EtherType.IPv4;
        Protocol protocol = Protocol.ANY;
        return new SecurityRuleInstance(id, direction, portFrom, portTo, cidr, etherType, protocol);
    }

    private Rule buildRule() {
        Rule rule = Rule.builder()
                .protocol(SecurityRuleUtil.TCP_TEMPLATE_VALUE)
                .ip(CIDR_IP_ADDRESS)
                .size(CIDR_SIZE_VALUE)
                .range(RANGE_PORTS_VALUE)
                .type(SecurityRuleUtil.INBOUND_TEMPLATE_VALUE)
                .groupId(TestUtils.FAKE_SECURITY_GROUP_ID)
                .build();
        
        return rule;
    }
    
    private SecurityGroup getSecurityGroupMocked(Order majorOrder) throws FogbowException {
        SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);
        Mockito.doReturn(securityGroup).when(this.plugin).getSecurityGroup(Mockito.eq(this.client),
                Mockito.eq(majorOrder));
        
        Mockito.when(securityGroup.getId()).thenReturn(TestUtils.FAKE_SECURITY_GROUP_ID);
        Mockito.when(securityGroup.getName()).thenReturn(SystemConstants.PN_SECURITY_GROUP_PREFIX + TestUtils.FAKE_ORDER_ID);
        
        return securityGroup;
    }
    
    private Order createMajorOrder(ResourceType resourceType) {
        Order majorOrder = null;
        switch (resourceType) {
        case NETWORK:
            majorOrder = this.testUtils.createLocalNetworkOrder();
            majorOrder.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
            break;
        case PUBLIC_IP:
            majorOrder = this.testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);
            majorOrder.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        default:
            break;
        }
        return majorOrder;
    }
    
    private SecurityRule createSecurityRule(Direction direction) {
        Protocol protocol = Protocol.ANY;
        EtherType etherType = EtherType.IPv4;
        int portFrom = OpenNebulaSecurityRulePlugin.MINIMUM_RANGE_PORT;
        int portTo = OpenNebulaSecurityRulePlugin.MAXIMUM_RANGE_PORT;
        String cidr = CIDR_FULL_ADDRESS;
        return new SecurityRule(direction, portFrom, portTo, cidr, etherType, protocol);
    }
    
}
