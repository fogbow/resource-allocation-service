package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9;

import cloud.fogbow.common.constants.CloudStackConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.*;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.core.plugins.interoperability.SecurityRulePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9.CloudStackPublicIpPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9.CreateFirewallRuleAsyncResponse;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9.CreateFirewallRuleRequest;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class CloudStackSecurityRulePlugin implements SecurityRulePlugin<CloudStackUser> {

    public static final int ONE_SECOND_IN_MILIS = 1000;
    public static final int MAX_TRIES = 30;
    public static final String CLOUDSTACK_URL = "cloudstack_api_url";

    public static final Logger LOGGER = Logger.getLogger(CloudStackSecurityRulePlugin.class);

    private String cloudStackUrl;
    private CloudStackHttpClient client;
    private Properties properties;

    public CloudStackSecurityRulePlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        this.cloudStackUrl = this.properties.getProperty(CLOUDSTACK_URL);
        this.client = new CloudStackHttpClient();
    }

    @Override
    public String requestSecurityRule(SecurityRule securityRule, Order majorOrder, CloudStackUser cloudUser)
            throws FogbowException {
        if (securityRule.getDirection() == SecurityRule.Direction.OUT) {
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

            CloudStackUrlUtil.sign(createFirewallRuleRequest.getUriBuilder(), cloudUser.getToken());

            String jsonResponse = null;
            try {
                jsonResponse = this.client.doGetRequest(createFirewallRuleRequest.getUriBuilder().toString(), cloudUser);
            } catch (HttpResponseException e) {
                CloudStackHttpToFogbowExceptionMapper.map(e);
            }

            CreateFirewallRuleAsyncResponse response = CreateFirewallRuleAsyncResponse.fromJson(jsonResponse);

            return waitForJobResult(this.client, response.getJobId(), cloudUser);
        } else {
            throw new InvalidParameterException(Messages.Exception.INVALID_RESOURCE);
        }
    }

    @Override
    public List<SecurityRuleInstance> getSecurityRules(Order majorOrder, CloudStackUser cloudUser) throws FogbowException {
        switch (majorOrder.getType()) {
        	case PUBLIC_IP:
        		return getFirewallRules(CloudStackPublicIpPlugin.getPublicIpId(majorOrder.getId()), cloudUser);
        	case NETWORK:
        		throw new UnsupportedOperationException();
        	default:
				String errorMsg = String.format(Messages.Error.INVALID_LIST_SECURITY_RULE_TYPE, majorOrder.getType());
				LOGGER.error(errorMsg);
				throw new UnexpectedException(errorMsg);
        }
    }
       
    @Override
    public void deleteSecurityRule(String securityRuleId, CloudStackUser cloudUser) throws FogbowException {
        DeleteFirewallRuleRequest request = new DeleteFirewallRuleRequest.Builder()
                .ruleId(securityRuleId)
                .build(this.cloudStackUrl);

        CloudStackUrlUtil.sign(request.getUriBuilder(), cloudUser.getToken());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), cloudUser);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowExceptionMapper.map(e);
        }

        DeleteFirewallRuleResponse response = DeleteFirewallRuleResponse.fromJson(jsonResponse);

        waitForDeleteResult(this.client, response.getJobId(), cloudUser);
    }

    protected String waitForDeleteResult(CloudStackHttpClient client, String jobId, CloudStackUser cloudUser)
            throws FogbowException, UnexpectedException {
        CloudStackQueryAsyncJobResponse queryAsyncJobResult = getAsyncJobResponse(client, jobId, cloudUser);

        if (queryAsyncJobResult.getJobStatus() == CloudStackQueryJobResult.PROCESSING) {
            for (int i = 0; i < MAX_TRIES; i++) {
                queryAsyncJobResult = getAsyncJobResponse(client, jobId, cloudUser);
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

	protected List<SecurityRuleInstance> getFirewallRules(String ipAddressId, CloudStackUser cloudUser) throws FogbowException {
		ListFirewallRulesRequest request = new ListFirewallRulesRequest.Builder()
				.ipAddressId(ipAddressId)
				.build(this.cloudStackUrl);

		CloudStackUrlUtil.sign(request.getUriBuilder(), cloudUser.getToken());

		String jsonResponse = null;
		try {
			jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), cloudUser);
		} catch (HttpResponseException e) {
			CloudStackHttpToFogbowExceptionMapper.map(e);
		}

		ListFirewallRulesResponse response = ListFirewallRulesResponse.fromJson(jsonResponse);
		List<ListFirewallRulesResponse.SecurityRuleResponse> securityRulesResponse = response.getSecurityRulesResponse();
		return convertToFogbowSecurityRules(securityRulesResponse);
	}

	protected List<SecurityRuleInstance> convertToFogbowSecurityRules(List<ListFirewallRulesResponse.SecurityRuleResponse> securityRulesResponse) throws UnexpectedException {
		List<SecurityRuleInstance> securityRuleInstances = new ArrayList<SecurityRuleInstance>();
		for (ListFirewallRulesResponse.SecurityRuleResponse securityRuleResponse : securityRulesResponse) {
			SecurityRule.Direction direction = securityRuleResponse.getDirection();
			int portFrom = securityRuleResponse.getPortFrom();
			int portTo = securityRuleResponse.getPortTo();
			String cidr = securityRuleResponse.getCidr();
			String ipAddress = securityRuleResponse.getIpAddress();
			SecurityRule.EtherType etherType = inferEtherType(ipAddress);
			SecurityRule.Protocol protocol = getFogbowProtocol(securityRuleResponse.getProtocol());

			SecurityRule securityRule = new SecurityRule(direction, portFrom, portTo, cidr, etherType, protocol);
            SecurityRuleInstance securityRuleInstance = new SecurityRuleInstance(securityRuleResponse.getInstanceId(), securityRule);
			securityRuleInstances.add(securityRuleInstance);
		}
		return securityRuleInstances;
	}

	private SecurityRule.Protocol getFogbowProtocol(String protocol) throws UnexpectedException {
		switch (protocol) {
			case CloudStackConstants.SecurityGroupPlugin.TCP_VALUE_PROTOCOL:
				return SecurityRule.Protocol.TCP;
			case CloudStackConstants.SecurityGroupPlugin.UDP_VALUE_PROTOCOL:
				return SecurityRule.Protocol.UDP;
			case CloudStackConstants.SecurityGroupPlugin.ICMP_VALUE_PROTOCOL:
				return SecurityRule.Protocol.ICMP;
			case CloudStackConstants.SecurityGroupPlugin.ALL_VALUE_PROTOCOL:
				return SecurityRule.Protocol.ANY;
			default:
				throw new UnexpectedException(Messages.Exception.INVALID_CLOUDSTACK_PROTOCOL);
		}
	}

	private SecurityRule.EtherType inferEtherType(String ipAddress) {
		if (CidrUtils.isIpv4(ipAddress)) {
			return SecurityRule.EtherType.IPv4;
		} else if (CidrUtils.isIpv6(ipAddress)) {
			return SecurityRule.EtherType.IPv6;
		} else {
			return null;
		}
	}

    protected String waitForJobResult(CloudStackHttpClient client, String jobId, CloudStackUser cloudUser)
            throws FogbowException {
        CloudStackQueryAsyncJobResponse queryAsyncJobResult = getAsyncJobResponse(client, jobId, cloudUser);

        if (queryAsyncJobResult.getJobStatus() == CloudStackQueryJobResult.PROCESSING) {
            for (int i = 0; i < MAX_TRIES; i++) {
                queryAsyncJobResult = getAsyncJobResponse(client, jobId, cloudUser);
                if (queryAsyncJobResult.getJobStatus() != CloudStackQueryJobResult.PROCESSING) {
                    return processJobResult(queryAsyncJobResult, jobId);
                }
                try {
                    Thread.sleep(ONE_SECOND_IN_MILIS);
                } catch (InterruptedException e) {
                    throw new FogbowException();
                }
            }
            deleteSecurityRule(queryAsyncJobResult.getJobInstanceId(), cloudUser);
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

    protected CloudStackQueryAsyncJobResponse getAsyncJobResponse(CloudStackHttpClient client, String jobId, CloudStackUser cloudUser)
            throws FogbowException {
        String jsonResponse = CloudStackQueryJobResult.getQueryJobResult(client, this.cloudStackUrl, jobId, cloudUser);
        return CloudStackQueryAsyncJobResponse.fromJson(jsonResponse);
    }

    protected void setClient(CloudStackHttpClient client) {
        this.client = client;
    }
}
