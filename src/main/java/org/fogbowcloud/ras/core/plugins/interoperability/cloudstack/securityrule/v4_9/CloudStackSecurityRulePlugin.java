package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9;

import org.apache.http.client.HttpResponseException;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.securityrules.Direction;
import org.fogbowcloud.ras.core.models.securityrules.EtherType;
import org.fogbowcloud.ras.core.models.securityrules.SecurityRule;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.SecurityRulePlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackHttpToFogbowRasExceptionMapper;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackQueryAsyncJobResponse;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackQueryJobResult;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.publicip.v4_9.CreateFirewallRuleAsyncResponse;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.publicip.v4_9.CreateFirewallRuleRequest;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;

import java.util.List;

public class CloudStackSecurityRulePlugin implements SecurityRulePlugin {

    private HttpRequestClientUtil client;

    public CloudStackSecurityRulePlugin() {
        this.client = new HttpRequestClientUtil();
    }

    @Override
    public String requestSecurityRule(SecurityRule securityRule, Order majorOrder,
                                      Token localUserAttributes) throws FogbowRasException, UnexpectedException {
        if (securityRule.getDirection() == Direction.OUT) {
            throw new UnsupportedOperationException();
        }
        if (securityRule.getEtherType() == EtherType.IPv6) {
            throw new UnsupportedOperationException();
        }

        String cidr = securityRule.getCidr();
        String portFrom = Integer.toString(securityRule.getPortFrom());
        String portTo = Integer.toString(securityRule.getPortTo());
        String protocol = securityRule.getProtocol().toString();

        CreateFirewallRuleRequest createFirewallRuleRequest = new CreateFirewallRuleRequest.Builder()
                .protocol(protocol)
                .startPort(portFrom)
                .endPort(portTo)
                .ipAddressId(cidr)
                .build();

        CloudStackUrlUtil.sign(createFirewallRuleRequest.getUriBuilder(), localUserAttributes.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(createFirewallRuleRequest.getUriBuilder().toString(), localUserAttributes);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowRasExceptionMapper.map(e);
        }

        CreateFirewallRuleAsyncResponse response = CreateFirewallRuleAsyncResponse.fromJson(jsonResponse);

        waitForJobResult(this.client, response.getJobId(), localUserAttributes);

        return null;
    }

    @Override
    public List<SecurityRule> getSecurityRules(Order majorOrder, Token localUserAttributes) throws FogbowRasException, UnexpectedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteSecurityRule(String securityRuleId, Token localUserAttributes) throws FogbowRasException, UnexpectedException {
        throw new UnsupportedOperationException();
    }

    private void waitForJobResult(HttpRequestClientUtil client, String jobId, Token token) throws FogbowRasException {
        String jsonResponse = CloudStackQueryJobResult.getQueryJobResult(client, jobId, token);
        CloudStackQueryAsyncJobResponse queryAsyncJobResult = CloudStackQueryAsyncJobResponse.fromJson(jsonResponse);

        switch (queryAsyncJobResult.getJobStatus()) {
            case CloudStackQueryJobResult.PROCESSING:
                break;
            case CloudStackQueryJobResult.SUCCESS:
                break;
            case CloudStackQueryJobResult.FAILURE:
                break;
            default:
                break;
        }
    }
}
