package cloud.fogbow.ras.core.plugins.interoperability.openstack.securityrule.v2;

import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.util.CidrUtils;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.api.parameters.SecurityRule.Direction;
import cloud.fogbow.ras.api.parameters.SecurityRule.EtherType;
import cloud.fogbow.ras.api.parameters.SecurityRule.Protocol;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.plugins.interoperability.SecurityRulePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.OpenStackPluginUtils;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.requests.CreateSecurityRuleRequest;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.responses.CreateSecurityRuleResponse;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.responses.GetSecurityRulesResponse;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.responses.GetSecurityRulesResponse.SecurityGroupRule;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.responses.GetSecurityGroupsResponse;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.responses.GetSecurityGroupsResponse.SecurityGroup;
import com.google.common.annotations.VisibleForTesting;
import org.apache.log4j.Logger;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class OpenStackSecurityRulePlugin implements SecurityRulePlugin<OpenStackV3User> {

    private static final Logger LOGGER = Logger.getLogger(OpenStackSecurityRulePlugin.class);
    private static final int MAXIMUM_SIZE_ALLOWED = 1;

    public static final int MAXIMUM_PORT_RANGE = 65535;
    public static final int MINIMUM_PORT_RANGE = 0;

    private String prefixEndpoint;
    private OpenStackHttpClient client;

    public OpenStackSecurityRulePlugin(String confFilePath) throws FatalErrorException {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.prefixEndpoint = properties.getProperty(OpenStackPluginUtils.NETWORK_NEUTRON_URL_KEY)
                + OpenStackConstants.NEUTRON_V2_API_ENDPOINT;

        initClient();
    }

    @Override
    public String requestSecurityRule(SecurityRule securityRule, Order majorOrder, OpenStackV3User cloudUser)
            throws FogbowException {

        LOGGER.info(String.format(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER));
        String securityGroupName = retrieveSecurityGroupName(majorOrder);
        String securityGroupId = retrieveSecurityGroupId(securityGroupName, cloudUser);
        CreateSecurityRuleRequest request = buildCreateSecurityRuleRequest(securityGroupId, securityRule);
        return doRequestSecurityRule(request, cloudUser);
    }

    @Override
    public List<SecurityRuleInstance> getSecurityRules(Order majorOrder, OpenStackV3User cloudUser)
            throws FogbowException {

        LOGGER.info(String.format(Messages.Log.GETTING_INSTANCE_S, majorOrder.getInstanceId()));
        String securityGroupName = retrieveSecurityGroupName(majorOrder);
        String securityGroupId = retrieveSecurityGroupId(securityGroupName, cloudUser);
        GetSecurityRulesResponse response = doGetSecurityRules(securityGroupId, cloudUser);
        return getSecurityRuleInstances(response);
    }

    @Override
    public void deleteSecurityRule(String securityRuleId, OpenStackV3User cloudUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Log.DELETING_INSTANCE_S, securityRuleId));
        String endpoint = this.prefixEndpoint
                + OpenStackConstants.SECURITY_GROUP_RULES_ENDPOINT
                + OpenStackConstants.ENDPOINT_SEPARATOR
                + securityRuleId;

        doDeleteRequest(endpoint, cloudUser);
    }

    @VisibleForTesting
    void doDeleteRequest(String endpoint, OpenStackV3User cloudUser) throws FogbowException {
        this.client.doDeleteRequest(endpoint, cloudUser);
    }

    @VisibleForTesting
    String doPostRequest(String endpoint, String bodyContent, OpenStackV3User cloudUser) throws FogbowException {
        String response = this.client.doPostRequest(endpoint, bodyContent, cloudUser);
        return response;
    }

    @VisibleForTesting
    String doGetRequest(String endpoint, OpenStackV3User cloudUser) throws FogbowException {
        String response = this.client.doGetRequest(endpoint, cloudUser);
        return response;
    }

    @VisibleForTesting
    List<SecurityRuleInstance> getSecurityRuleInstances(GetSecurityRulesResponse response)
            throws FogbowException {

        List<SecurityRuleInstance> securityRuleInstances = new ArrayList<>();
        List<SecurityGroupRule> securityGroupRules = response.getSecurityGroupRules();
        for (GetSecurityRulesResponse.SecurityGroupRule securityGroupRule : securityGroupRules) {
            SecurityRuleInstance securityRuleInstance = buildSecurityRuleInstance(securityGroupRule);
            securityRuleInstances.add(securityRuleInstance);
        }
        return securityRuleInstances;
    }

    @VisibleForTesting
    GetSecurityRulesResponse doGetSecurityRules(String securityGroupId, OpenStackV3User cloudUser)
            throws FogbowException {

        String endpoint = buildQueryEndpointBySecurityGroupId(securityGroupId);
        String responseJson = doGetRequest(endpoint, cloudUser);
        return GetSecurityRulesResponse.fromJson(responseJson);
    }

    @VisibleForTesting
    String buildQueryEndpointBySecurityGroupId(String securityGroupId) {
        String fieldName = OpenStackConstants.SecurityRule.SECURITY_GROUP_ID_KEY_JSON;

        URI uri = UriBuilder
                .fromPath(this.prefixEndpoint)
                .path(OpenStackConstants.SECURITY_GROUP_RULES_ENDPOINT)
                .queryParam(fieldName, securityGroupId)
                .build();

        return uri.toString();
    }

    @VisibleForTesting
    String doRequestSecurityRule(CreateSecurityRuleRequest request, OpenStackV3User cloudUser)
            throws FogbowException {

        String endpoint = this.prefixEndpoint + OpenStackConstants.SECURITY_GROUP_RULES_ENDPOINT;
        String requestJson = request.toJson();
        String responseJson = doPostRequest(endpoint, requestJson, cloudUser);

        CreateSecurityRuleResponse response = CreateSecurityRuleResponse.fromJson(responseJson);
        return response.getId();
    }

    @VisibleForTesting
    CreateSecurityRuleRequest buildCreateSecurityRuleRequest(String securityGroupId, SecurityRule securityRule) {
        String cidr = securityRule.getCidr();
        String direction = securityRule.getDirection().toString();
        String etherType = securityRule.getEtherType().toString();
        int portFrom = securityRule.getPortFrom();
        int portTo = securityRule.getPortTo();
        String protocol = securityRule.getProtocol().toString();

        CreateSecurityRuleRequest request = new CreateSecurityRuleRequest.Builder()
                .securityGroupId(securityGroupId)
                .remoteIpPrefix(cidr)
                .portRangeMin(portFrom)
                .portRangeMax(portTo)
                .direction(direction)
                .etherType(etherType)
                .protocol(protocol)
                .build();

        return request;
    }

    @VisibleForTesting
    String retrieveSecurityGroupName(Order majorOrder) throws FogbowException {
        String securityGroupName;
        switch (majorOrder.getType()) {
            case NETWORK:
                securityGroupName = SystemConstants.PN_SECURITY_GROUP_PREFIX + majorOrder.getInstanceId();
                break;
            case PUBLIC_IP:
                securityGroupName = SystemConstants.PIP_SECURITY_GROUP_PREFIX + majorOrder.getInstanceId();
                break;
            default:
                String message = String.format(Messages.Exception.INVALID_PARAMETER_S, majorOrder.getType());
                throw new InvalidParameterException(message);
        }
        return securityGroupName;
    }

    @VisibleForTesting
    String retrieveSecurityGroupId(String securityGroupName, OpenStackV3User cloudUser) throws FogbowException {
        String endpoint = buildQueryEndpointBySecurityGroupName(securityGroupName);
        String responseJson = doGetRequest(endpoint, cloudUser);
        GetSecurityGroupsResponse response = GetSecurityGroupsResponse.fromJson(responseJson);
        return getSecurityGroupId(response, securityGroupName);
    }

    @VisibleForTesting
    String getSecurityGroupId(GetSecurityGroupsResponse response, String securityGroupName)
            throws FogbowException {

        List<SecurityGroup> securityGroups = response.getSecurityGroups();
        checkSecurityGroupsListIntegrity(securityGroups, securityGroupName);
        SecurityGroup securityGroup = securityGroups.listIterator().next();
        return securityGroup.getId();
    }

    @VisibleForTesting
    void checkSecurityGroupsListIntegrity(List<SecurityGroup> securityGroups, String securityGroupName)
            throws FogbowException {

        if (securityGroups.size() < MAXIMUM_SIZE_ALLOWED) {
            String message = String.format(Messages.Exception.SECURITY_GROUP_EQUALLY_NAMED_S_NOT_FOUND_S, securityGroupName);
            throw new InstanceNotFoundException(message);
        } else if (securityGroups.size() > MAXIMUM_SIZE_ALLOWED) {
            String message = String.format(Messages.Exception.MULTIPLE_SECURITY_GROUPS_EQUALLY_NAMED_S, securityGroupName);
            throw new InternalServerErrorException(message);
        }
    }

    @VisibleForTesting
    String buildQueryEndpointBySecurityGroupName(String securityGroupName) {
        URI uri = UriBuilder
                .fromPath(this.prefixEndpoint)
                .path(OpenStackConstants.SECURITY_GROUPS_ENDPOINT)
                .queryParam(OpenStackConstants.SecurityRule.NAME_KEY_JSON, securityGroupName)
                .build();

        return uri.toString();
    }

    @VisibleForTesting
    SecurityRuleInstance buildSecurityRuleInstance(SecurityGroupRule securityGroupRule)
            throws FogbowException {

        String id = securityGroupRule.getId();
        Direction direction = defineDirection(securityGroupRule);
        Integer portFrom = definePortFrom(securityGroupRule);
        Integer portTo = definePortTo(securityGroupRule);
        String cidr = defineCIDR(securityGroupRule);
        EtherType etherType = defineEtherType(securityGroupRule);
        Protocol protocol = defineProtocol(securityGroupRule);

        return new SecurityRuleInstance(id, direction, portFrom, portTo, cidr, etherType, protocol);
    }

    @VisibleForTesting
    Protocol defineProtocol(SecurityGroupRule securityGroupRule) throws FogbowException {
        String protocolStr = securityGroupRule.getProtocol();
        if (protocolStr != null) {
            for (Protocol protocol : Protocol.values()) {
                if (protocolStr.equals(protocol.toString())) {
                    return protocol;
                }
            }
        }
        return Protocol.ANY;
    }

    @VisibleForTesting
    String defineCIDR(SecurityGroupRule securityGroupRule) {
        String cidr = securityGroupRule.getCidr();
        String etherTypeStr = securityGroupRule.getEtherType();
        if (cidr == null) {
            cidr = etherTypeStr.equals(OpenStackConstants.IPV4_ETHER_TYPE)
                    ? CidrUtils.DEFAULT_IPV4_CIDR
                    : CidrUtils.DEFAULT_IPV6_CIDR;
        }
        return cidr;
    }

    @VisibleForTesting
    EtherType defineEtherType(SecurityGroupRule securityGroupRule) {
        String etherTypeStr = securityGroupRule.getEtherType();
        return etherTypeStr.equals(OpenStackConstants.IPV4_ETHER_TYPE)
                ? EtherType.IPv4
                : EtherType.IPv6;
    }

    @VisibleForTesting
    Integer definePortTo(SecurityGroupRule securityGroupRule) {
        Integer portTo = securityGroupRule.getPortTo();
        return (portTo != null)
                ? portTo
                : MAXIMUM_PORT_RANGE;
    }

    @VisibleForTesting
    Integer definePortFrom(SecurityGroupRule securityGroupRule) {
        Integer portFrom = securityGroupRule.getPortFrom();
        return (portFrom != null)
                ? portFrom
                : MINIMUM_PORT_RANGE;
    }

    @VisibleForTesting
    Direction defineDirection(SecurityGroupRule securityGroupRule) {
        String directionStr = securityGroupRule.getDirection();
        return directionStr.equalsIgnoreCase(OpenStackConstants.INGRESS_DIRECTION)
                ? Direction.IN
                : Direction.OUT;
    }

    @VisibleForTesting
    void setClient(OpenStackHttpClient client) {
        this.client = client;
    }

    private void initClient() {
        this.client = new OpenStackHttpClient();
    }

}
