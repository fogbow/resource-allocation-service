package cloud.fogbow.ras.core.plugins.interoperability.opennebula.securityrule.v5_4;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.models.FederationUser;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.api.http.response.securityrules.Direction;
import cloud.fogbow.ras.api.http.response.securityrules.EtherType;
import cloud.fogbow.ras.api.http.response.securityrules.Protocol;
import cloud.fogbow.ras.api.http.response.securityrules.SecurityRule;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.secgroup.SecurityGroup;
import org.opennebula.client.vnet.VirtualNetwork;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SecurityGroupInfo.class, Integer.class})
public class OpenNebulaSecurityRulePluginTest {

    private static final String CLOUD_NAME = "opennebula";
    private OpenNebulaSecurityRulePlugin openNebulaSecurityRulePlugin;
    private OpenNebulaClientFactory openNebulaClientFactory;

    @Before
    public void setUp() {
        String openenbulaConfFilePath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME +
                File.separator + CLOUD_NAME + File.separator + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
        this.openNebulaSecurityRulePlugin = Mockito.spy(new OpenNebulaSecurityRulePlugin(openenbulaConfFilePath));

        this.openNebulaClientFactory = Mockito.mock(OpenNebulaClientFactory.class);
        this.openNebulaSecurityRulePlugin.setFactory(this.openNebulaClientFactory);
    }

    // test case: success case
    @Test
    public void testDeleteSecurityRule() throws FogbowException {
        // setup
        FederationUser federationUser = new FederationUser("provider", "userId", "userName", "tokenValue", new HashMap<>());
        CloudToken localUserAttributes = new CloudToken(federationUser);
        String securityGroupId = "0";
        String securityGroupName = "securityGroupName";
        String ipOne = "10.10.0.0";
        String ipTwo = "20.20.0.0";
        String ipTree = "30.30.0.0";

        List<Rule> rules = new ArrayList<>();
        Rule ruleOneToRemove = createSecurityRuleId(ipOne, securityGroupId);
        rules.add(ruleOneToRemove);
        rules.add(createSecurityRuleId(ipTwo, securityGroupId));
        rules.add(createSecurityRuleId(ipTree, securityGroupId));

        List<Rule> rulesExpected = new ArrayList<>(rules);
        rulesExpected.remove(ruleOneToRemove);
        SecurityGroupTemplate securityGroupTemplate = createSecurityGroupTemplate(securityGroupId, securityGroupName, rulesExpected);
        String securityGroupTemplateXml = securityGroupTemplate.marshalTemplate();

        String securityRuleId = ruleOneToRemove.serialize();

        SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);
        OneResponse oneResponseExpected = Mockito.mock(OneResponse.class);
        Mockito.doReturn(false).when(oneResponseExpected).isError();
        Mockito.doReturn(oneResponseExpected).when(securityGroup).update(Mockito.any());

        Mockito.when(securityGroup.getId()).thenReturn(securityGroupId);
        Mockito.when(this.openNebulaClientFactory.createSecurityGroup(Mockito.any(), Mockito.eq(securityGroupId)))
                .thenReturn(securityGroup);

        SecurityGroupInfo securityGroupInfo = Mockito.mock(SecurityGroupInfo.class);
        Mockito.when(securityGroupInfo.getId()).thenReturn(securityGroupId);
        Mockito.when(securityGroupInfo.getName()).thenReturn(securityGroupName);

        Mockito.doReturn(securityGroupInfo).when(this.openNebulaSecurityRulePlugin).getSecurityGroupInfo(Mockito.eq(securityGroup));

        Mockito.doReturn(rules).when(this.openNebulaSecurityRulePlugin).getRules(Mockito.eq(securityGroupInfo));

        // exercise
        this.openNebulaSecurityRulePlugin.deleteSecurityRule(securityRuleId, localUserAttributes);

