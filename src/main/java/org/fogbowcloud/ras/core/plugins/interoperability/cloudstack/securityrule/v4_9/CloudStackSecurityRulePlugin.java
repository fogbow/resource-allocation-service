package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9;

import org.apache.http.client.HttpResponseException;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.securitygroups.Direction;
import org.fogbowcloud.ras.core.models.securitygroups.EtherType;
import org.fogbowcloud.ras.core.models.securitygroups.SecurityGroupRule;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.SecurityGroupPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackHttpToFogbowRasExceptionMapper;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackQueryAsyncJobResponse;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackQueryJobResult;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.publicip.v4_9.CreateFirewallRuleAsyncResponse;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.publicip.v4_9.CreateFirewallRuleRequest;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;

import java.util.List;

public class CloudStackSecurityRulePlugin implements SecurityGroupPlugin {

    private HttpRequestClientUtil client;

    public CloudStackSecurityRulePlugin() {
        this.client = new HttpRequestClientUtil();
    }

    @Override
    public String requestSecurityGroupRule(SecurityGroupRule securityGroupRule, Order majorOrder,
                                           Token localUserAttributes) throws FogbowRasException, UnexpectedException {
        if (securityGroupRule.getDirection() == Direction.OUT) {
            throw new UnsupportedOperationException();
        }
        if (securityGroupRule.getEtherType() == EtherType.IPv6) {
            throw new UnsupportedOperationException();
        }

        String cidr = securityGroupRule.getCidr();
        String portFrom = Integer.toString(securityGroupRule.getPortFrom());
        String portTo = Integer.toString(securityGroupRule.getPortTo());
        String protocol = securityGroupRule.getProtocol().toString();

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
    public List<SecurityGroupRule> getSecurityGroupRules(Order majorOrder, Token localUserAttributes) throws FogbowRasException, UnexpectedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteSecurityGroupRule(String securityGroupRuleId, Token localUserAttributes) throws FogbowRasException, UnexpectedException {
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
