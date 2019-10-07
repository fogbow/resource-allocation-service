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
import cloud.fogbow.common.exceptions.UnexpectedException;
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

    private static final String OPENNEBULA_ENDPOINT = "http://localhost:2633/RPC2";
    private static final String SECURITY_RULE_IDENTIFIER_FORMAT = "%s@%s@%s@%s@%s@%s@%s";
    
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

        String expected = "Rule not available for deletion.";

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
    // template, it must verify that an UnexpectedException has been thrown.
    @Test
    public void testUpdateSecurityGroupFail() throws FogbowException {
        // set up
        String template = TestUtils.ANY_VALUE;

        SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);
        OneResponse response = Mockito.mock(OneResponse.class);
        Mockito.when(securityGroup.update(Mockito.eq(template))).thenReturn(response);
        Mockito.when(response.isError()).thenReturn(true);

        String expected = String.format(Messages.Error.ERROR_WHILE_UPDATING_SECURITY_GROUPS, template);

        try {
            // exercise
            this.plugin.updateSecurityGroup(securityGroup, template);
            Assert.fail();
        } catch (UnexpectedException e) {
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

    // test case: ...
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

        String content = String.format("%s,%s", securityGroupId, "another-security-group-id");
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

	// test case: ...
    @Test
    public void testFindSecurityGroupByName() throws FogbowException {
        // set up
        String securityGroupId = TestUtils.FAKE_SECURITY_GROUP_ID;
        String securityGroupName = SystemConstants.PN_SECURITY_GROUP_PREFIX + TestUtils.FAKE_INSTANCE_ID;
        String content = String.format("%s,%s", securityGroupId, "another-security-group-id");
        
        SecurityGroup securityGroup = mockSecurityGroupFromNetwork(securityGroupId, securityGroupName);
        PowerMockito.when(OpenNebulaClientUtil.getSecurityGroup(Mockito.eq(this.client), 
                Mockito.eq(securityGroupId))).thenReturn(securityGroup);

        // exercise
        this.plugin.findSecurityGroupByName(this.client, content, securityGroupName);

        // verify
        PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenNebulaClientUtil.getSecurityGroup(Mockito.eq(this.client), Mockito.eq(securityGroupId));
    }
    
    // test case: ...
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
    
    // test case: ...
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
    
    // test case: ...
    @Test
    public void testGetSecurityGroupContentFromVirtualNetworkFail() throws FogbowException {
        // set up
        String content = null;
        VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
        Mockito.when(virtualNetwork.xpath(OpenNebulaSecurityRulePlugin.SECURITY_GROUPS_PATH)).thenReturn(content);
        
        String expected = Messages.Error.CONTENT_SECURITY_GROUP_NOT_DEFINED;

        try {
            // exercise
            this.plugin.getSecurityGroupContentFrom(virtualNetwork);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: ...
    @Test
    public void testRetrieveSecurityGroupNameFromNetworkOrder() throws FogbowException {
        // set up
        Order majorOrder = createMajorOrder(ResourceType.NETWORK);
        String expected = SystemConstants.PN_SECURITY_GROUP_PREFIX + majorOrder.getId();

        // exercise
        String securityGroupName = this.plugin.retrieveSecurityGroupName(majorOrder);

        // verify
        Assert.assertEquals(expected, securityGroupName);
    }
    
    // test case: ...
    @Test
    public void testRetrieveSecurityGroupNameFromPublicIpOrder() throws FogbowException {
        // set up
        Order majorOrder = createMajorOrder(ResourceType.PUBLIC_IP);
        String expected = SystemConstants.PIP_SECURITY_GROUP_PREFIX + majorOrder.getId();

        // exercise
        String securityGroupName = this.plugin.retrieveSecurityGroupName(majorOrder);

        // verify
        Assert.assertEquals(expected, securityGroupName);
    }
    
    // test case: ...
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
                .name("fake-security-group-name")
                .rules(rules)
                .build();
        
        return request.marshalTemplate();
    }
    
    private GetSecurityGroupResponse buildSecurityGroupResponse(List<Rule> rules) {
        GetSecurityGroupResponse response = GetSecurityGroupResponse.builder()
                .id(TestUtils.FAKE_SECURITY_GROUP_ID)
                .name("fake-security-group-name")
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
                "",
                direction.equals(Direction.IN) ? "inbound" : "outbound",
                "0.0.0.0", 
                "0",
                "1:65536",
                Protocol.ANY);

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
        String id = TestUtils.FAKE_INSTANCE_ID;
        int portFrom = OpenNebulaSecurityRulePlugin.MINIMUM_RANGE_PORT;
        int portTo = OpenNebulaSecurityRulePlugin.MAXIMUM_RANGE_PORT;
        String cidr = OpenNebulaSecurityRulePlugin.ALL_ADDRESSES_REMOTE_PREFIX;
        EtherType etherType = EtherType.IPv4;
        Protocol protocol = Protocol.ANY;
        return new SecurityRuleInstance(id, direction, portFrom, portTo, cidr, etherType, protocol);
    }

    private Rule buildRule() {
        Rule rule = Rule.builder()
                .protocol("any")
                .ip("0.0.0.0")
                .size("0")
                .range("1:65536")
                .type("inbound")
                .groupId(TestUtils.FAKE_SECURITY_GROUP_ID)
                .build();
        
        return rule;
    }
    
    private SecurityGroup getSecurityGroupMocked(Order majorOrder) throws FogbowException {
        SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);
        Mockito.doReturn(securityGroup).when(this.plugin).getSecurityGroup(Mockito.eq(this.client),
                Mockito.eq(majorOrder));
        
        Mockito.when(securityGroup.getId()).thenReturn(TestUtils.FAKE_SECURITY_GROUP_ID);
        Mockito.when(securityGroup.getName()).thenReturn("fake-security-group-name");
        
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
        String cidr = OpenNebulaSecurityRulePlugin.ALL_ADDRESSES_REMOTE_PREFIX;
        return new SecurityRule(direction, portFrom, portTo, cidr, etherType, protocol);
    }
    
}
