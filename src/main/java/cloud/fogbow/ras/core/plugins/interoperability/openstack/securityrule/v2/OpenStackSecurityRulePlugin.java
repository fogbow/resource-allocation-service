package cloud.fogbow.ras.core.plugins.interoperability.openstack.securityrule.v2;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.api.http.response.securityrules.Direction;
import cloud.fogbow.ras.api.http.response.securityrules.EtherType;
import cloud.fogbow.ras.api.http.response.securityrules.Protocol;
import cloud.fogbow.ras.api.http.response.securityrules.SecurityRule;
import cloud.fogbow.ras.core.plugins.interoperability.NetworkPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.PublicIpPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.SecurityRulePlugin;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpToFogbowExceptionMapper;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.json.JSONException;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class OpenStackSecurityRulePlugin implements SecurityRulePlugin<OpenStackV3User> {
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
    private OpenStackHttpClient client;

    public OpenStackSecurityRulePlugin(String confFilePath) throws FatalErrorException {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.networkV2APIEndpoint = properties.getProperty(NETWORK_NEUTRON_V2_URL_KEY) + V2_API_ENDPOINT;
        initClient();
    }

    @Override
    public String requestSecurityRule(SecurityRule securityRule, Order majorOrder,
                                      OpenStackV3User cloudUser) throws FogbowException {
            CreateSecurityRuleResponse createSecurityRuleResponse = null;

            String cidr = securityRule.getCidr();
            int portFrom = securityRule.getPortFrom();
            int portTo = securityRule.getPortTo();
            String direction = securityRule.getDirection().toString();
            String etherType = securityRule.getEtherType().toString();
            String protocol = securityRule.getProtocol().toString();

            String securityGroupName = retrieveSecurityGroupName(majorOrder);
            String securityGroupId = retrieveSecurityGroupId(securityGroupName, cloudUser);

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
                String response = this.client.doPostRequest(endpoint, createSecurityRuleRequest.toJson(), cloudUser);
                createSecurityRuleResponse = CreateSecurityRuleResponse.fromJson(response);
            } catch (JSONException e) {
                String message = Messages.Error.UNABLE_TO_GENERATE_JSON;
                LOGGER.error(message, e);
                throw new InvalidParameterException(message, e);
            } catch (HttpResponseException e) {
                OpenStackHttpToFogbowExceptionMapper.map(e);
            }

            return createSecurityRuleResponse.getId();
    }

    @Override
    public List<SecurityRule> getSecurityRules(Order majorOrder, OpenStackV3User cloudUser) throws FogbowException {
        String securityGroupName = retrieveSecurityGroupName(majorOrder);
        String securityGroupId = retrieveSecurityGroupId(securityGroupName, cloudUser);

        String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SECURITY_GROUP_RULES + QUERY_PREFIX +
                SECURITY_GROUP_ID_PARAM + VALUE_QUERY_PREFIX + securityGroupId;
        String responseStr = null;

        try {
            responseStr = this.client.doGetRequest(endpoint, cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }

        return getSecurityRulesFromJson(responseStr);
    }

    @Override
    public void deleteSecurityRule(String securityRuleId, OpenStackV3User cloudUser) throws FogbowException {
        String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SECURITY_GROUP_RULES + "/" + securityRuleId;

        try {
            this.client.doDeleteRequest(endpoint, cloudUser);
        } catch (HttpResponseException e) {
            LOGGER.error(String.format(Messages.Error.UNABLE_TO_DELETE_INSTANCE, securityRuleId), e);
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }

        LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE, securityRuleId, cloudUser.getToken()));
    }

    private String retrieveSecurityGroupName(Order majorOrder) throws InvalidParameterException {
        String securityGroupName;
        switch (majorOrder.getType()) {
            case NETWORK:
                securityGroupName = SystemConstants.PN_SECURITY_GROUP_PREFIX + majorOrder.getInstanceId();
                break;
            case PUBLIC_IP:
                securityGroupName = SystemConstants.PIP_SECURITY_GROUP_PREFIX + majorOrder.getInstanceId();
                break;
            default:
                throw new InvalidParameterException();
        }
        return securityGroupName;
    }

    protected String retrieveSecurityGroupId(String securityGroupName, OpenStackV3User cloudUser) throws FogbowException {
        URI uri = UriBuilder
                .fromPath(this.networkV2APIEndpoint)
                .path(SECURITY_GROUPS_ENDPOINT)
                .queryParam(NAME_PARAM_KEY, securityGroupName)
                .build();

        String endpoint = uri.toString();
        String responseStr = null;
        try {
            responseStr = this.client.doGetRequest(endpoint, cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }

        GetSecurityGroupsResponse getSecurityGroupsResponse = GetSecurityGroupsResponse.fromJson(responseStr);
        List<GetSecurityGroupsResponse.SecurityGroup> securityGroups = getSecurityGroupsResponse.getSecurityGroups();
        if (securityGroups.size() == 1) {
            return securityGroups.get(0).getId();
        } else {
            throw new FogbowException(String.format(Messages.Exception.MULTIPLE_SECURITY_GROUPS_EQUALLY_NAMED, securityGroupName));
        }
    }

    protected List<SecurityRule> getSecurityRulesFromJson(String json) throws FogbowException {
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
                            throw new FogbowException(String.format(Messages.Exception.INVALID_PROTOCOL, secRules.getProtocol(),
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
        this.client = new OpenStackHttpClient();
    }

    protected void setClient(OpenStackHttpClient client) {
        this.client = client;
    }
}
