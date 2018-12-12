package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.securityrule.v5_4;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.utils.URIBuilder;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.orders.NetworkOrder;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.securityrules.Direction;
import org.fogbowcloud.ras.core.models.securityrules.EtherType;
import org.fogbowcloud.ras.core.models.securityrules.Protocol;
import org.fogbowcloud.ras.core.models.securityrules.SecurityRule;
import org.fogbowcloud.ras.core.models.tokens.OpenNebulaToken;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
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

@RunWith(PowerMockRunner.class)
@PrepareForTest({SecurityGroupInfo.class, Integer.class})
public class OpenNebulaSecurityRulePluginTest {

    private OpenNebulaSecurityRulePlugin openNebulaSecurityRulePlugin;
    private OpenNebulaClientFactory openNebulaClientFactory;

    @Before
    public void setUp() {
        this.openNebulaSecurityRulePlugin = Mockito.spy(new OpenNebulaSecurityRulePlugin());

        this.openNebulaClientFactory = Mockito.mock(OpenNebulaClientFactory.class);
        this.openNebulaSecurityRulePlugin.setFactory(this.openNebulaClientFactory);
    }

    // test case: success case
    @Test
    public void testGetSecurityRules() throws UnexpectedException, FogbowRasException {
        // setup
        Order majorOrder = new NetworkOrder();
        String instanceId = "instanceId";
        majorOrder.setInstanceId(instanceId);
        OpenNebulaToken localUserAttributes = new OpenNebulaToken("provider", "tokenValue", "userId", "userName", "signature");

        VirtualNetwork virtualNetwork = Mockito.mock(VirtualNetwork.class);
        // created by opennebula deploy
        String defaultIpOpennebula = "0";
        // created by Fogbow (RAS)
        String securityGroupId = "10";
        String securityGroupXMLContent = String.format("%s%s%s", defaultIpOpennebula, OpenNebulaSecurityRulePlugin.OPENNEBULA_XML_ARRAY_SEPARATOR,securityGroupId);
        Mockito.when(virtualNetwork.xpath(Mockito.eq(OpenNebulaSecurityRulePlugin.TEMPLATE_VNET_SECURITY_GROUPS_PATH)))
                .thenReturn(securityGroupXMLContent);
        Mockito.when(this.openNebulaClientFactory.createVirtualNetwork(Mockito.any(), Mockito.eq(instanceId)))
                .thenReturn(virtualNetwork);

        SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);

        Mockito.when(securityGroup.getId()).thenReturn(securityGroupId);
        Mockito.when(this.openNebulaClientFactory.getSecurityGroup(Mockito.any(), Mockito.eq(securityGroupId)))
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
        
        Mockito.doReturn(rules).when(this.openNebulaSecurityRulePlugin).getRules(Mockito.eq(securityGroup));

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
    }

    // test case: error while trying to get rules
    @Test(expected = FogbowRasException.class)
    public void testGetSecurityRulesErrorWhileGetRules() throws UnexpectedException, FogbowRasException {
        // setup
        SecurityGroup securityGroup = Mockito.mock(SecurityGroup.class);
        Mockito.doThrow(Exception.class).when(this.openNebulaSecurityRulePlugin).getRules(Mockito.eq(securityGroup));

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
    
    // test case: Success case...
    @Test
    public void testRequestSecurityRule() throws FogbowRasException, UnexpectedException {
    	// set up
    	OpenNebulaToken token = createOpenNebulaToken();
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
		Mockito.when(this.openNebulaClientFactory.getSecurityGroup(Mockito.eq(client), Mockito.eq(securityGroupId)))
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
        Mockito.verify(this.openNebulaClientFactory, Mockito.times(1)).createVirtualNetwork(Mockito.eq(client), Mockito.eq(instanceId));
        Mockito.verify(this.openNebulaClientFactory, Mockito.times(1)).getSecurityGroup(Mockito.eq(client), Mockito.eq(securityGroupId));
        Mockito.verify(virtualNetwork, Mockito.times(1)).xpath(Mockito.eq(OpenNebulaSecurityRulePlugin.TEMPLATE_VNET_SECURITY_GROUPS_PATH));
        Mockito.verify(sgiResponse, Mockito.times(1)).getMessage();
        Mockito.verify(securityGroup, Mockito.times(1)).info();
        Mockito.verify(securityGroup, Mockito.times(1)).update(Mockito.eq(template));
        Mockito.verify(sgtResponse, Mockito.times(1)).isError();
        PowerMockito.verifyStatic(SecurityGroupInfo.class, VerificationModeFactory.times(1));
        SecurityGroupInfo.unmarshal(Mockito.eq(xml));
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
    
    private OpenNebulaToken createOpenNebulaToken() {
		String provider = "fake-provider";
		String tokenValue = "fake-token-value";
		String userId = "fake-user-id";
		String userName = "fake-user-name";
		String signature = "fake-signature";
		
		OpenNebulaToken token = new OpenNebulaToken(
				provider, 
				tokenValue, 
				userId, 
				userName, 
				signature);
		
		return token;
	}

}
