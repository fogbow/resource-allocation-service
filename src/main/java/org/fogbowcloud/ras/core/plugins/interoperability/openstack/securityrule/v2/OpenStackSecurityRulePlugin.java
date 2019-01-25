package org.fogbowcloud.ras.core.plugins.interoperability.openstack.securityrule.v2;

import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.securityrules.*;
import org.fogbowcloud.ras.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.ras.core.plugins.interoperability.NetworkPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.PublicIpPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.SecurityRulePlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenStackHttpToFogbowRasExceptionMapper;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.fogbowcloud.ras.util.connectivity.AuditableHttpRequestClient;
import org.json.JSONException;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.fogbowcloud.ras.core.constants.Messages.Exception.INVALID_PROTOCOL;
import static org.fogbowcloud.ras.core.constants.Messages.Exception.MULTIPLE_SECURITY_GROUPS_EQUALLY_NAMED;

public class OpenStackSecurityRulePlugin implements SecurityRulePlugin<OpenStackV3Token> {
    private static final Logger LOGGER = Logger.getLogger(OpenStackSecurityRulePlugin.class);

    protected static final String NETWORK_NEUTRON_V2_URL_KEY = "openstack_neutron_v2_url";
    protected static final String V2_API_ENDPOINT = "/v2.0";
    protected static final String SUFFIX_ENDPOINT_SECURITY_GROUP_RULES = "/security-group-rules";
    protected static final String QUERY_PREFIX = "?";
    protected static final String VALUE_QUERY_PREFIX = "=";
    protected static final String SECURITY_GROUP_ID_PARAM = "security_group_id";
    protected static final String NAME_PARAM_KEY = "name";
    protected static final String SECURITY_GROUPS_ENDPOINT = "security-groups";

    private String networkV2APIEndpoint;
    private AuditableHttpRequestClient client;

    public OpenStackSecurityRulePlugin(String confFilePath) throws FatalErrorException {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.networkV2APIEndpoint = properties.getProperty(NETWORK_NEUTRON_V2_URL_KEY) + V2_API_ENDPOINT;
        initClient();
    }

    @Override
    public String requestSecurityRule(SecurityRule securityRule, Order majorOrder,
                                      OpenStackV3Token openStackV3Token) throws FogbowRasException, UnexpectedException {
            CreateSecurityRuleResponse createSecurityRuleResponse = null;

            String cidr = securityRule.getCidr();
            int portFrom = securityRule.getPortFrom();
            int portTo = securityRule.getPortTo();
            String direction = securityRule.getDirection().toString();
            String etherType = securityRule.getEtherType().toString();
            String protocol = securityRule.getProtocol().toString();

            String securityGroupName = retrieveSecurityGroupName(majorOrder);
            String securityGroupId = retrieveSecurityGroupId(securityGroupName, openStackV3Token);

            try {
                CreateSecurityRuleRequest createSecurityRuleRequest = new CreateSecurityRuleRequest.Builder()
                        .securityGroupId(securityGroupId)
                        .remoteIpPrefix(cidr)
                        .portRangeMin(portFrom)
                        .portRangeMax(portTo)
                        .direction(direction)
                        .etherType(etherType)
                        .protocol(protocol)
                        .build();

                String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SECURITY_GROUP_RULES;
                String response = this.client.doPostRequest(endpoint, openStackV3Token, createSecurityRuleRequest.toJson());
                createSecurityRuleResponse = CreateSecurityRuleResponse.fromJson(response);
            } catch (JSONException e) {
                String message = Messages.Error.UNABLE_TO_GENERATE_JSON;
                LOGGER.error(message, e);
                throw new InvalidParameterException(message, e);
            } catch (HttpResponseException e) {
                OpenStackHttpToFogbowRasExceptionMapper.map(e);
            }

            return createSecurityRuleResponse.getId();
    }

    @Override
    public List<SecurityRule> getSecurityRules(Order majorOrder, OpenStackV3Token openStackV3Token)
            throws FogbowRasException, UnexpectedException {
        String securityGroupName = retrieveSecurityGroupName(majorOrder);
        String securityGroupId = retrieveSecurityGroupId(securityGroupName, openStackV3Token);

        String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SECURITY_GROUP_RULES + QUERY_PREFIX +
                SECURITY_GROUP_ID_PARAM + VALUE_QUERY_PREFIX + securityGroupId;
        String responseStr = null;

        try {
            responseStr = this.client.doGetRequest(endpoint, openStackV3Token);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowRasExceptionMapper.map(e);
        }

        return getSecurityRulesFromJson(responseStr);
    }

