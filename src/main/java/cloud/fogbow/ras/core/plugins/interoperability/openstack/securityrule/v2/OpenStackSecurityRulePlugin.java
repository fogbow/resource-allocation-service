package cloud.fogbow.ras.core.plugins.interoperability.openstack.securityrule.v2;

import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.core.plugins.interoperability.SecurityRulePlugin;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpToFogbowExceptionMapper;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackCloudUtils;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class OpenStackSecurityRulePlugin implements SecurityRulePlugin<OpenStackV3User> {
    private static final Logger LOGGER = Logger.getLogger(OpenStackSecurityRulePlugin.class);

    private String networkV2APIEndpoint;
    private OpenStackHttpClient client;

    public OpenStackSecurityRulePlugin(String confFilePath) throws FatalErrorException {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.networkV2APIEndpoint = properties.getProperty(OpenStackCloudUtils.NETWORK_NEUTRON_URL_KEY) +
                OpenStackConstants.NEUTRON_V2_API_ENDPOINT;
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

        CreateSecurityRuleRequest createSecurityRuleRequest = new CreateSecurityRuleRequest.Builder()
                .securityGroupId(securityGroupId)
                .remoteIpPrefix(cidr)
                .portRangeMin(portFrom)
                .portRangeMax(portTo)
                .direction(direction)
                .etherType(etherType)
                .protocol(protocol)
                .build();

        String endpoint = this.networkV2APIEndpoint + OpenStackConstants.SECURITY_GROUP_RULES_ENDPOINT;
        String response = doPostRequest(endpoint, createSecurityRuleRequest.toJson(), cloudUser);
        createSecurityRuleResponse = CreateSecurityRuleResponse.fromJson(response);

        return createSecurityRuleResponse.getId();
    }

    @Override
    public List<SecurityRuleInstance> getSecurityRules(Order majorOrder, OpenStackV3User cloudUser) throws FogbowException {
        String securityGroupName = retrieveSecurityGroupName(majorOrder);
        String securityGroupId = retrieveSecurityGroupId(securityGroupName, cloudUser);

        String endpoint = this.networkV2APIEndpoint + OpenStackConstants.SECURITY_GROUP_RULES_ENDPOINT +
                "?" + OpenStackConstants.SecurityRule.SECURITY_GROUP_ID_KEY_JSON +
                "=" + securityGroupId;

        String responseStr = doGetRequest(endpoint, cloudUser);
        return getSecurityRulesFromJson(responseStr);
    }

    @Override
    public void deleteSecurityRule(String securityRuleId, OpenStackV3User cloudUser) throws FogbowException {
        String endpoint = this.networkV2APIEndpoint + OpenStackConstants.SECURITY_GROUP_RULES_ENDPOINT + "/" + securityRuleId;
        doDeleteRequest(endpoint, cloudUser);
    }

    protected void doDeleteRequest(String endpoint, OpenStackV3User cloudUser) throws FogbowException {
        try {
            this.client.doDeleteRequest(endpoint, cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
    }

    protected String doPostRequest(String endpoint, String createSecurityRuleBody, OpenStackV3User cloudUser) throws FogbowException {
        String response = null;
        try {
            response = this.client.doPostRequest(endpoint, createSecurityRuleBody, cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
        return response;
    }

    protected String doGetRequest(String endpoint, OpenStackV3User cloudUser) throws FogbowException {
        String response = null;
        try {
            response = this.client.doGetRequest(endpoint, cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
        return response;
    }

    protected String retrieveSecurityGroupName(Order majorOrder) throws InvalidParameterException {
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
                .path(OpenStackConstants.SECURITY_GROUPS_ENDPOINT)
                .queryParam(OpenStackConstants.SecurityRule.NAME_KEY_JSON, securityGroupName)
                .build();

        String endpoint = uri.toString();
        String responseStr = doGetRequest(endpoint, cloudUser);

        GetSecurityGroupsResponse getSecurityGroupsResponse = GetSecurityGroupsResponse.fromJson(responseStr);
        List<GetSecurityGroupsResponse.SecurityGroup> securityGroups = getSecurityGroupsResponse.getSecurityGroups();
        if (securityGroups.size() == 1) {
            return securityGroups.get(0).getId();
        } else {
            throw new FogbowException(String.format(Messages.Exception.MULTIPLE_SECURITY_GROUPS_EQUALLY_NAMED, securityGroupName));
        }
    }

    protected List<SecurityRuleInstance> getSecurityRulesFromJson(String json) throws FogbowException {
        GetSecurityRulesResponse getSecurityGroupResponse = GetSecurityRulesResponse.fromJson(json);
        List<GetSecurityRulesResponse.SecurityRules> securityRules = getSecurityGroupResponse.getSecurityRules();

        List<SecurityRuleInstance> rules = new ArrayList<>();
        for (GetSecurityRulesResponse.SecurityRules secRules : securityRules) {
            SecurityRule.Direction direction = secRules.getDirection().
                    equalsIgnoreCase("ingress") ? SecurityRule.Direction.IN : SecurityRule.Direction.OUT;
            SecurityRule.EtherType etherType = secRules.getEtherType().
                    equals("IPv6") ? SecurityRule.EtherType.IPv6 : SecurityRule.EtherType.IPv4;
            SecurityRule.Protocol protocol = defineRuleProtocol(secRules);

            SecurityRuleInstance securityRuleInstance = new SecurityRuleInstance(secRules.getId(), direction,
                    secRules.getPortFrom(), secRules.getPortTo(), secRules.getCidr(), etherType, protocol);
            rules.add(securityRuleInstance);
        }
        return rules;
    }

    protected SecurityRule.Protocol defineRuleProtocol(GetSecurityRulesResponse.SecurityRules secRules) throws FogbowException {
        SecurityRule.Protocol protocol;
        if (secRules.getProtocol() != null) {
            switch(secRules.getProtocol().toLowerCase()) {
                case "tcp":
                    protocol = SecurityRule.Protocol.TCP;
                    break;
                case "udp":
                    protocol = SecurityRule.Protocol.UDP;
                    break;
                case "icmp":
                    protocol = SecurityRule.Protocol.ICMP;
                    break;
                default:
                    throw new FogbowException(String.format(Messages.Exception.INVALID_PROTOCOL, secRules.getProtocol(),
                            SecurityRule.Protocol.values()));
            }
        } else {
            protocol = SecurityRule.Protocol.ANY;
        }

        return protocol;
    }

    private void initClient() {
        this.client = new OpenStackHttpClient();
    }

    protected void setClient(OpenStackHttpClient client) {
        this.client = client;
    }
}
