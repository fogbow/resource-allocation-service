package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.constants.ConfigurationConstants;
import cloud.fogbow.ras.core.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackHttpToFogbowExceptionMapper;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackQueryAsyncJobResponse;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9.CloudStackPublicIpPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9.CreateFirewallRuleAsyncResponse;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9.CreateFirewallRuleRequest;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.securityrules.Direction;
import cloud.fogbow.ras.core.models.securityrules.EtherType;
import cloud.fogbow.ras.core.models.securityrules.Protocol;
import cloud.fogbow.ras.core.models.securityrules.SecurityRule;
import cloud.fogbow.ras.core.plugins.interoperability.SecurityRulePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackQueryJobResult;
import cloud.fogbow.ras.util.connectivity.AuditableHttpRequestClient;

public class CloudStackSecurityRulePlugin implements SecurityRulePlugin {

    public static final int ONE_SECOND_IN_MILIS = 1000;
    public static final int MAX_TRIES = 30;
    public static final String CLOUDSTACK_URL = "cloudstack_api_url";

    public static final Logger LOGGER = Logger.getLogger(CloudStackSecurityRulePlugin.class);

    private String cloudStackUrl;
    private AuditableHttpRequestClient client;
    private Properties properties;

    public CloudStackSecurityRulePlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        this.cloudStackUrl = this.properties.getProperty(CLOUDSTACK_URL);
        this.client = new AuditableHttpRequestClient(new Integer(PropertiesHolder.getInstance().getProperty(ConfigurationConstants.HTTP_REQUEST_TIMEOUT)));
    }

    @Override
    public String requestSecurityRule(SecurityRule securityRule, Order majorOrder, CloudToken localUserAttributes)
            throws FogbowException {
        if (securityRule.getDirection() == Direction.OUT) {
            throw new UnsupportedOperationException();
        }
        if (majorOrder.getType() == ResourceType.PUBLIC_IP) {
            String cidr = securityRule.getCidr();
            String portFrom = Integer.toString(securityRule.getPortFrom());
            String portTo = Integer.toString(securityRule.getPortTo());
            String protocol = securityRule.getProtocol().toString();

            CreateFirewallRuleRequest createFirewallRuleRequest = new CreateFirewallRuleRequest.Builder()
                    .protocol(protocol)
                    .startPort(portFrom)
                    .endPort(portTo)
                    .ipAddressId(CloudStackPublicIpPlugin.getPublicIpId(majorOrder.getId()))
                    .cidrList(cidr)
                    .build(this.cloudStackUrl);

            CloudStackUrlUtil.sign(createFirewallRuleRequest.getUriBuilder(), localUserAttributes.getTokenValue());

            String jsonResponse = null;
            try {
                jsonResponse = this.client.doGetRequest(createFirewallRuleRequest.getUriBuilder().toString(), localUserAttributes);
            } catch (HttpResponseException e) {
                CloudStackHttpToFogbowExceptionMapper.map(e);
            }

            CreateFirewallRuleAsyncResponse response = CreateFirewallRuleAsyncResponse.fromJson(jsonResponse);

            return waitForJobResult(this.client, response.getJobId(), localUserAttributes);
        } else {
            throw new InvalidParameterException(Messages.Exception.INVALID_RESOURCE);
        }
    }

    @Override
    public List<SecurityRule> getSecurityRules(Order majorOrder, CloudToken localUserAttributes) throws FogbowException, UnexpectedException {
        switch (majorOrder.getType()) {
        	case PUBLIC_IP:
        		return getFirewallRules(CloudStackPublicIpPlugin.getPublicIpId(majorOrder.getId()), localUserAttributes);
        	case NETWORK:
        		throw new UnsupportedOperationException();
        	default:
				String errorMsg = String.format(Messages.Error.INVALID_LIST_SECURITY_RULE_TYPE, majorOrder.getType());
				LOGGER.error(errorMsg);
				throw new UnexpectedException(errorMsg);
        }
    }
       
    @Override
    public void deleteSecurityRule(String securityRuleId, CloudToken localUserAttributes)
            throws FogbowException, UnexpectedException {
        DeleteFirewallRuleRequest request = new DeleteFirewallRuleRequest.Builder()
                .ruleId(securityRuleId)
                .build(this.cloudStackUrl);

        CloudStackUrlUtil.sign(request.getUriBuilder(), localUserAttributes.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), localUserAttributes);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowExceptionMapper.map(e);
        }

        DeleteFirewallRuleResponse response = DeleteFirewallRuleResponse.fromJson(jsonResponse);

        waitForDeleteResult(this.client, response.getJobId(), localUserAttributes);
    }

    protected String waitForDeleteResult(AuditableHttpRequestClient client, String jobId, CloudToken token)
            throws FogbowException, UnexpectedException {
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
                    throw new FogbowException();
                }
            }
            throw new FogbowException(String.format(Messages.Exception.JOB_TIMEOUT, jobId));
        } else {
            throw new UnexpectedException();
        }
    }

	protected List<SecurityRule> getFirewallRules(String ipAddressId, CloudToken localUserAttributes) throws FogbowException, UnexpectedException {
		ListFirewallRulesRequest request = new ListFirewallRulesRequest.Builder()
				.ipAddressId(ipAddressId)
				.build(this.cloudStackUrl);

		CloudStackUrlUtil.sign(request.getUriBuilder(), localUserAttributes.getTokenValue());

		String jsonResponse = null;
		try {
			jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), localUserAttributes);
		} catch (HttpResponseException e) {
			CloudStackHttpToFogbowExceptionMapper.map(e);
		}

		ListFirewallRulesResponse response = ListFirewallRulesResponse.fromJson(jsonResponse);
		List<ListFirewallRulesResponse.SecurityRuleResponse> securityRulesResponse = response.getSecurityRulesResponse();
		return convertToFogbowSecurityRules(securityRulesResponse);
	}

	protected List<SecurityRule> convertToFogbowSecurityRules(List<ListFirewallRulesResponse.SecurityRuleResponse> securityRulesResponse) throws UnexpectedException {
		List<SecurityRule> securityRules = new ArrayList<SecurityRule>();
		for (ListFirewallRulesResponse.SecurityRuleResponse securityRuleResponse : securityRulesResponse) {
			Direction direction = securityRuleResponse.getDirection();
			int portFrom = securityRuleResponse.getPortFrom();
			int portTo = securityRuleResponse.getPortTo();
			String cidr = securityRuleResponse.getCidr();
			String ipAddress = securityRuleResponse.getIpAddress();
			EtherType etherType = inferEtherType(ipAddress);
			Protocol protocol = getFogbowProtocol(securityRuleResponse.getProtocol());

			SecurityRule securityRule = new SecurityRule(direction, portFrom, portTo, cidr, etherType, protocol);
			securityRule.setInstanceId(securityRuleResponse.getInstanceId());

			securityRules.add(securityRule);
		}
		return securityRules;
	}

	private Protocol getFogbowProtocol(String protocol) throws UnexpectedException {
		switch (protocol) {
			case CloudStackRestApiConstants.SecurityGroupPlugin.TCP_VALUE_PROTOCOL:
				return Protocol.TCP;
			case CloudStackRestApiConstants.SecurityGroupPlugin.UDP_VALUE_PROTOCOL:
				return Protocol.UDP;
			case CloudStackRestApiConstants.SecurityGroupPlugin.ICMP_VALUE_PROTOCOL:
				return Protocol.ICMP;
			case CloudStackRestApiConstants.SecurityGroupPlugin.ALL_VALUE_PROTOCOL:
				return Protocol.ANY;
			default:
				throw new UnexpectedException(Messages.Exception.INVALID_CLOUDSTACK_PROTOCOL);
		}
	}

	private EtherType inferEtherType(String ipAddress) {
		if (CidrUtils.isIpv4(ipAddress)) {
			return EtherType.IPv4;
		} else if (CidrUtils.isIpv6(ipAddress)) {
			return EtherType.IPv6;
		} else {
			return null;
		}
	}

    protected String waitForJobResult(AuditableHttpRequestClient client, String jobId, CloudToken token)
            throws FogbowException {
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
                    throw new FogbowException();
                }
            }
            deleteSecurityRule(queryAsyncJobResult.getJobInstanceId(), token);
            throw new FogbowException(String.format(Messages.Exception.JOB_TIMEOUT, jobId));
        }
        return processJobResult(queryAsyncJobResult, jobId);
    }

    protected String processJobResult(CloudStackQueryAsyncJobResponse queryAsyncJobResult,
                                      String jobId)
            throws FogbowException, UnexpectedException {
        switch (queryAsyncJobResult.getJobStatus()){
            case CloudStackQueryJobResult.SUCCESS:
                return queryAsyncJobResult.getJobInstanceId();
            case CloudStackQueryJobResult.FAILURE:
                throw new FogbowException(String.format(Messages.Exception.JOB_HAS_FAILED, jobId));
            default:
                throw new UnexpectedException(Messages.Error.UNEXPECTED_JOB_STATUS);
        }
    }

    protected CloudStackQueryAsyncJobResponse getAsyncJobResponse(AuditableHttpRequestClient client, String jobId, CloudToken token)
            throws FogbowException {
        String jsonResponse = CloudStackQueryJobResult.getQueryJobResult(client, jobId, token);
        return CloudStackQueryAsyncJobResponse.fromJson(jsonResponse);
    }

    protected void setClient(AuditableHttpRequestClient client) {
        this.client = client;
    }
}
