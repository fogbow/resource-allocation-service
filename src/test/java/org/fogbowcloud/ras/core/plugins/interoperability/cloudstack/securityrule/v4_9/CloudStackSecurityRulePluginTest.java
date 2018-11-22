package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.orders.ComputeOrder;
import org.fogbowcloud.ras.core.models.orders.NetworkOrder;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.orders.PublicIpOrder;
import org.fogbowcloud.ras.core.models.securityrules.Direction;
import org.fogbowcloud.ras.core.models.securityrules.EtherType;
import org.fogbowcloud.ras.core.models.securityrules.Protocol;
import org.fogbowcloud.ras.core.models.securityrules.SecurityRule;
import org.fogbowcloud.ras.core.models.tokens.CloudStackToken;
import org.fogbowcloud.ras.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.cloudstack.CloudStackTokenGeneratorPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.gson.Gson;

public class CloudStackSecurityRulePluginTest {

	private CloudStackSecurityRulePlugin cloudStackSecurityRulePlugin;
	private HttpRequestClientUtil httpClient;
	
	@Before
	public void setUp() {
		this.cloudStackSecurityRulePlugin = new CloudStackSecurityRulePlugin();
		this.httpClient = Mockito.mock(HttpRequestClientUtil.class);
		this.cloudStackSecurityRulePlugin.setClient(this.httpClient);
	}
	
	// test case: success case
	@Test
	public void testGetFirewallRules() throws FogbowRasException, UnexpectedException, HttpResponseException {
		// setup
		Order publicIpOrder = new PublicIpOrder();
		String instanceId = "instanceId";
		publicIpOrder.setInstanceId(instanceId);
		String tokenValue = "x" + CloudStackTokenGeneratorPlugin.CLOUDSTACK_TOKEN_VALUE_SEPARATOR + "y";
		CloudStackToken localUserAttributes = new CloudStackToken("", tokenValue, "", "", "");
		
		// create firewall rules response
		int portFrom = 20;
		int portTo = 30;
		String cidr = "0.0.0.0/0";
		EtherType etherType = EtherType.IPv4;
		Protocol protocol = Protocol.TCP;
		List<SecurityRule> securityRulesExpected = new ArrayList<SecurityRule>();
		securityRulesExpected.add(new SecurityRule(Direction.IN, portFrom, portTo, cidr, etherType, protocol));
		securityRulesExpected.add(new SecurityRule(Direction.IN, portFrom, portTo, cidr, etherType, protocol));
		String listFirewallRulesResponse = getlistFirewallRulesResponseJson(securityRulesExpected, etherType);
		
		Mockito.when(this.httpClient.doGetRequest(
				Mockito.anyString(), Mockito.any(CloudStackToken.class)))
				.thenReturn(listFirewallRulesResponse);
		
		// exercise
		List<SecurityRule> securityRules = this.cloudStackSecurityRulePlugin.getSecurityRules(publicIpOrder, localUserAttributes);		
		
		//verify
		Assert.assertEquals(securityRulesExpected.size(), securityRules.size());
		Assert.assertArrayEquals(securityRulesExpected.toArray(), securityRules.toArray());
	}
	
	// test case: throw exception where trying to request to the cloudstack
	@Test
	public void testGetFirewallRulesExceptionInComunication()
			throws FogbowRasException, UnexpectedException, HttpResponseException {
		// setup
		Order publicIpOrder = new PublicIpOrder();
		String tokenValue = "x" + CloudStackTokenGeneratorPlugin.CLOUDSTACK_TOKEN_VALUE_SEPARATOR + "y";
		CloudStackToken localUserAttributes = new CloudStackToken("", tokenValue, "", "", "");
		
		HttpResponseException badRequestException = new HttpResponseException(HttpStatus.SC_BAD_REQUEST, "");
		Mockito.doThrow(badRequestException).when(this.httpClient).doGetRequest(
				Mockito.anyString(), Mockito.any(OpenStackV3Token.class));
		
		// exercise
		List<SecurityRule> securityRules = null;
		try {
			this.cloudStackSecurityRulePlugin.getSecurityRules(publicIpOrder, localUserAttributes);		
			Assert.fail();
		} catch (InvalidParameterException e) {}		
		

		// verify
		Assert.assertNull(securityRules);
		Mockito.verify(this.httpClient, Mockito.times(1)).doGetRequest(
				Mockito.anyString(),
				Mockito.eq(localUserAttributes));		
	}	
	
	// test case: unsupported network order
	@Test(expected=UnsupportedOperationException.class)
	public void testGetFirewallRulesNetworkOrder() throws FogbowRasException, UnexpectedException {
		// setup
		Order networkOrder = new NetworkOrder();
		CloudStackToken localUserAttributes = new CloudStackToken();
		
		// exercise
		this.cloudStackSecurityRulePlugin.getSecurityRules(networkOrder, localUserAttributes);
	}

	// test case: throw exception when the order is different of the options: network, publicip
	@Test(expected=UnexpectedException.class)
	public void testGetFirewallRulesOrderIrregular() throws FogbowRasException, UnexpectedException {
		// setup
		Order irregularkOrder = new ComputeOrder();
		CloudStackToken localUserAttributes = new CloudStackToken();
		
		// exercise
		this.cloudStackSecurityRulePlugin.getSecurityRules(irregularkOrder, localUserAttributes);
	}	
			
	private String getlistFirewallRulesResponseJson(List<SecurityRule> securityRules, EtherType etherType) {
		List<Map<String, Object>> listFirewallRule = new ArrayList<Map<String, Object>>();
		for (SecurityRule securityRule : securityRules) {
			Map<String, Object> firewallRule = new HashMap<String, Object>();
			firewallRule.put(CloudStackRestApiConstants.SecurityGroupPlugin.CIDR_LIST_KEY_JSON,
					securityRule.getCidr());
			firewallRule.put(CloudStackRestApiConstants.SecurityGroupPlugin.ID_KEY_JSON,
					securityRule.getInstanceId());
			firewallRule.put(CloudStackRestApiConstants.SecurityGroupPlugin.START_PORT_KEY_JSON,
					securityRule.getPortFrom());
			firewallRule.put(CloudStackRestApiConstants.SecurityGroupPlugin.END_PORT_KEY_JSON,
					securityRule.getPortTo());			
			firewallRule.put(CloudStackRestApiConstants.SecurityGroupPlugin.PROPOCOL_KEY_JSON,
					securityRule.getProtocol().toString());
			if (etherType.equals(EtherType.IPv4)) {
				firewallRule.put(CloudStackRestApiConstants.SecurityGroupPlugin.IP_ADDRESS_KEY_JSON, "0.0.0.0");				
			} else {
				firewallRule.put(CloudStackRestApiConstants.SecurityGroupPlugin.IP_ADDRESS_KEY_JSON, 
							"FE80:0000:0000:0000:0202:B3FF:FE1E:8329");
			}
			
			listFirewallRule.add(firewallRule);
		}
		Map<String, List<Map<String, Object>>> firewallRules = new HashMap<String, List<Map<String, Object>>>();
		firewallRules.put(CloudStackRestApiConstants.SecurityGroupPlugin.FIREWALL_RULE_KEY_JSON, listFirewallRule);
		
        Map<String, Object> floatingipJsonKey = new HashMap<String, Object>();
        floatingipJsonKey.put(CloudStackRestApiConstants.SecurityGroupPlugin.LIST_FIREWALL_RULES_KEY_JSON, firewallRules);

        Gson gson = new Gson();
        return gson.toJson(floatingipJsonKey);
    }
	
}
