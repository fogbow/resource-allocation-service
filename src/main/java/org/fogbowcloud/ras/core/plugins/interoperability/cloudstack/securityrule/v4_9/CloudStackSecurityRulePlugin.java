package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpResponseException;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.securityrules.Direction;
import org.fogbowcloud.ras.core.models.securityrules.EtherType;
import org.fogbowcloud.ras.core.models.securityrules.Protocol;
import org.fogbowcloud.ras.core.models.securityrules.SecurityRule;
import org.fogbowcloud.ras.core.models.tokens.CloudStackToken;
import org.fogbowcloud.ras.core.plugins.interoperability.SecurityRulePlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackHttpToFogbowRasExceptionMapper;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9.ListFirewallRulesResponse.SecurityRuleResponse;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;

public class CloudStackSecurityRulePlugin implements SecurityRulePlugin<CloudStackToken> {
	
	private HttpRequestClientUtil client;

	public CloudStackSecurityRulePlugin() {
		this.client = new HttpRequestClientUtil();
	}
	
    @Override
    public String requestSecurityRule(SecurityRule securityRule, Order majorOrder, CloudStackToken localUserAttributes) throws FogbowRasException, UnexpectedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<SecurityRule> getSecurityRules(Order majorOrder, CloudStackToken localUserAttributes) throws FogbowRasException, UnexpectedException {
        switch (majorOrder.getType()) {
        	case PUBLIC_IP:
        		return getFirewallRules(majorOrder.getInstanceId(), localUserAttributes);
        	case NETWORK:
        		throw new UnsupportedOperationException();
        	default:
        		// TODO add message
        		throw new UnexpectedException("");
        }
    }
       
    protected List<SecurityRule> getFirewallRules(String ipAddressId, CloudStackToken localUserAttributes) throws FogbowRasException, UnexpectedException {
    	ListFirewallRulesRequest request = new ListFirewallRulesRequest.Builder()
    			.ipAddressId(ipAddressId)
    			.build();
    	CloudStackUrlUtil.sign(request.getUriBuilder(), localUserAttributes.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), localUserAttributes);
        } catch (HttpResponseException e) {
        	CloudStackHttpToFogbowRasExceptionMapper.map(e);
        }

        ListFirewallRulesResponse response = ListFirewallRulesResponse.fromJson(jsonResponse);
        List<SecurityRuleResponse> securityRulesResponse = response.getSecurityRulesResponse();                
        return convertToFogbowSecurityRules(securityRulesResponse);
    }    
    
    protected List<SecurityRule> convertToFogbowSecurityRules(List<SecurityRuleResponse> securityRulesResponse) throws UnexpectedException {
    	List<SecurityRule> securityRules = new ArrayList<SecurityRule>();
    	for (SecurityRuleResponse securityRuleResponse : securityRulesResponse) {
			Direction direction = securityRuleResponse.getDirection();
			int portFrom = securityRuleResponse.getPortFrom();
			int portTo = securityRuleResponse.getPortTo();
			String cidr = securityRuleResponse.getCidr();
			// TODO check this
			EtherType etherType = EtherType.IPv4;
			
			Protocol protocol = securityRuleResponse.getFogbowProtocol();
			securityRules.add(new SecurityRule(direction, portFrom, portTo, cidr, etherType, protocol));
		}
    	return null;
    }

    @Override
    public void deleteSecurityRule(String securityRuleId, CloudStackToken localUserAttributes) throws FogbowRasException, UnexpectedException {
        throw new UnsupportedOperationException();
    }
}