    @Override
    public void deleteSecurityRule(String securityRuleId, OpenStackV3Token openStackV3Token)
            throws FogbowRasException, UnexpectedException {
        String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SECURITY_GROUP_RULES + "/" + securityRuleId;

        try {
            this.client.doDeleteRequest(endpoint, openStackV3Token);
        } catch (HttpResponseException e) {
            LOGGER.error(String.format(Messages.Error.UNABLE_TO_DELETE_INSTANCE, securityRuleId), e);
            OpenStackHttpToFogbowRasExceptionMapper.map(e);
        }

        LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE, securityRuleId, openStackV3Token.getTokenValue()));
    }

    private String retrieveSecurityGroupName(Order majorOrder) throws InvalidParameterException {
        String securityGroupName;
        switch (majorOrder.getType()) {
            case NETWORK:
                securityGroupName = NetworkPlugin.SECURITY_GROUP_PREFIX + majorOrder.getInstanceId();
                break;
            case PUBLIC_IP:
                securityGroupName = PublicIpPlugin.SECURITY_GROUP_PREFIX + majorOrder.getInstanceId();
                break;
            default:
                throw new InvalidParameterException();
        }
        return securityGroupName;
    }

    protected String retrieveSecurityGroupId(String securityGroupName, OpenStackV3Token openStackV3Token) throws UnexpectedException, FogbowRasException {
        URI uri = UriBuilder
                .fromPath(this.networkV2APIEndpoint)
                .path(SECURITY_GROUPS_ENDPOINT)
                .queryParam(NAME_PARAM_KEY, securityGroupName)
                .build();

        String endpoint = uri.toString();
        String responseStr = null;
        try {
            responseStr = this.client.doGetRequest(endpoint, openStackV3Token);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowRasExceptionMapper.map(e);
        }

        GetSecurityGroupsResponse getSecurityGroupsResponse = GetSecurityGroupsResponse.fromJson(responseStr);
        List<GetSecurityGroupsResponse.SecurityGroup> securityGroups = getSecurityGroupsResponse.getSecurityGroups();
        if (securityGroups.size() == 1) {
            return securityGroups.get(0).getId();
        } else {
            throw new FogbowRasException(String.format(MULTIPLE_SECURITY_GROUPS_EQUALLY_NAMED, securityGroupName));
        }
    }

    protected List<SecurityRule> getSecurityRulesFromJson(String json)
            throws FogbowRasException {
        GetSecurityRulesResponse getSecurityGroupResponse = GetSecurityRulesResponse.fromJson(json);
        List<GetSecurityRulesResponse.SecurityRules> securityRules = getSecurityGroupResponse.getSecurityRules();

        List<SecurityRule> rules = new ArrayList<>();
        try {
            for (GetSecurityRulesResponse.SecurityRules secRules : securityRules) {
                Direction direction = secRules.getDirection().equalsIgnoreCase("ingress") ? Direction.IN : Direction.OUT;
                EtherType etherType = secRules.getEtherType().equals("IPv6") ? EtherType.IPv6 : EtherType.IPv4;
                Protocol protocol;

                if (secRules.getProtocol() != null) {
                    switch(secRules.getProtocol()) {
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
                            throw new FogbowRasException(String.format(INVALID_PROTOCOL, secRules.getProtocol(),
                                    Protocol.values()));
                    }
                } else {
                    protocol = Protocol.ANY;
                }

                SecurityRule rule = new SecurityRule(direction, secRules.getPortFrom(),
                        secRules.getPortTo(), secRules.getCidr(), etherType, protocol);
                rule.setInstanceId(secRules.getId());
                rules.add(rule);
            }
        } catch (JSONException e) {
            String message = String.format(Messages.Error.UNABLE_TO_GET_SECURITY_GROUP, json);
            LOGGER.error(message, e);
            throw new InvalidParameterException(message, e);
        }

        return rules;
    }

    private void initClient() {
        this.client = new AuditableHttpRequestClient();
    }

    protected void setClient(AuditableHttpRequestClient client) {
        this.client = client;
    }
}