        // verify
        Mockito.verify(securityGroup, Mockito.times(1)).update(Mockito.eq(securityGroupTemplateXml));
    }

    // test case: Occur an error when updating the security group in the cloud
    @Test(expected = FogbowException.class)
    public void testDeleteSecurityRuleErrorWhileUpdatingSecurity() throws FogbowException {
        // setup
        CloudToken localUserAttributes = new CloudToken(new FederationUser("provider", "userId", "userName", "tokenValue", new HashMap<>()));
        String securityGroupId = "0";
        String securityGroupName = "securityGroupName";
        String ipOne = "10.10.0.0";

        List<Rule> rules = new ArrayList<>();
        Rule ruleOneToRemove = createSecurityRuleId(ipOne, securityGroupId);
        rules.add(ruleOneToRemove);

        List<Rule> rulesExpected = new ArrayList<>(rules);
        rulesExpected.remove(ruleOneToRemove);
        SecurityGroupTemplate securityGroupTemplate = createSecurityGroupTemplate(securityGroupId, securityGroupName, rulesExpected);
        String securityGroupTemplateXml = securityGroupTemplate.marshalTemplate();

        String securityRuleId = ruleOneToRemove.serialize();

        SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);
        OneResponse oneResponseExpected = Mockito.mock(OneResponse.class);
        boolean isError = true;
        Mockito.doReturn(isError).when(oneResponseExpected).isError();
        Mockito.doReturn(oneResponseExpected).when(securityGroup).update(Mockito.any());

        Mockito.when(securityGroup.getId()).thenReturn(securityGroupId);
        Mockito.when(this.openNebulaClientFactory.createSecurityGroup(Mockito.any(), Mockito.eq(securityGroupId)))
                .thenReturn(securityGroup);

        SecurityGroupInfo securityGroupInfo = Mockito.mock(SecurityGroupInfo.class);
        Mockito.when(securityGroupInfo.getId()).thenReturn(securityGroupId);
        Mockito.when(securityGroupInfo.getName()).thenReturn(securityGroupName);

        Mockito.doReturn(securityGroupInfo).when(this.openNebulaSecurityRulePlugin).getSecurityGroupInfo(Mockito.eq(securityGroup));

        Mockito.doReturn(rules).when(this.openNebulaSecurityRulePlugin).getRules(Mockito.eq(securityGroupInfo));

        // exercise
        this.openNebulaSecurityRulePlugin.deleteSecurityRule(securityRuleId, localUserAttributes);

        // verify
        Mockito.verify(securityGroup, Mockito.times(1)).update(Mockito.eq(securityGroupTemplateXml));
    }

    // test case: the security rule id came in a unknown format
    @Test(expected = FogbowException.class)
    public void testCreateRuleException() throws FogbowException {
        // setup
        String securityRuleId = "wrong";
        // exercise
        this.openNebulaSecurityRulePlugin.createRule(securityRuleId);
    }

    // test case: trying to remove a rule that there is not in the rules
    @Test
    public void testRemoveUnknownRule() {
        // setup
        List<Rule> rules = new ArrayList<>();
        Rule ruleToRemove = new Rule();
        ruleToRemove.setIp("10.10.10.10");
        rules.add(new Rule());
        rules.add(new Rule());
        rules.add(new Rule());
        int ruleSizeExpected = 3 ;

        // verify before
        Assert.assertEquals(ruleSizeExpected, rules.size());

        // exercise
        this.openNebulaSecurityRulePlugin.removeRule(ruleToRemove, rules);

        // verify after
        Assert.assertEquals(ruleSizeExpected, rules.size());
    }

    // test case: the rules is null and this will not throw exception
    @Test
    public void testRemoveRuleNullRules() {
        // exercise and verify
        try {
            this.openNebulaSecurityRulePlugin.removeRule(null, null);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    // test case: throw exception when security group is null
    @Test(expected = FogbowException.class)
    public void testCreateSecurityGroupTemplate() throws FogbowException {
        // set up
        List<Rule> rules = new ArrayList<>();
        SecurityGroupInfo securityGroupInfo = null;
        // exercise
        this.openNebulaSecurityRulePlugin.createSecurityGroupTemplate(securityGroupInfo, rules);
    }

    private SecurityGroupTemplate createSecurityGroupTemplate(String id, String name, List<Rule> rules) {
        SecurityGroupTemplate securityGroupTemplate = new SecurityGroupTemplate();
        securityGroupTemplate.setId(id);
        securityGroupTemplate.setName(name);
        securityGroupTemplate.setRules(rules);
        return  securityGroupTemplate;
    }

    private Rule createSecurityRuleId(String ip, String securityGruopId) {
        Rule rule = new Rule();
        rule.setIp(ip);
        rule.setSecurityGroupId(securityGruopId);
        rule.setType(null);
        rule.setRange(null);
        rule.setNetworkId(null);
        rule.setProtocol(null);
        rule.setSize(null);
        return rule;
    }

    // test case: success case
    @Test
    public void testGetSecurityRules() throws FogbowException {
        // setup
        Order majorOrder = new NetworkOrder();
        String networkId = "instanceId";
        majorOrder.setInstanceId(networkId);
        CloudToken localUserAttributes = new CloudToken(new FederationUser("provider", "userId", "userName", "tokenValue", new HashMap<>()));

        VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
        // created by opennebula deploy
        String defaultIpOpennebula = "0";
        // created by Fogbow (RAS)
        String securityGroupId = "10";
        String securityGroupXMLContent = String.format("%s%s%s", defaultIpOpennebula, OpenNebulaSecurityRulePlugin.OPENNEBULA_XML_ARRAY_SEPARATOR,securityGroupId);
        Mockito.when(virtualNetwork.xpath(Mockito.eq(OpenNebulaSecurityRulePlugin.TEMPLATE_VNET_SECURITY_GROUPS_PATH)))
                .thenReturn(securityGroupXMLContent);
        Mockito.when(this.openNebulaClientFactory.createVirtualNetwork(Mockito.any(), Mockito.eq(networkId)))
                .thenReturn(virtualNetwork);

        SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);

        Mockito.when(securityGroup.getId()).thenReturn(securityGroupId);
        Mockito.when(this.openNebulaClientFactory.createSecurityGroup(Mockito.any(), Mockito.eq(securityGroupId)))
                .thenReturn(securityGroup);

        List<Rule> rules = new ArrayList<Rule>();
        int portFrom = 22;
        int portTo = 3000;
        String ip = "10.10.0.1"; // ipv4
        String sizeRangeIp = "256"; // subnet is 24
        
        Rule rule = new Rule(Rule.TCP_XML_TEMPLATE_VALUE, 
        		ip, 
        		sizeRangeIp, 
        		String.format("%s%s%s", portFrom, Rule.OPENNEBULA_RANGE_SEPARATOR, portTo), 
        		Rule.INBOUND_XML_TEMPLATE_VALUE, 
        		null, 
        		securityGroupId);
        
        rules.add(rule);

        SecurityGroupInfo securityGroupInfo = Mockito.mock(SecurityGroupInfo.class);
        Mockito.doReturn(securityGroupInfo).when(this.openNebulaSecurityRulePlugin).getSecurityGroupInfo(Mockito.eq(securityGroup));
        Mockito.doReturn(rules).when(this.openNebulaSecurityRulePlugin).getRules(Mockito.eq(securityGroupInfo));

        // exercise
        List<SecurityRule> securityRules = this.openNebulaSecurityRulePlugin.getSecurityRules(majorOrder, localUserAttributes);

        // verify
        SecurityRule securityRule = securityRules.iterator().next();
        Assert.assertEquals(Protocol.TCP, securityRule.getProtocol());
        Assert.assertEquals(portFrom, securityRule.getPortFrom());
        Assert.assertEquals(portTo, securityRule.getPortTo());
        Assert.assertEquals(String.format("%s%s%s", ip, Rule.CIRD_SEPARATOR, 24) , securityRule.getCidr());
        Assert.assertEquals(Direction.IN, securityRule.getDirection());
        Assert.assertEquals(EtherType.IPv4, securityRule.getEtherType());
        Assert.assertEquals(rule.serialize(), securityRule.getInstanceId());
    }

    // test case: error while trying to get rules
    @Test(expected = FogbowException.class)
    public void testGetSecurityRulesErrorWhileGetRules() throws FogbowException {
        // setup
        SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);
        SecurityGroupInfo securityGroupInfo = Mockito.mock(SecurityGroupInfo.class);
        Mockito.doReturn(securityGroupInfo).when(this.openNebulaSecurityRulePlugin).getSecurityGroupInfo(Mockito.eq(securityGroup));
        Mockito.doThrow(Exception.class).when(this.openNebulaSecurityRulePlugin).getRules(Mockito.eq(securityGroupInfo));

        // exercise
        this.openNebulaSecurityRulePlugin.getSecurityRules(securityGroup);
    }

    // test case: there is not security group in the opennebula response(xml)
    @Test
    public void testGetSecurityGroupByEmptyContent() {
        // setup
        VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
        Mockito.when(virtualNetwork.xpath(Mockito.eq(OpenNebulaSecurityRulePlugin.TEMPLATE_VNET_SECURITY_GROUPS_PATH)))
                .thenReturn(null);

        // exercise
        String securityGroupXMLContent = this.openNebulaSecurityRulePlugin.getSecurityGroupBy(virtualNetwork);

        // verify
        Assert.assertNull(securityGroupXMLContent);
    }

    // test case: there is security group with unknown format in the opennebula response(xml)
    @Test
    public void testGetSecurityGroupBy() {
        // setup
        VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
        String securityGroupXMLContent = "unknown";
        Mockito.when(virtualNetwork.xpath(Mockito.eq(OpenNebulaSecurityRulePlugin.TEMPLATE_VNET_SECURITY_GROUPS_PATH)))
                .thenReturn(securityGroupXMLContent);

        // exercise
        String securityGroupContent = this.openNebulaSecurityRulePlugin.getSecurityGroupBy(virtualNetwork);

        // verify
        Assert.assertNull(securityGroupContent);
    }
    
	// test case: When calling the requestInstance method with a valid client, a
	// virtual network instance will be loaded to obtain a security group through
	// its ID, a new rule will be created and added to the template so that it can
	// update the instance of the security group.
	@Test
	public void testRequestSecurityRuleSuccessful() throws FogbowException {
		// set up
		CloudToken token = createCloudToken();
		Client client = this.openNebulaClientFactory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.openNebulaClientFactory).createClient(token.getTokenValue());
		this.openNebulaSecurityRulePlugin.setFactory(this.openNebulaClientFactory);

		Order majorOrder = new NetworkOrder();
		String instanceId = "fake-instance-id";
		majorOrder.setInstanceId(instanceId);

		SecurityRule securityRule = createSecurityRule();

		VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
		Mockito.when(this.openNebulaClientFactory.createVirtualNetwork(Mockito.eq(client), Mockito.eq(instanceId)))
				.thenReturn(virtualNetwork);

		String securityGroupContent = "0,100";
		Mockito.when(virtualNetwork.xpath(Mockito.eq(OpenNebulaSecurityRulePlugin.TEMPLATE_VNET_SECURITY_GROUPS_PATH)))
				.thenReturn(securityGroupContent);

		String securityGroupId = "100";
		SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);
		Mockito.when(securityGroup.getId()).thenReturn(securityGroupId);
		Mockito.when(this.openNebulaClientFactory.createSecurityGroup(Mockito.eq(client), Mockito.eq(securityGroupId)))
				.thenReturn(securityGroup);

		String xml = getSecurityGroupInfo();

		OneResponse sgiResponse = Mockito.mock(OneResponse.class);
		Mockito.when(sgiResponse.getMessage()).thenReturn(xml);
		Mockito.when(securityGroup.info()).thenReturn(sgiResponse);

		SecurityGroupInfo securityGroupInfo = SecurityGroupInfo.unmarshal(xml);
		PowerMockito.mockStatic(SecurityGroupInfo.class);
		BDDMockito.given(SecurityGroupInfo.unmarshal(Mockito.eq(xml))).willReturn(securityGroupInfo);

		String template = generateSecurityGroupTemplate();
		OneResponse sgtResponse = Mockito.mock(OneResponse.class);
		Mockito.when(securityGroup.update(Mockito.eq(template))).thenReturn(sgtResponse);
		Mockito.when(sgtResponse.isError()).thenReturn(false);

		// exercise
		this.openNebulaSecurityRulePlugin.requestSecurityRule(securityRule, majorOrder, token);

		// verify
		Mockito.verify(this.openNebulaClientFactory, Mockito.times(2)).createClient(Mockito.eq(token.getTokenValue()));
		Mockito.verify(this.openNebulaClientFactory, Mockito.times(1)).createVirtualNetwork(Mockito.eq(client),
				Mockito.eq(instanceId));
		Mockito.verify(this.openNebulaClientFactory, Mockito.times(1)).createSecurityGroup(Mockito.eq(client),
				Mockito.eq(securityGroupId));
		Mockito.verify(virtualNetwork, Mockito.times(1))
				.xpath(Mockito.eq(OpenNebulaSecurityRulePlugin.TEMPLATE_VNET_SECURITY_GROUPS_PATH));
		Mockito.verify(sgiResponse, Mockito.times(1)).getMessage();
		Mockito.verify(securityGroup, Mockito.times(1)).info();
		Mockito.verify(securityGroup, Mockito.times(1)).update(Mockito.eq(template));
		Mockito.verify(sgtResponse, Mockito.times(1)).isError();
		PowerMockito.verifyStatic(SecurityGroupInfo.class, VerificationModeFactory.times(1));
		SecurityGroupInfo.unmarshal(Mockito.eq(xml));
	}
    
	// test case: When calling the requestInstance method with a valid client, and a malformed
	// security rule template, its must be throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class) // verify
	public void testRequestSecurityRuleThrowInvalidParameterException() throws FogbowException {
		// set up
		CloudToken token = createCloudToken();
		Client client = this.openNebulaClientFactory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.openNebulaClientFactory).createClient(token.getTokenValue());
		this.openNebulaSecurityRulePlugin.setFactory(this.openNebulaClientFactory);

		Order majorOrder = new NetworkOrder();
		String instanceId = "fake-instance-id";
		majorOrder.setInstanceId(instanceId);

		SecurityRule securityRule = createSecurityRule();

		VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
		Mockito.when(this.openNebulaClientFactory.createVirtualNetwork(Mockito.eq(client), Mockito.eq(instanceId)))
				.thenReturn(virtualNetwork);

		String securityGroupContent = "0,100";
		Mockito.when(virtualNetwork.xpath(Mockito.eq(OpenNebulaSecurityRulePlugin.TEMPLATE_VNET_SECURITY_GROUPS_PATH)))
				.thenReturn(securityGroupContent);

		String securityGroupId = "100";
		SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);
		Mockito.when(securityGroup.getId()).thenReturn(securityGroupId);
		Mockito.when(this.openNebulaClientFactory.createSecurityGroup(Mockito.eq(client), Mockito.eq(securityGroupId)))
				.thenReturn(securityGroup);

		String xml = getSecurityGroupInfo();

		OneResponse sgiResponse = Mockito.mock(OneResponse.class);
		Mockito.when(sgiResponse.getMessage()).thenReturn(xml);
		Mockito.when(securityGroup.info()).thenReturn(sgiResponse);

		SecurityGroupInfo securityGroupInfo = SecurityGroupInfo.unmarshal(xml);
		PowerMockito.mockStatic(SecurityGroupInfo.class);
		BDDMockito.given(SecurityGroupInfo.unmarshal(Mockito.eq(xml))).willReturn(securityGroupInfo);

		String template = generateSecurityGroupTemplate();
		OneResponse sgtResponse = Mockito.mock(OneResponse.class);
		Mockito.when(securityGroup.update(Mockito.eq(template))).thenReturn(sgtResponse);
		Mockito.when(sgtResponse.isError()).thenReturn(true);

		// exercise
		this.openNebulaSecurityRulePlugin.requestSecurityRule(securityRule, majorOrder, token);
	}
    
    private String generateSecurityGroupTemplate() {
    	String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + 
    			"<TEMPLATE>\n" + 
    			"    <ID>100</ID>\n" + 
    			"    <NAME>TestSecurityRule</NAME>\n" + 
    			"    <RULE>\n" + 
    			"        <IP>10.10.10.0</IP>\n" + 
    			"        <PROTOCOL>TCP</PROTOCOL>\n" + 
    			"        <RANGE>1:65536</RANGE>\n" + 
    			"        <SIZE>256</SIZE>\n" + 
    			"        <RULE_TYPE>inbound</RULE_TYPE>\n" + 
    			"    </RULE>\n" + 
    			"    <RULE>\n" + 
    			"        <NETWORK_ID>4</NETWORK_ID>\n" + 
    			"        <PROTOCOL>TCP</PROTOCOL>\n" + 
    			"        <RULE_TYPE>inbound</RULE_TYPE>\n" + 
    			"    </RULE>\n" + 
    			"    <RULE>\n" + 
    			"        <IP>10.10.10.0</IP>\n" + 
    			"        <PROTOCOL>TCP</PROTOCOL>\n" + 
    			"        <RANGE>1:65536</RANGE>\n" + 
    			"        <SIZE>256</SIZE>\n" + 
    			"        <RULE_TYPE>inbound</RULE_TYPE>\n" + 
    			"    </RULE>\n" + 
    			"</TEMPLATE>\n";
    	
    	return template;
    }

	private SecurityRule createSecurityRule() {
		SecurityRule securityRule = new SecurityRule();
		securityRule.setCidr("10.10.10.0/24");
		securityRule.setDirection(Direction.IN);
		securityRule.setEtherType(EtherType.IPv4);
		securityRule.setInstanceId("fake-instance-id");
		securityRule.setPortFrom(1);
		securityRule.setPortTo(65536);
		securityRule.setProtocol(Protocol.TCP);
		return securityRule;
	}

	private String getSecurityGroupInfo() {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
				"<SECURITY_GROUP>\n" + 
				"   <ID>100</ID>\n" + 
				"   <UID>0</UID>\n" + 
				"   <GID>0</GID>\n" + 
				"   <UNAME>oneadmin</UNAME>\n" + 
				"   <GNAME>oneadmin</GNAME>\n" + 
				"   <NAME>TestSecurityRule</NAME>\n" + 
				"   <PERMISSIONS>\n" + 
				"      <OWNER_U>1</OWNER_U>\n" + 
				"      <OWNER_M>1</OWNER_M>\n" + 
				"      <OWNER_A>0</OWNER_A>\n" + 
				"      <GROUP_U>0</GROUP_U>\n" + 
				"      <GROUP_M>0</GROUP_M>\n" + 
				"      <GROUP_A>0</GROUP_A>\n" + 
				"      <OTHER_U>0</OTHER_U>\n" + 
				"      <OTHER_M>0</OTHER_M>\n" + 
				"      <OTHER_A>0</OTHER_A>\n" + 
				"   </PERMISSIONS>\n" + 
				"   <UPDATED_VMS />\n" + 
				"   <OUTDATED_VMS />\n" + 
				"   <UPDATING_VMS />\n" + 
				"   <ERROR_VMS />\n" + 
				"   <TEMPLATE>\n" + 
				"      <DESCRIPTION />\n" + 
				"      <RULE>\n" + 
				"         <IP><![CDATA[10.10.10.0]]></IP>\n" + 
				"         <PROTOCOL><![CDATA[TCP]]></PROTOCOL>\n" + 
				"         <RANGE><![CDATA[1:65536]]></RANGE>\n" + 
				"         <RULE_TYPE><![CDATA[inbound]]></RULE_TYPE>\n" + 
				"         <SIZE><![CDATA[256]]></SIZE>\n" + 
				"      </RULE>\n" + 
				"      <RULE>\n" + 
				"         <NETWORK_ID><![CDATA[4]]></NETWORK_ID>\n" + 
				"         <PROTOCOL><![CDATA[TCP]]></PROTOCOL>\n" + 
				"         <RULE_TYPE><![CDATA[inbound]]></RULE_TYPE>\n" + 
				"      </RULE>\n" + 
				"   </TEMPLATE>\n" + 
				"</SECURITY_GROUP>";
		
		return xml;
	}
    
    private CloudToken createCloudToken() {
		String provider = "fake-provider";
		String tokenValue = "fake-token-value";
		String userId = "fake-user-id";
		String userName = "fake-user-name";

		return new CloudToken(new FederationUser(provider, userId, userName, tokenValue, new HashMap<>()));
	}

}
