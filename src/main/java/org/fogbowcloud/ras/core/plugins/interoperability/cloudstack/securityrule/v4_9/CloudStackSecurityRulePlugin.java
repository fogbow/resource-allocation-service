package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9;

import org.apache.http.client.HttpResponseException;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.securityrules.SecurityRule;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.SecurityRulePlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackHttpToFogbowRasExceptionMapper;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;

import java.util.List;

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
    public List<SecurityRule> getSecurityRules(Order majorOrder, Token localUserAttributes) throws FogbowRasException, UnexpectedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteSecurityRule(String securityRuleId, Token localUserAttributes) throws FogbowRasException, UnexpectedException {
        DeleteFirewallRuleRequest request = new DeleteFirewallRuleRequest.Builder()
                .ruleId(securityRuleId)
                .build();

        CloudStackUrlUtil.sign(request.getUriBuilder(), localUserAttributes.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), localUserAttributes);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowRasExceptionMapper.map(e);
        }


    }
}
