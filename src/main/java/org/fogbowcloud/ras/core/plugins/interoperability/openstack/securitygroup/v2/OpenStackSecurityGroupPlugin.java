package org.fogbowcloud.ras.core.plugins.interoperability.openstack.securitygroup.v2;

import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.constants.SystemConstants;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.securitygroups.*;
import org.fogbowcloud.ras.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.ras.core.plugins.interoperability.SecurityGroupPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenStackHttpToFogbowRasExceptionMapper;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class OpenStackSecurityGroupPlugin implements SecurityGroupPlugin<OpenStackV3Token> {
    private static final Logger LOGGER = Logger.getLogger(OpenStackSecurityGroupPlugin.class);

    public static final String NETWORK_NEUTRONV2_URL_KEY = "openstack_neutron_v2_url";
    protected static final String V2_API_ENDPOINT = "/v2.0";
    protected static final String SUFFIX_ENDPOINT_SECURITY_GROUP_RULES = "/security-group-rules";
    protected static final String QUERY_PREFIX = "?";
    protected static final String VALUE_QUERY_PREFIX = "?";
    protected static final String SECURITY_GROUP_ID_PARAM = "security_group_id";

    private String networkV2APIEndpoint;
    private HttpRequestClientUtil client;

    public OpenStackSecurityGroupPlugin() throws FatalErrorException {
        Properties properties = PropertiesUtil.readProperties(HomeDir.getPath() +
                SystemConstants.OPENSTACK_CONF_FILE_NAME);
        this.networkV2APIEndpoint = properties.getProperty(NETWORK_NEUTRONV2_URL_KEY) + V2_API_ENDPOINT;
        initClient();
    }

    @Override
    public String requestSecurityGroupRule(SecurityGroupRule securityGroupRule, String securityGroupId,
                                           OpenStackV3Token openStackV3Token)
            throws FogbowRasException, UnexpectedException {
        CreateSecurityGroupRuleResponse createSecurityGroupRuleResponse = null;

        String cidr = securityGroupRule.getCidr();
        int portFrom = securityGroupRule.getPortFrom();
        int portTo = securityGroupRule.getPortTo();
        String direction = securityGroupRule.getDirection().toString();
        String etherType = securityGroupRule.getEtherType().toString();
        String protocol = securityGroupRule.getProtocol().toString();

        try {
            CreateSecurityGroupRuleRequest createSecurityGroupRuleRequest = new CreateSecurityGroupRuleRequest.Builder()
                    .securityGroupId(securityGroupId)
                    .remoteIpPrefix(cidr)
                    .portRangeMin(portFrom)
                    .portRangeMax(portTo)
                    .direction(direction)
                    .etherType(etherType)
                    .protocol(protocol)
                    .build();

            String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SECURITY_GROUP_RULES;
            String response = this.client.doPostRequest(endpoint, openStackV3Token, createSecurityGroupRuleRequest.toJson());
            createSecurityGroupRuleResponse = CreateSecurityGroupRuleResponse.fromJson(response);
        } catch (JSONException e) {
            String message = Messages.Error.UNABLE_TO_GENERATE_JSON;
            LOGGER.error(message, e);
            throw new InvalidParameterException(message, e);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowRasExceptionMapper.map(e);
        }

        return createSecurityGroupRuleResponse.getId();
    }

    @Override
    public List<SecurityGroupRule> getSecurityGroupRules(String securityGroupId, OpenStackV3Token openStackV3Token)
            throws FogbowRasException, UnexpectedException {
        String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SECURITY_GROUP_RULES + QUERY_PREFIX +
                SECURITY_GROUP_ID_PARAM + VALUE_QUERY_PREFIX + securityGroupId;
        String responseStr = null;

        try {
            responseStr = this.client.doGetRequest(endpoint, openStackV3Token);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowRasExceptionMapper.map(e);
        }

        return getSecurityGroupFromJson(responseStr);
    }

    protected List<SecurityGroupRule> getSecurityGroupFromJson(String json)
            throws FogbowRasException {
        GetSecurityGroupRulesResponse getSecurityGroupResponse = GetSecurityGroupRulesResponse.fromJson(json);
        List<GetSecurityGroupRulesResponse.SecurityGroupRule> securityGroupRules = getSecurityGroupResponse.getSecurityGroupRules();

        List<SecurityGroupRule> rules = new ArrayList<>();
        try {
            for (GetSecurityGroupRulesResponse.SecurityGroupRule securityGroupRule : securityGroupRules) {
                Direction direction = securityGroupRule.getDirection().equalsIgnoreCase("ingress") ? Direction.IN : Direction.OUT;
                EtherType etherType = securityGroupRule.getEtherType().equals("IPv6") ? EtherType.IPv6 : EtherType.IPv4;
                Protocol protocol;

                switch(securityGroupRule.getProtocol()) {
                    case "tcp":
                        protocol = Protocol.TCP;
                        break;
                    case "udp":
                        protocol = Protocol.UDP;
                        break;
                    case "icmp":
                        protocol = Protocol.ICMP;
                        break;
                    default:
                        protocol = Protocol.TCP;
                }

                SecurityGroupRule rule = new SecurityGroupRule(direction, securityGroupRule.getPortFrom(),
                        securityGroupRule.getPortTo(), securityGroupRule.getCidr(), etherType, protocol);
                rule.setInstanceId(securityGroupRule.getId());
                rules.add(rule);
            }
        } catch (JSONException e) {
            String message = String.format(Messages.Error.UNABLE_TO_GET_SECURITY_GROUP, json);
            LOGGER.error(message, e);
            throw new InvalidParameterException(message, e);
        }

        return rules;
    }

    @Override
    public void deleteSecurityGroupRule(String securityGroupRuleId, OpenStackV3Token openStackV3Token)
            throws FogbowRasException, UnexpectedException {
        String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SECURITY_GROUP_RULES + "/" + securityGroupRuleId;

        try {
            this.client.doDeleteRequest(endpoint, openStackV3Token);
        } catch (HttpResponseException e) {
            LOGGER.error(String.format(Messages.Error.UNABLE_TO_DELETE_INSTANCE, securityGroupRuleId));
            OpenStackHttpToFogbowRasExceptionMapper.map(e);
        }

        LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE, securityGroupRuleId, openStackV3Token.getTokenValue()));
    }

    private void initClient() {
        this.client = new HttpRequestClientUtil();
    }

    protected void setClient(HttpRequestClientUtil client) {
        this.client = client;
    }
}
