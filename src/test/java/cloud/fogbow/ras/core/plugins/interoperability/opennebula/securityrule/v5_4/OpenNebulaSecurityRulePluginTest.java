package cloud.fogbow.ras.core.plugins.interoperability.opennebula.securityrule.v5_4;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opennebula.client.secgroup.SecurityGroup;
import org.opennebula.client.vnet.VirtualNetwork;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.api.parameters.SecurityRule.Direction;
import cloud.fogbow.ras.api.parameters.SecurityRule.EtherType;
import cloud.fogbow.ras.api.parameters.SecurityRule.Protocol;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaBaseTests;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;

@PrepareForTest({ DatabaseManager.class, OpenNebulaClientUtil.class, VirtualNetwork.class })
public class OpenNebulaSecurityRulePluginTest extends OpenNebulaBaseTests {

    private OpenNebulaSecurityRulePlugin plugin;

    @Before
    public void setUp() throws FogbowException {
        super.setUp();
        this.plugin = Mockito.spy(new OpenNebulaSecurityRulePlugin(this.openNebulaConfFilePath));
    }
    
    // test case: ...
    @Test
    public void testRequestSecurityRule() throws FogbowException {
        // set up
        Order majorOrder = createMajorOrder();
        SecurityRule securityRule = createSecurityRule(Direction.IN);

        SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);
        Mockito.doReturn(securityGroup).when(this.plugin).getSecurityGroup(Mockito.eq(this.client),
                Mockito.eq(majorOrder));
        
        Rule rule = createRequestRule();
        Mockito.doReturn(rule).when(this.plugin).createSecurityRuleRequest(Mockito.eq(securityRule), Mockito.eq(securityGroup));
        
        Mockito.doReturn(TestUtils.FAKE_INSTANCE_ID).when(this.plugin).doRequestSecurityRule(Mockito.eq(securityGroup),
                Mockito.eq(rule));

        // exercise
        this.plugin.requestSecurityRule(securityRule, majorOrder, this.cloudUser);

