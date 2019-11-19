package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9;

import cloud.fogbow.common.constants.CloudStackConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpToFogbowExceptionMapper;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackQueryAsyncJobResponse;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.plugins.interoperability.SecurityRulePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9.CloudStackPublicIpPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9.CreateFirewallRuleAsyncResponse;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9.CreateFirewallRuleRequest;
import com.google.common.annotations.VisibleForTesting;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class CloudStackSecurityRulePlugin implements SecurityRulePlugin<CloudStackUser> {
    public static final Logger LOGGER = Logger.getLogger(CloudStackSecurityRulePlugin.class);

    private String cloudStackUrl;
    private CloudStackHttpClient client;

    public CloudStackSecurityRulePlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.cloudStackUrl = properties.getProperty(CloudStackCloudUtils.CLOUDSTACK_URL_CONFIG);
        this.client = new CloudStackHttpClient();
    }

    @Override
    public String requestSecurityRule(@NotNull SecurityRule securityRule,
                                      @NotNull Order majorOrder,
                                      @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE_FROM_PROVIDER));
        checkRequestSecurityParameters(securityRule, majorOrder);

        String cidr = securityRule.getCidr();
        String portFrom = Integer.toString(securityRule.getPortFrom());
        String portTo = Integer.toString(securityRule.getPortTo());
        String protocol = securityRule.getProtocol().toString();
        String publicIpId = CloudStackPublicIpPlugin.getPublicIpId(majorOrder.getId());
        CreateFirewallRuleRequest request = new CreateFirewallRuleRequest.Builder()
                .protocol(protocol)
                .startPort(portFrom)
                .endPort(portTo)
                .ipAddressId(publicIpId)
                .cidrList(cidr)
                .build(this.cloudStackUrl);

        return doRequestInstance(request, cloudStackUser);
    }

    @Override
    public List<SecurityRuleInstance> getSecurityRules(@NotNull Order majorOrder,
                                                       @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE_S, majorOrder.getInstanceId()));
        return doGetSecurityRules(majorOrder, cloudStackUser);
    }

    @Override
    public void deleteSecurityRule(String securityRuleId, @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE_S, securityRuleId));
        DeleteFirewallRuleRequest request = new DeleteFirewallRuleRequest.Builder()
                .ruleId(securityRuleId)
                .build(this.cloudStackUrl);

        doDeleteInstance(request, cloudStackUser);
    }

    @NotNull
    @VisibleForTesting
    List<SecurityRuleInstance> doGetSecurityRules(@NotNull Order majorOrder,
                                                  @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        switch (majorOrder.getType()) {
            case PUBLIC_IP:
                String publicIpId = CloudStackPublicIpPlugin.getPublicIpId(majorOrder.getId());
                return getFirewallRules(publicIpId, cloudStackUser);
            case NETWORK:
                return new ArrayList<>();
            default:
                String errorMsg = String.format(Messages.Error.INVALID_LIST_SECURITY_RULE_TYPE, majorOrder.getType());
                throw new UnexpectedException(errorMsg);
        }
    }

    @VisibleForTesting
    void doDeleteInstance(@NotNull DeleteFirewallRuleRequest request,
                          @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        URIBuilder uriRequest = request.getUriBuilder();
        CloudStackUrlUtil.sign(uriRequest, cloudStackUser.getToken());

        try {
            String jsonResponse = CloudStackCloudUtils.doRequest(this.client,
                    uriRequest.toString(), cloudStackUser);
            DeleteFirewallRuleResponse response = DeleteFirewallRuleResponse.fromJson(jsonResponse);
            CloudStackCloudUtils.waitForResult(this.client, this.cloudStackUrl,
                    response.getJobId(), cloudStackUser);
        } catch (HttpResponseException e) {
            throw CloudStackHttpToFogbowExceptionMapper.get(e);
        }
    }

    @VisibleForTesting
    void checkRequestSecurityParameters(@NotNull SecurityRule securityRule,
                                        @NotNull Order majorOrder) throws InvalidParameterException {

        if (securityRule.getDirection() == SecurityRule.Direction.OUT) {
            throw new InvalidParameterException(Messages.Exception.INVALID_PARAMETER);
        }
        if (majorOrder.getType() != ResourceType.PUBLIC_IP) {
            throw new InvalidParameterException(Messages.Exception.INVALID_RESOURCE);
        }
    }

    @NotNull
    @VisibleForTesting
    String doRequestInstance(@NotNull CreateFirewallRuleRequest request,
                             @NotNull CloudStackUser cloudStackUser) throws FogbowException {

        URIBuilder uriRequest = request.getUriBuilder();
        CloudStackUrlUtil.sign(uriRequest, cloudStackUser.getToken());

        try {
            String jsonResponse = CloudStackCloudUtils.doRequest(
                    this.client, uriRequest.toString(), cloudStackUser);
            CreateFirewallRuleAsyncResponse response = CreateFirewallRuleAsyncResponse.fromJson(jsonResponse);
            try {
                return CloudStackCloudUtils.waitForResult(
                        this.client, this.cloudStackUrl, response.getJobId(), cloudStackUser);
            } catch (CloudStackCloudUtils.CloudStackTimeoutException e) {
                CloudStackQueryAsyncJobResponse asyncJobResponse = CloudStackCloudUtils.getAsyncJobResponse(
                        this.client, this.cloudStackUrl, response.getJobId(), cloudStackUser);
                deleteSecurityRule(asyncJobResponse.getJobInstanceId(), cloudStackUser);
                throw e;
            }
        } catch (HttpResponseException e) {
            throw CloudStackHttpToFogbowExceptionMapper.get(e);
        }
    }

    @NotNull
    @VisibleForTesting
    List<SecurityRuleInstance> getFirewallRules(String ipAddressId, @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        ListFirewallRulesRequest request = new ListFirewallRulesRequest.Builder()
                .ipAddressId(ipAddressId)
                .build(this.cloudStackUrl);

        URIBuilder uriRequest = request.getUriBuilder();
        CloudStackUrlUtil.sign(uriRequest, cloudStackUser.getToken());

        try {
            String jsonResponse = CloudStackCloudUtils.doRequest(
                    this.client, uriRequest.toString(), cloudStackUser);
            ListFirewallRulesResponse response = ListFirewallRulesResponse.fromJson(jsonResponse);
            List<ListFirewallRulesResponse.SecurityRuleResponse> securityRulesResponse =
                    response.getSecurityRulesResponse();
            return convertToFogbowSecurityRules(securityRulesResponse);
        } catch (HttpResponseException e) {
            throw CloudStackHttpToFogbowExceptionMapper.get(e);
        }
    }

    @NotNull
    @VisibleForTesting
    List<SecurityRuleInstance> convertToFogbowSecurityRules(
            @NotNull List<ListFirewallRulesResponse.SecurityRuleResponse> securityRulesResponse) {

        List<SecurityRuleInstance> securityRuleInstances = new ArrayList<SecurityRuleInstance>();
        for (ListFirewallRulesResponse.SecurityRuleResponse securityRuleResponse : securityRulesResponse) {
            SecurityRule.Direction direction = securityRuleResponse.getDirection();
            int portFrom = securityRuleResponse.getPortFrom();
            int portTo = securityRuleResponse.getPortTo();
            String cidr = securityRuleResponse.getCidr();
            String ipAddress = securityRuleResponse.getIpAddress();
            SecurityRule.EtherType etherType = inferEtherType(ipAddress);
            SecurityRule.Protocol protocol = getFogbowProtocol(securityRuleResponse.getProtocol());
            String instanceId = securityRuleResponse.getInstanceId();

            SecurityRuleInstance securityRuleInstance = new SecurityRuleInstance(instanceId, direction,
                    portFrom, portTo, cidr, etherType, protocol);
            securityRuleInstances.add(securityRuleInstance);
        }
        return securityRuleInstances;
    }

    @VisibleForTesting
    SecurityRule.Protocol getFogbowProtocol(String protocol) {
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
                return null;
        }
    }

    @VisibleForTesting
    SecurityRule.EtherType inferEtherType(String ipAddress) {
        if (CidrUtils.isIpv4(ipAddress)) {
            return SecurityRule.EtherType.IPv4;
        } else if (CidrUtils.isIpv6(ipAddress)) {
            return SecurityRule.EtherType.IPv6;
        } else {
            return null;
        }
    }

    @VisibleForTesting
    void setClient(CloudStackHttpClient client) {
        this.client = client;
    }
}
