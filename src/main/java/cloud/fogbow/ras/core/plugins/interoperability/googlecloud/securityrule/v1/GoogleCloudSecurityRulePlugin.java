package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.securityrule.v1;

import cloud.fogbow.as.core.models.GoogleCloudSystemUser;
import cloud.fogbow.common.constants.GoogleCloudConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.GoogleCloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.googlecloud.GoogleCloudHttpClient;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.plugins.interoperability.SecurityRulePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.securityrule.models.CreateFirewallRuleRequest;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.securityrule.models.CreateFirewallRuleResponse;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.securityrule.models.GetFirewallRuleResponse;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.securityrule.models.GetFirewallRulesResponse;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.util.GoogleCloudPluginUtils;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public class GoogleCloudSecurityRulePlugin implements SecurityRulePlugin<GoogleCloudUser> {

    private static final Logger LOGGER = Logger.getLogger(GoogleCloudSecurityRulePlugin.class);

    private String prefixEndpoint;
    private GoogleCloudHttpClient client;

    private static class NetworkProtocol {
        private static final String TCP = SecurityRule.Protocol.TCP.toString();
        private static final String UDP = SecurityRule.Protocol.UDP.toString();
        private static final String ICMP = SecurityRule.Protocol.ICMP.toString();
        private static final String ANY = SecurityRule.Protocol.ANY.toString();
    }

    public GoogleCloudSecurityRulePlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.prefixEndpoint = properties.getProperty(GoogleCloudPluginUtils.NETWORK_URL_KEY)
                + GoogleCloudConstants.COMPUTE_ENGINE_V1_ENDPOINT;
        this.client = new GoogleCloudHttpClient();
    }

    @Override
    public String requestSecurityRule(SecurityRule securityRule, Order majorOrder, GoogleCloudUser cloudUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER));

        String securityRuleName = SystemConstants.PN_SECURITY_GROUP_PREFIX
                + ((NetworkOrder) majorOrder).getName()
                + GoogleCloudConstants.ELEMENT_SEPARATOR
                + UUID.randomUUID().toString();

        CreateFirewallRuleRequest request = buildCreateSecurityRuleRequest(securityRule, majorOrder, securityRuleName);

        String endpoint = this.prefixEndpoint
                            + GoogleCloudConstants.PROJECT_ENDPOINT
                            + GoogleCloudConstants.ENDPOINT_SEPARATOR
                            + ((GoogleCloudSystemUser) majorOrder.getSystemUser()).getProjectId()
                            + GoogleCloudConstants.GLOBAL_FIREWALL_ENDPOINT;



        String requestJson = request.toJson();
        String responseJson = this.client.doPostRequest(endpoint, requestJson, cloudUser);

        CreateFirewallRuleResponse response = CreateFirewallRuleResponse.fromJson(responseJson);
        return securityRuleName;
    }

    public CreateFirewallRuleRequest buildCreateSecurityRuleRequest(SecurityRule securityRule, Order majorOrder, String securityRuleName) {
        return buildCreateSecurityRuleRequest(securityRule, majorOrder, GoogleCloudConstants.Network.Firewall.DEFAULT_RULE_PRIORITY, securityRuleName);
    }

    public CreateFirewallRuleRequest buildCreateSecurityRuleRequest(SecurityRule securityRule, Order majorOrder, int securityRulePriority, String securityRuleName) {
        String networkName = ((NetworkOrder) majorOrder).getName();

        String network = GoogleCloudConstants.Network.GLOBAL_NETWORKS_PATH
                + GoogleCloudConstants.ENDPOINT_SEPARATOR
                + networkName;
        String cidr = securityRule.getCidr();
        String direction = securityRule.getDirection().toString();
        String portFrom = String.valueOf(securityRule.getPortFrom());
        String portTo = String.valueOf(securityRule.getPortTo());
        String protocol = securityRule.getProtocol().toString();
        int priority = securityRulePriority;

        CreateFirewallRuleRequest request = new CreateFirewallRuleRequest.Builder()
                .name(securityRuleName)
                .network(network)
                .priority(priority)
                .direction(direction)
                .incomeCidr(cidr)
                .outcomeCidr(cidr)
                .connection(portFrom, portTo, protocol)
                .build();

        return request;
    }

    @Override
    public List<SecurityRuleInstance> getSecurityRules(Order majorOrder, GoogleCloudUser cloudUser) throws FogbowException {
        String endpoint = this.prefixEndpoint
                + GoogleCloudConstants.PROJECT_ENDPOINT
                + GoogleCloudConstants.ENDPOINT_SEPARATOR
                + cloudUser.getProjectId()
                + GoogleCloudConstants.GLOBAL_FIREWALL_ENDPOINT;
        String responseJson = this.client.doGetRequest(endpoint, cloudUser);

        String networkName = ((NetworkOrder) majorOrder).getName();

        GetFirewallRulesResponse firewallRuleResponse = GetFirewallRulesResponse.fromJson(responseJson);
        if(firewallRuleResponse.getSecurityGroupRules() == null) {
            LOGGER.debug(String.format(Messages.Log.CONTENT_SECURITY_GROUP_EMPTY));
            return new ArrayList<SecurityRuleInstance>();
        }

        return getSecurityRuleInstances(firewallRuleResponse, networkName);
    }

    private List<SecurityRuleInstance> getSecurityRuleInstances(GetFirewallRulesResponse firewallRulesResponse, String networkName) {
        List<SecurityRuleInstance> securityRuleInstances = new ArrayList<>();
        List<GetFirewallRulesResponse.FirewallRule> firewallRules = firewallRulesResponse.getSecurityGroupRules();

        for(GetFirewallRulesResponse.FirewallRule firewallRule: firewallRules) {
            String id = firewallRule.getName();

            String firewallName = id.replaceAll(SystemConstants.PN_SECURITY_GROUP_PREFIX, "");
            String actualNetworkName = firewallName.split(GoogleCloudConstants.ELEMENT_SEPARATOR)[0];

            if(actualNetworkName.equalsIgnoreCase(networkName)) {
                String direction = firewallRule.getDirection();
                String portRange = firewallRule.getPort();
                String cidr = firewallRule.getCidr();
                String etherType = firewallRule.getEtherType();
                String protocol = firewallRule.getIpProtocol();

                SecurityRuleInstance securityRuleInstance = createSecurityRuleInstance(id, direction, portRange, cidr, etherType, protocol);
                securityRuleInstances.add(securityRuleInstance);
            }
        }

        return securityRuleInstances;
    }

    private SecurityRuleInstance createSecurityRuleInstance(String id, String directionString, String portRange, String cidr,
                                                            String etherTypeString, String protocolString) {

        SecurityRule.Direction direction = null;
        if(directionString.equalsIgnoreCase(SecurityRule.Direction.IN.toString())) {
            direction = SecurityRule.Direction.IN;
        } else {
            direction = SecurityRule.Direction.OUT;
        }

        Integer portFrom = null;
        Integer portTo = null;
        if(portRange.contains(GoogleCloudConstants.ELEMENT_SEPARATOR)) {
            String[] portPair = portRange.split(GoogleCloudConstants.ELEMENT_SEPARATOR);
            portFrom = Integer.valueOf(portPair[0]);
            portTo = Integer.valueOf(portPair[1]);
        } else {
            portFrom = Integer.valueOf(portRange);
            portTo = Integer.valueOf(portRange);
        }

        SecurityRule.EtherType etherType = null;
        if(etherTypeString.equalsIgnoreCase(SecurityRule.EtherType.IPv4.toString())) {
            etherType = SecurityRule.EtherType.IPv4;
        } else {
            etherType = SecurityRule.EtherType.IPv4;
        }

        SecurityRule.Protocol protocol = null;
        if(protocolString.equalsIgnoreCase(SecurityRule.Protocol.TCP.toString())) {
            protocol = SecurityRule.Protocol.TCP;
        } else if(protocolString.equalsIgnoreCase(SecurityRule.Protocol.UDP.toString())) {
            protocol = SecurityRule.Protocol.UDP;
        } else if(protocolString.equalsIgnoreCase(SecurityRule.Protocol.ICMP.toString())) {
            protocol = SecurityRule.Protocol.ICMP;
        } else {
            protocol = SecurityRule.Protocol.ANY;
        }

        SecurityRuleInstance securityRuleInstance = new SecurityRuleInstance(id, direction, portFrom, portTo, cidr, etherType, protocol);
        return securityRuleInstance;
    }

    public SecurityRuleInstance getSecurityRule(String securityRuleName, GoogleCloudUser cloudUser) throws FogbowException {
        String endpoint = this.prefixEndpoint
                + GoogleCloudConstants.PROJECT_ENDPOINT
                + GoogleCloudConstants.ENDPOINT_SEPARATOR
                + cloudUser.getProjectId()
                + GoogleCloudConstants.GLOBAL_FIREWALL_ENDPOINT
                + GoogleCloudConstants.ENDPOINT_SEPARATOR
                + securityRuleName;

        String responseJson = this.client.doGetRequest(endpoint, cloudUser);

        GetFirewallRuleResponse firewallRuleResponse = GetFirewallRuleResponse.fromJson(responseJson);

        String id = firewallRuleResponse.getId();
        String direction = firewallRuleResponse.getDirection();
        String portRange = firewallRuleResponse.getPort();
        String cidr = firewallRuleResponse.getCidr();
        String etherType = firewallRuleResponse.getEtherType();
        String protocol = firewallRuleResponse.getIpProtocol();

        SecurityRuleInstance securityRuleInstance = createSecurityRuleInstance(id, direction, portRange, cidr, etherType, protocol);

        return securityRuleInstance;
    }

    @Override
    public void deleteSecurityRule(String securityRuleId, GoogleCloudUser cloudUser) throws FogbowException {
        SecurityRuleInstance securityRuleInstance = getSecurityRule(securityRuleId, cloudUser);
        String endpoint = this.prefixEndpoint
                + GoogleCloudConstants.PROJECT_ENDPOINT
                + GoogleCloudConstants.ENDPOINT_SEPARATOR
                + cloudUser.getProjectId()
                + GoogleCloudConstants.GLOBAL_FIREWALL_ENDPOINT
                + GoogleCloudConstants.ENDPOINT_SEPARATOR
                + securityRuleId;
        this.client.doDeleteRequest(endpoint, cloudUser);
    }
}
