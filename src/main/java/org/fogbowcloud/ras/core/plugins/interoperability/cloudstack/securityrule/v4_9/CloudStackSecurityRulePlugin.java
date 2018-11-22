package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9;

import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.securityrules.Direction;
import org.fogbowcloud.ras.core.models.securityrules.EtherType;
import org.fogbowcloud.ras.core.models.securityrules.SecurityRule;
import org.fogbowcloud.ras.core.models.tokens.CloudStackToken;
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

import static org.fogbowcloud.ras.core.constants.Messages.Error.UNEXPECTED_JOB_STATUS;
import static org.fogbowcloud.ras.core.constants.Messages.Exception.JOB_HAS_FAILED;
import static org.fogbowcloud.ras.core.constants.Messages.Exception.JOB_TIMEOUT;

public class CloudStackSecurityRulePlugin implements SecurityRulePlugin<CloudStackToken> {

    public static final int ONE_SECOND_IN_MILIS = 1000;
    private static final Logger LOGGER = Logger.getLogger(CloudStackSecurityRulePlugin.class);
    private static final int MAX_TRIES = 15;
    private HttpRequestClientUtil client;

    public CloudStackSecurityRulePlugin() {
        this.client = new HttpRequestClientUtil();
    }

    @Override
    public String requestSecurityRule(SecurityRule securityRule, Order majorOrder, CloudStackToken localUserAttributes)
            throws FogbowRasException, UnexpectedException {
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

        return waitForJobResult(this.client, response.getJobId(), localUserAttributes);
    }

    @Override
    public List<SecurityRule> getSecurityRules(Order majorOrder, CloudStackToken localUserAttributes)
            throws FogbowRasException, UnexpectedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteSecurityRule(String securityRuleId, CloudStackToken localUserAttributes)
            throws FogbowRasException, UnexpectedException {
        throw new UnsupportedOperationException();
    }

    private String waitForJobResult(HttpRequestClientUtil client, String jobId, CloudStackToken token)
            throws FogbowRasException, UnexpectedException {
        CloudStackQueryAsyncJobResponse queryAsyncJobResult = getAsyncJobResponse(client, jobId, token);

        if (queryAsyncJobResult.getJobStatus() == CloudStackQueryJobResult.PROCESSING) {
            for (int i = 0; i < MAX_TRIES; i++) {
                queryAsyncJobResult = getAsyncJobResponse(client, jobId, token);
                if (queryAsyncJobResult.getJobStatus() != CloudStackQueryJobResult.PROCESSING) {
                    return processJobResult(queryAsyncJobResult, jobId);
                }
                try {
                    Thread.sleep(ONE_SECOND_IN_MILIS);
                } catch (InterruptedException e) {
                    throw new FogbowRasException();
                }
            }
            deleteSecurityRule(queryAsyncJobResult.getJobInstanceId(), token);
            throw new FogbowRasException(String.format(JOB_TIMEOUT, jobId));
        }
        return processJobResult(queryAsyncJobResult, jobId);
    }

    private String processJobResult(CloudStackQueryAsyncJobResponse queryAsyncJobResult, String jobId)
            throws FogbowRasException, UnexpectedException {
        switch (queryAsyncJobResult.getJobStatus()){
            case CloudStackQueryJobResult.SUCCESS:
                return queryAsyncJobResult.getJobInstanceId();
            case CloudStackQueryJobResult.FAILURE:
                throw new FogbowRasException(String.format(JOB_HAS_FAILED, jobId));
            default:
                throw new UnexpectedException(UNEXPECTED_JOB_STATUS);
        }
    }

    private CloudStackQueryAsyncJobResponse getAsyncJobResponse(HttpRequestClientUtil client, String jobId, Token token)
            throws FogbowRasException {
        String jsonResponse = CloudStackQueryJobResult.getQueryJobResult(client, jobId, token);
        return CloudStackQueryAsyncJobResponse.fromJson(jsonResponse);
    }
}