        // verify
        PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getSecurityGroup(Mockito.eq(this.client),
                Mockito.eq(majorOrder));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .createSecurityRuleRequest(Mockito.eq(securityRule), Mockito.eq(securityGroup));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doRequestSecurityRule(Mockito.eq(securityGroup),
                Mockito.eq(rule));
    }

    private Rule createRequestRule() {
        Rule rule = Rule.builder().protocol("any")
                .ip("0.0.0.0")
                .size("0")
                .range("1:65536")
                .type("inbound")
                .groupId("fake-group-id")
                .build();
        return rule;
    }
    
    private Order createMajorOrder() {
        NetworkOrder majorOrder = this.testUtils.createLocalNetworkOrder();
        majorOrder.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
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
    
    // test case: ...
    @Test
    public void testGetSecurityRules() {
        // set up
        
        // exercise
        
        // verify
    }
    
    // test case: ...
    @Test
    public void testDeleteSecurityRule() {
        // set up
        
        // exercise
        
        // verify
    }
    
    // test case: ...
    @Test
    public void testDoDeleteSecurityRule() {
        // set up
        
        // exercise
        
        // verify
    }
    
    // test case: ...
    @Test
    public void testtestDoDeleteSecurityRuleFail() {
        // set up
        
        // exercise
        
        // verify
    }
    
    // test case: ...
    @Test
    public void testDoUnpakingSecurityRuleId() {
        // set up
        
        // exercise
        
        // verify
    }
    
    // test case: ...
    @Test
    public void testDoUnpakingSecurityRuleIdFail() {
        // set up
        
        // exercise
        
        // verify
    }
    
    // test case: ...
    @Test
    public void testDoGetSecurityRules() {
        // set up

        // exercise

        // verify
    }
    
    // test case: ...
    @Test
    public void testBuildAddressCidrWithNullEtherType() {
        // set up

        // exercise

        // verify
    }
    
    // test case: ...
    @Test
    public void testBuildAddressCidrWithNullSize() {
        // set up

        // exercise

        // verify
    }
    
    // test case: ...
    @Test
    public void testBuildAddressCidrWithEmptySize() {
        // set up

        // exercise

        // verify
    }
    
    // test case: ...
    @Test
    public void testCalculateCidrWithEtherTypeIPv4() {
        // set up

        // exercise

        // verify
    }
    
    // test case: ...
    @Test
    public void testCalculateCidrWithEtherTypeIPv6() {
        // set up

        // exercise

        // verify
    }
    
    // test case: ...
    @Test
    public void testGetPortInRange() {
        // set up

        // exercise

        // verify
    }
    
    // test case: ...
    @Test
    public void testGetProtocolFrom() {
        // set up

        // exercise

        // verify
    }
    
    // test case: ...
    @Test
    public void testGetEtherTypeFrom() {
        // set up

        // exercise

        // verify
    }
    
    // test case: ...
    @Test
    public void testGetDirectionFrom() {
        // set up

        // exercise

        // verify
    }
    
    // test case: ...
    @Test
    public void testDoRequestSecurityRule() {
        // set up

        // exercise

        // verify
    }
    
    // test case: ...
    @Test
    public void testUpdateSecurityGroup() {
        // set up

        // exercise

        // verify
    }
    
    // test case: ...
    @Test
    public void testUpdateSecurityGroupFail() {
        // set up

        // exercise

        // verify
    }
    
    // test case: ...
    @Test
    public void testDoPackingSecurityRuleId() {
        // set up

        // exercise

        // verify
    }
    
    // test case: ...
    @Test
    public void testDoCreateSecurityGroupRequest() {
        // set up

        // exercise

        // verify
    }
    
    // test case: ...
    @Test
    public void testDoGetSecurityGroupResponse() {
        // set up

        // exercise

        // verify
    }
    
    // test case: ...
    @Test
    public void testCreateSecurityRuleRequest() {
        // set up

        // exercise

        // verify
    }
    
    // test case: ...
    @Test
    public void testGetRuleTypeBy() {
        // set up

        // exercise

        // verify
    }
    
    // test case: ...
    @Test
    public void testGetCidrFrom() {
        // set up

        // exercise

        // verify
    }
    
    // test case: ...
    @Test
    public void testGetSecurityGroup() {
        // set up

        // exercise

        // verify
    }
    
    // test case: ...
    @Test
    public void testFindSecurityGroupByName() {
        // set up

        // exercise

        // verify
    }
    
    // test case: ...
    @Test
    public void testFindSecurityGroupByNameFail() {
        // set up

        // exercise

        // verify
    }
    
    // test case: ...
    @Test
    public void testGetSecurityGroupContentFromVirtualNetwork() {
        // set up

        // exercise

        // verify
    }
    
    // test case: ...
    @Test
    public void testGetSecurityGroupContentFromVirtualNetworkFail() {
        // set up

        // exercise

        // verify
    }
    
    // test case: ...
    @Test
    public void testRetrieveSecurityGroupName() {
        // set up

        // exercise

        // verify
    }
    
//  private static final String DEFAULT_SECURITY_GROUP_ID = "0";
//  private static final String FAKE_CIDR = "10.10.10.0/24";
//  private static final String FAKE_ID_VALUE = "100";
//  private static final String FAKE_INSTANCE_ID = "fake-instance-id";
//  private static final String FAKE_IP_RANGE_SIZE = "256";
//  private static final String FAKE_IPV4_VALUE = "10.10.0.1";
//  private static final String FAKE_SECURITY_GROUP_NAME = "fake-security-group-name";
//  private static final String FAKE_USER_ID = "fake-user-id";
//  private static final String FAKE_USER_NAME = "fakeuser-name";
//  private static final String FAKE_TOKEN_VALUE = "fake-token-value";
//  private static final String FORMAT_CONTENT = "%s%s%s";
//  private static final String IP_ONE = "10.10.0.0";
//  private static final String IP_TWO = "20.20.0.0";
//  private static final String IP_TREE = "30.30.0.0";
//  private static final String SEPARATOR = ",";
//  private static final String OPENNEBULA_CLOUD_NAME_DIRECTORY = "opennebula";

//    // test case: success case when call the deleteSecurityRule method.
//    @Test
//	public void testDeleteSecurityRule() throws FogbowException {
//		// setup
//		String securityGroupId = DEFAULT_SECURITY_GROUP_ID;
//		String securityGroupName = FAKE_SECURITY_GROUP_NAME;
//
//		List<Rule> rules = new ArrayList<>();
//		Rule ruleOneToRemove = createSecurityRuleId(IP_ONE, securityGroupId);
//		rules.add(ruleOneToRemove);
//		rules.add(createSecurityRuleId(IP_TWO, securityGroupId));
//		rules.add(createSecurityRuleId(IP_TREE, securityGroupId));
//
//		List<Rule> rulesExpected = new ArrayList<>(rules);
//		rulesExpected.remove(ruleOneToRemove);
//		SecurityGroupTemplate securityGroupTemplate = createSecurityGroupTemplate(securityGroupId, securityGroupName,
//				rulesExpected);
//		String securityGroupTemplateXml = securityGroupTemplate.marshalTemplate();
//
//		OneResponse oneResponseExpected = Mockito.mock(OneResponse.class);
//		Mockito.when(oneResponseExpected.isError()).thenReturn(false);
//
//		SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);
//		Mockito.when(securityGroup.update(Mockito.anyString())).thenReturn(oneResponseExpected);
//
//		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
//		BDDMockito.given(OpenNebulaClientUtil.getSecurityGroup(Mockito.any(Client.class), Mockito.eq(securityGroupId)))
//				.willReturn(securityGroup);
//
//		SecurityGroupInfo securityGroupInfo = Mockito.mock(SecurityGroupInfo.class);
//		Mockito.when(securityGroupInfo.getId()).thenReturn(securityGroupId);
//		Mockito.when(securityGroupInfo.getName()).thenReturn(securityGroupName);
//
//		Mockito.doReturn(securityGroupInfo).when(this.plugin).getSecurityGroupInfo(Mockito.eq(securityGroup));
//		Mockito.doReturn(rules).when(this.plugin).getRules(Mockito.eq(securityGroupInfo));
//
//		CloudUser cloudUser = createCloudUser();
//		String securityRuleId = ruleOneToRemove.serialize();
//
//		// exercise
//		this.plugin.deleteSecurityRule(securityRuleId, cloudUser);
//
//		// verify
//		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
//		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());
//
//		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
//		OpenNebulaClientUtil.getSecurityGroup(Mockito.any(Client.class), Mockito.eq(securityGroupId));
//
//		Mockito.verify(oneResponseExpected, Mockito.times(1)).isError();
//		Mockito.verify(securityGroup, Mockito.times(1)).update(Mockito.eq(securityGroupTemplateXml));
//		Mockito.verify(securityGroupInfo, Mockito.times(1)).getId();
//		Mockito.verify(securityGroupInfo, Mockito.times(1)).getName();
//		Mockito.verify(this.plugin, Mockito.times(1)).getSecurityGroupInfo(Mockito.eq(securityGroup));
//		Mockito.verify(this.plugin, Mockito.times(1)).getRules(Mockito.eq(securityGroupInfo));
//	}

//    // test case: Occur an error when updating the security group in the cloud.
//    @Test(expected = FogbowException.class)
//	public void testDeleteSecurityRuleErrorWhileUpdatingSecurity() throws FogbowException {
//		// setup
//		String ipOne = IP_ONE;
//		String securityGroupId = DEFAULT_SECURITY_GROUP_ID;
//		String securityGroupName = FAKE_SECURITY_GROUP_NAME;
//
//		List<Rule> rules = new ArrayList<>();
//		Rule ruleOneToRemove = createSecurityRuleId(ipOne, securityGroupId);
//		rules.add(ruleOneToRemove);
//
//		List<Rule> rulesExpected = new ArrayList<>(rules);
//		rulesExpected.remove(ruleOneToRemove);
//		SecurityGroupTemplate securityGroupTemplate = createSecurityGroupTemplate(securityGroupId, securityGroupName,
//				rulesExpected);
//		String securityGroupTemplateXml = securityGroupTemplate.marshalTemplate();
//
//		OneResponse oneResponseExpected = Mockito.mock(OneResponse.class);
//		Mockito.when(oneResponseExpected.isError()).thenReturn(true);
//
//		SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);
//		Mockito.when(securityGroup.update(Mockito.anyString())).thenReturn(oneResponseExpected);
//
//		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
//		BDDMockito.given(OpenNebulaClientUtil.getSecurityGroup(Mockito.any(Client.class), Mockito.eq(securityGroupId)))
//				.willReturn(securityGroup);
//
//		SecurityGroupInfo securityGroupInfo = Mockito.mock(SecurityGroupInfo.class);
//		Mockito.when(securityGroupInfo.getId()).thenReturn(securityGroupId);
//		Mockito.when(securityGroupInfo.getName()).thenReturn(securityGroupName);
//
//		Mockito.doReturn(securityGroupInfo).when(this.plugin).getSecurityGroupInfo(Mockito.eq(securityGroup));
//
//		Mockito.doReturn(rules).when(this.plugin).getRules(Mockito.eq(securityGroupInfo));
//
//		CloudUser cloudUser = createCloudUser();
//		String securityRuleId = ruleOneToRemove.serialize();
//
//		// exercise
//		this.plugin.deleteSecurityRule(securityRuleId, cloudUser);
//
//		// verify
//		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
//		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());
//
//		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
//		OpenNebulaClientUtil.getSecurityGroup(Mockito.any(Client.class), Mockito.eq(securityGroupId));
//
//		Mockito.verify(oneResponseExpected, Mockito.times(1)).isError();
//		Mockito.verify(securityGroup, Mockito.times(1)).update(Mockito.eq(securityGroupTemplateXml));
//		Mockito.verify(securityGroupInfo, Mockito.times(1)).getId();
//		Mockito.verify(securityGroupInfo, Mockito.times(1)).getName();
//		Mockito.verify(this.plugin, Mockito.times(1)).getSecurityGroupInfo(Mockito.eq(securityGroup));
//		Mockito.verify(this.plugin, Mockito.times(1)).getRules(Mockito.eq(securityGroupInfo));
//	}

//    // test case: the security rule id came in a unknown format.
//    @Test(expected = FogbowException.class)
//    public void testCreateRuleException() throws FogbowException {
//        // setup
//        String securityRuleId = "wrong";
//        // exercise
//        this.plugin.createRule(securityRuleId);
//    }

//    // test case: trying to remove a rule that there is not in the rules.
//    @Test
//    public void testRemoveUnknownRule() {
//        // setup
//        List<Rule> rules = new ArrayList<>();
//        Rule ruleToRemove = new Rule();
//        ruleToRemove.setIp(IP_ONE);
//        rules.add(new Rule());
//        rules.add(new Rule());
//        rules.add(new Rule());
//        int ruleSizeExpected = 3 ;
//
//        // verify before
//        Assert.assertEquals(ruleSizeExpected, rules.size());
//
//        // exercise
//        this.plugin.removeRule(ruleToRemove, rules);
//
//        // verify after
//        Assert.assertEquals(ruleSizeExpected, rules.size());
//    }

//    // test case: the rules is null and this will not throw exception.
//    @Test
//    public void testRemoveRuleNullRules() {
//        // exercise and verify
//        try {
//            this.plugin.removeRule(null, null);
//        } catch (Exception e) {
//            Assert.fail();
//        }
//    }

//    // test case: throw exception when security group is null.
//    @Test(expected = FogbowException.class)
//    public void testCreateSecurityGroupTemplate() throws FogbowException {
//        // set up
//        List<Rule> rules = new ArrayList<>();
//        SecurityGroupInfo securityGroupInfo = null;
//        // exercise
//        this.plugin.createSecurityGroupTemplate(securityGroupInfo, rules);
//    }

//    // test case: success case when call the getSecurityRules method.
//    @Test
//	public void testGetSecurityRules() throws FogbowException {
//		// setup
//		VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
//		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
//		BDDMockito.given(OpenNebulaClientUtil.getVirtualNetwork(Mockito.any(Client.class), Mockito.anyString()))
//				.willReturn(virtualNetwork);
//
//		// created by opennebula deploy
//		String defaultSecurityGroupId = DEFAULT_SECURITY_GROUP_ID;
//		
//		// created by Fogbow (RAS)
//		String securityGroupId = FAKE_ID_VALUE;
//		String securityGroupXMLContent = String.format(FORMAT_CONTENT, defaultSecurityGroupId,
//				OpenNebulaSecurityRulePlugin.CONTENT_SEPARATOR, securityGroupId);
//		Mockito.when(virtualNetwork.xpath(Mockito.eq(OpenNebulaSecurityRulePlugin.SECURITY_GROUPS_PATH)))
//				.thenReturn(securityGroupXMLContent);
//
//		SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);
//		BDDMockito.given(OpenNebulaClientUtil.getSecurityGroup(Mockito.any(Client.class), Mockito.eq(securityGroupId)))
//				.willReturn(securityGroup);
//
//		List<Rule> rules = new ArrayList<Rule>();
//		int portFrom = 22;
//		int portTo = 3000;
//		String ipv4 = FAKE_IPV4_VALUE;
//		String sizeRangeIp = FAKE_IP_RANGE_SIZE; // subnet is 24
//
//		Rule rule = new Rule(Rule.TCP_XML_TEMPLATE_VALUE, ipv4, sizeRangeIp,
//				String.format(FORMAT_CONTENT, portFrom, Rule.OPENNEBULA_RANGE_SEPARATOR, portTo),
//				Rule.INBOUND_XML_TEMPLATE_VALUE, null, securityGroupId);
//
//		rules.add(rule);
//
//		SecurityGroupInfo securityGroupInfo = Mockito.mock(SecurityGroupInfo.class);
//		Mockito.doReturn(securityGroupInfo).when(this.plugin).getSecurityGroupInfo(Mockito.eq(securityGroup));
//		Mockito.doReturn(rules).when(this.plugin).getRules(Mockito.eq(securityGroupInfo));
//
//		CloudUser cloudUser = createCloudUser();
//		Order majorOrder = new NetworkOrder();
//		String networkId = FAKE_INSTANCE_ID;
//		majorOrder.setInstanceId(networkId);
//
//		// exercise
//		List<SecurityRuleInstance> securityRuleInstances = this.plugin.getSecurityRules(majorOrder, cloudUser);
//
//		// verify
//		SecurityRuleInstance securityRuleInstance = securityRuleInstances.iterator().next();
//		Assert.assertEquals(SecurityRule.Protocol.TCP, securityRuleInstance.getProtocol());
//		Assert.assertEquals(portFrom, securityRuleInstance.getPortFrom());
//		Assert.assertEquals(portTo, securityRuleInstance.getPortTo());
//		Assert.assertEquals(String.format(FORMAT_CONTENT, ipv4, Rule.CIRD_SEPARATOR, 24), securityRuleInstance.getCidr());
//		Assert.assertEquals(SecurityRule.Direction.IN, securityRuleInstance.getDirection());
//		Assert.assertEquals(SecurityRule.EtherType.IPv4, securityRuleInstance.getEtherType());
//		Assert.assertEquals(rule.serialize(), securityRuleInstance.getId());
//	}

//    // test case: error while trying to get rules.
//    @Test(expected = FogbowException.class)
//    public void testGetSecurityRulesErrorWhileGetRules() throws FogbowException {
//        // setup
//        SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);
//        SecurityGroupInfo securityGroupInfo = Mockito.mock(SecurityGroupInfo.class);
//        Mockito.doReturn(securityGroupInfo).when(this.plugin).getSecurityGroupInfo(Mockito.eq(securityGroup));
//        Mockito.doThrow(Exception.class).when(this.plugin).getRules(Mockito.eq(securityGroupInfo));
//
//        // exercise
//        this.plugin.getSecurityRules(securityGroup);
//    }

//    // test case: there is not security group in the opennebula response(xml).
//    @Test
//    public void testGetSecurityGroupByEmptyContent() {
//        // setup
//        VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
//        Mockito.when(virtualNetwork.xpath(Mockito.eq(OpenNebulaSecurityRulePlugin.SECURITY_GROUPS_PATH)))
//                .thenReturn(null);
//
//        // exercise
//        String securityGroupXMLContent = this.plugin.getSecurityGroupBy(virtualNetwork);
//
//        // verify
//        Assert.assertNull(securityGroupXMLContent);
//    }

//    // test case: there is security group with unknown format in the opennebula response(xml).
//    @Test
//    public void testGetSecurityGroupBy() {
//        // setup
//        VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
//        String securityGroupXMLContent = "unknown";
//        Mockito.when(virtualNetwork.xpath(Mockito.eq(OpenNebulaSecurityRulePlugin.SECURITY_GROUPS_PATH)))
//                .thenReturn(securityGroupXMLContent);
//
//        // exercise
//        String securityGroupContent = this.plugin.getSecurityGroupBy(virtualNetwork);
//
//        // verify
//        Assert.assertNull(securityGroupContent);
//    }

//	// test case: When calling the requestInstance method with a valid client, a
//	// virtual network instance will be loaded to obtain a security group through
//	// its ID, a new rule will be created and added to the template so that it can
//	// update the instance of the security group.
//	@Test
//	public void testRequestSecurityRuleSuccessful() throws FogbowException {
//		// set up
//		Client client = Mockito.mock(Client.class);
//		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
//		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
//				.willReturn(client);
//
//		VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
//		PowerMockito.mockStatic(VirtualNetwork.class);
//		BDDMockito.given(OpenNebulaClientUtil.getVirtualNetwork(Mockito.eq(client), Mockito.anyString()))
//				.willReturn(virtualNetwork);
//
//		String securityGroupContent = DEFAULT_SECURITY_GROUP_ID + SEPARATOR + FAKE_ID_VALUE;
//		Mockito.when(virtualNetwork.xpath(Mockito.eq(OpenNebulaSecurityRulePlugin.SECURITY_GROUPS_PATH)))
//				.thenReturn(securityGroupContent);
//
//		String securityGroupId = FAKE_ID_VALUE;
//		SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);
//		BDDMockito.given(OpenNebulaClientUtil.getSecurityGroup(Mockito.eq(client), Mockito.eq(securityGroupId)))
//				.willReturn(securityGroup);
//
//		String xml = getSecurityGroupInfo();
//
//		OneResponse sgiResponse = Mockito.mock(OneResponse.class);
//		Mockito.when(sgiResponse.getMessage()).thenReturn(xml);
//		Mockito.when(securityGroup.info()).thenReturn(sgiResponse);
//
//		SecurityGroupInfo securityGroupInfo = SecurityGroupInfo.unmarshal(xml);
//		PowerMockito.mockStatic(SecurityGroupInfo.class);
//		BDDMockito.given(SecurityGroupInfo.unmarshal(Mockito.eq(xml))).willReturn(securityGroupInfo);
//
//		String template = generateSecurityGroupTemplate();
//		OneResponse sgtResponse = Mockito.mock(OneResponse.class);
//		Mockito.when(securityGroup.update(Mockito.eq(template))).thenReturn(sgtResponse);
//		Mockito.when(sgtResponse.isError()).thenReturn(false);
//
//		CloudUser cloudUser = createCloudUser();
//		Order majorOrder = new NetworkOrder();
//		String instanceId = FAKE_INSTANCE_ID;
//		majorOrder.setInstanceId(instanceId);
//		SecurityRule securityRule = createSecurityRule();
//
//		// exercise
//		this.plugin.requestSecurityRule(securityRule, majorOrder, cloudUser);
//
//		// verify
//		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
//		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());
//
//		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
//		OpenNebulaClientUtil.getVirtualNetwork(Mockito.eq(client), Mockito.anyString());
//
//		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
//		OpenNebulaClientUtil.getSecurityGroup(Mockito.eq(client), Mockito.anyString());
//
//		Mockito.verify(virtualNetwork, Mockito.times(1))
//				.xpath(Mockito.eq(OpenNebulaSecurityRulePlugin.SECURITY_GROUPS_PATH));
//
//		Mockito.verify(sgiResponse, Mockito.times(1)).getMessage();
//		Mockito.verify(securityGroup, Mockito.times(1)).info();
//		Mockito.verify(securityGroup, Mockito.times(1)).update(Mockito.eq(template));
//		Mockito.verify(sgtResponse, Mockito.times(1)).isError();
//
//		PowerMockito.verifyStatic(SecurityGroupInfo.class, VerificationModeFactory.times(1));
//		SecurityGroupInfo.unmarshal(Mockito.eq(xml));
//	}

//	// test case: When calling the requestInstance method with a valid client, and a malformed
//	// security rule template, its must be throw an InvalidParameterException.
//	@Test(expected = InvalidParameterException.class) // verify
//	public void testRequestSecurityRuleThrowInvalidParameterException() throws FogbowException {
//		// set up
//		Client client = Mockito.mock(Client.class);
//		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
//		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
//				.willReturn(client);
//
//		VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
//		PowerMockito.mockStatic(VirtualNetwork.class);
//		BDDMockito.given(OpenNebulaClientUtil.getVirtualNetwork(Mockito.eq(client), Mockito.anyString()))
//				.willReturn(virtualNetwork);
//
//		String securityGroupContent = DEFAULT_SECURITY_GROUP_ID + SEPARATOR + FAKE_ID_VALUE;
//		Mockito.when(virtualNetwork.xpath(Mockito.eq(OpenNebulaSecurityRulePlugin.SECURITY_GROUPS_PATH)))
//				.thenReturn(securityGroupContent);
//
//		String securityGroupId = FAKE_ID_VALUE;
//		SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);
//		BDDMockito.given(OpenNebulaClientUtil.getSecurityGroup(Mockito.eq(client), Mockito.eq(securityGroupId)))
//				.willReturn(securityGroup);
//
//		String xml = getSecurityGroupInfo();
//
//		OneResponse sgiResponse = Mockito.mock(OneResponse.class);
//		Mockito.when(sgiResponse.getMessage()).thenReturn(xml);
//		Mockito.when(securityGroup.info()).thenReturn(sgiResponse);
//
//		SecurityGroupInfo securityGroupInfo = SecurityGroupInfo.unmarshal(xml);
//		PowerMockito.mockStatic(SecurityGroupInfo.class);
//		BDDMockito.given(SecurityGroupInfo.unmarshal(Mockito.eq(xml))).willReturn(securityGroupInfo);
//
//		String template = generateSecurityGroupTemplate();
//		OneResponse sgtResponse = Mockito.mock(OneResponse.class);
//		Mockito.when(securityGroup.update(Mockito.eq(template))).thenReturn(sgtResponse);
//		Mockito.when(sgtResponse.isError()).thenReturn(true);
//
//		CloudUser cloudUser = createCloudUser();
//		Order majorOrder = new NetworkOrder();
//		String instanceId = FAKE_INSTANCE_ID;
//		majorOrder.setInstanceId(instanceId);
//		SecurityRule securityRule = createSecurityRule();
//
//		// exercise
//		this.plugin.requestSecurityRule(securityRule, majorOrder, cloudUser);
//	}

//    private String generateSecurityGroupTemplate() {
//        String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" 
//                + "<TEMPLATE>\n"
//                + "    <ID>100</ID>\n" 
//                + "    <NAME>TestSecurityRule</NAME>\n" 
//                + "    <RULE>\n"
//                + "        <IP>10.10.10.0</IP>\n" 
//                + "        <PROTOCOL>TCP</PROTOCOL>\n"
//                + "        <RANGE>1:65536</RANGE>\n" 
//                + "        <SIZE>256</SIZE>\n"
//                + "        <RULE_TYPE>inbound</RULE_TYPE>\n" 
//                + "    </RULE>\n" 
//                + "    <RULE>\n"
//                + "        <NETWORK_ID>4</NETWORK_ID>\n" 
//                + "        <PROTOCOL>TCP</PROTOCOL>\n"
//                + "        <RULE_TYPE>inbound</RULE_TYPE>\n" 
//                + "    </RULE>\n" 
//                + "    <RULE>\n"
//                + "        <IP>10.10.10.0</IP>\n" 
//                + "        <PROTOCOL>TCP</PROTOCOL>\n"
//                + "        <RANGE>1:65536</RANGE>\n" 
//                + "        <SIZE>256</SIZE>\n"
//                + "        <RULE_TYPE>inbound</RULE_TYPE>\n" 
//                + "    </RULE>\n" 
//                + "</TEMPLATE>\n";
//
//        return template;
//    }

//    private String getSecurityGroupInfo() {
//        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" 
//                + "<SECURITY_GROUP>\n" 
//                + "   <ID>100</ID>\n"
//                + "   <UID>0</UID>\n" 
//                + "   <GID>0</GID>\n" 
//                + "   <UNAME>oneadmin</UNAME>\n"
//                + "   <GNAME>oneadmin</GNAME>\n" 
//                + "   <NAME>TestSecurityRule</NAME>\n" 
//                + "   <PERMISSIONS>\n"
//                + "      <OWNER_U>1</OWNER_U>\n" 
//                + "      <OWNER_M>1</OWNER_M>\n" 
//                + "      <OWNER_A>0</OWNER_A>\n"
//                + "      <GROUP_U>0</GROUP_U>\n" 
//                + "      <GROUP_M>0</GROUP_M>\n" 
//                + "      <GROUP_A>0</GROUP_A>\n"
//                + "      <OTHER_U>0</OTHER_U>\n" 
//                + "      <OTHER_M>0</OTHER_M>\n" 
//                + "      <OTHER_A>0</OTHER_A>\n"
//                + "   </PERMISSIONS>\n" 
//                + "   <UPDATED_VMS />\n" 
//                + "   <OUTDATED_VMS />\n" 
//                + "   <UPDATING_VMS />\n"
//                + "   <ERROR_VMS />\n" 
//                + "   <TEMPLATE>\n" 
//                + "      <DESCRIPTION />\n" 
//                + "      <RULE>\n"
//                + "         <IP><![CDATA[10.10.10.0]]></IP>\n" 
//                + "         <PROTOCOL><![CDATA[TCP]]></PROTOCOL>\n"
//                + "         <RANGE><![CDATA[1:65536]]></RANGE>\n"
//                + "         <RULE_TYPE><![CDATA[inbound]]></RULE_TYPE>\n" 
//                + "         <SIZE><![CDATA[256]]></SIZE>\n"
//                + "      </RULE>\n" 
//                + "      <RULE>\n" 
//                + "         <NETWORK_ID><![CDATA[4]]></NETWORK_ID>\n"
//                + "         <PROTOCOL><![CDATA[TCP]]></PROTOCOL>\n"
//                + "         <RULE_TYPE><![CDATA[inbound]]></RULE_TYPE>\n" 
//                + "      </RULE>\n" 
//                + "   </TEMPLATE>\n"
//                + "</SECURITY_GROUP>";
//
//        return xml;
//    }

//	private SecurityGroupTemplate createSecurityGroupTemplate(String id, String name, List<Rule> rules) {
//        SecurityGroupTemplate securityGroupTemplate = new SecurityGroupTemplate();
//        securityGroupTemplate.setId(id);
//        securityGroupTemplate.setName(name);
//        securityGroupTemplate.setRules(rules);
//        return  securityGroupTemplate;
//    }

//    private Rule createSecurityRuleId(String ip, String securityGruopId) {
//        Rule rule = new Rule();
//        rule.setIp(ip);
//        rule.setSecurityGroupId(securityGruopId);
//        rule.setType(null);
//        rule.setRange(null);
//        rule.setNetworkId(null);
//        rule.setProtocol(null);
//        rule.setSize(null);
//        return rule;
//    }

//    private CloudUser createCloudUser() {
//        String userId = FAKE_USER_ID;
//        String userName = FAKE_USER_NAME;
//        String tokenValue = FAKE_TOKEN_VALUE;
//
//        return new CloudUser(userId, userName, tokenValue);
//    }
    
}
