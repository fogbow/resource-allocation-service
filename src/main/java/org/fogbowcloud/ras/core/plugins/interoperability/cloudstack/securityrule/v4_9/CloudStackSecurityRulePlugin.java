package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9;

import java.util.List;

import org.apache.http.client.HttpResponseException;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.securityrules.SecurityRule;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.SecurityRulePlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackHttpToFogbowRasExceptionMapper;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import org.fogbowcloud.ras.core.plugins.interoperability.openstack.network.v2.CreateSecurityGroupRuleRequest.SecurityGroupRule;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;


// TODO add the CloudstackToken in the interface generics:  SecurityGroupPlugin<CloudStackToken>
public class CloudStackSecurityRulePlugin implements SecurityRulePlugin {
	
	private HttpRequestClientUtil client;

	public CloudStackSecurityRulePlugin() {
		this.client = new HttpRequestClientUtil();
	}
	
    @Override
    public String requestSecurityRule(SecurityRule securityRule, Order majorOrder, Token localUserAttributes) throws FogbowRasException, UnexpectedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<SecurityGroupRule> getSecurityRules(Order majorOrder, Token localUserAttributes) throws FogbowRasException, UnexpectedException {
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
       
    public List<SecurityGroupRule> getFirewallRules(String ipAddressId, Token localUserAttributes) throws FogbowRasException {
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
//        List<GetAllImagesResponse.Image> images = response.getImages();

        return null;
    }

    @Override
    public void deleteSecurityRule(String securityRuleId, Token localUserAttributes) throws FogbowRasException, UnexpectedException {
        throw new UnsupportedOperationException();
    }
}
