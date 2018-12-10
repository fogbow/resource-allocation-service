package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.securityrule.v5_4;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.orders.NetworkOrder;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.securityrules.Direction;
import org.fogbowcloud.ras.core.models.securityrules.EtherType;
import org.fogbowcloud.ras.core.models.securityrules.Protocol;
import org.fogbowcloud.ras.core.models.securityrules.SecurityRule;
import org.fogbowcloud.ras.core.models.tokens.OpenNebulaToken;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opennebula.client.secgroup.SecurityGroup;
import org.opennebula.client.vnet.VirtualNetwork;

import java.util.ArrayList;
import java.util.List;

public class OpenNebulaSecurityRulePluginTest {

    private OpenNebulaSecurityRulePlugin openNebulaSecurityRulePlugin;
    private OpenNebulaClientFactory openNebulaClientFactory;

    private HttpRequestClientUtil client;

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
        int sizeRangeIp = 256; // subnet is 24
        rules.add(new Rule(Rule.TCP_XML_TEMPLATE_VALUE,
                ip,
                sizeRangeIp,
                String.format("%s%s%s", portFrom, Rule.OPENNEBULA_RANGE_SEPARATOR, portTo ),
                Rule.INBOUND_XML_TEMPLATE_VALUE,
                Integer.valueOf(securityGroupId)));
        Mockito.doReturn(rules).when(this.openNebulaSecurityRulePlugin).getRules(Mockito.eq(securityGroup));

        // exercise
        List<SecurityRule> securityRules = this.openNebulaSecurityRulePlugin.getSecurityRules(majorOrder, localUserAttributes);

        // verify
        SecurityRule securityRule = securityRules.iterator().next();
        Assert.assertEquals(Protocol.TCP, securityRule.getProtocol());
        Assert.assertEquals(portFrom, securityRule.getPortFrom());
        Assert.assertEquals(portTo, securityRule.getPortTo());
        Assert.assertEquals(String.format("%s%s%S", ip, Rule.CIRD_SEPARATOR, 24) , securityRule.getCidr());
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

}
