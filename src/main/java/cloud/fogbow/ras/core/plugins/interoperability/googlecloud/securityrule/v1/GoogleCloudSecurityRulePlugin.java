package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.securityrule.v1;

import cloud.fogbow.common.constants.GoogleCloudConstants;
import cloud.fogbow.common.constants.OpenStackConstants;
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
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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
        // TODO: Get correct prefix endpoint from config file.
        this.prefixEndpoint = "";
        this.client = new GoogleCloudHttpClient();
    }

    @Override
    public String requestSecurityRule(SecurityRule securityRule, Order majorOrder, GoogleCloudUser cloudUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER));

        CreateFirewallRuleRequest request = buildCreateSecurityRuleRequest(securityRule, majorOrder);

        String endpoint = this.prefixEndpoint + OpenStackConstants.SECURITY_GROUP_RULES_ENDPOINT;
        String requestJson = request.toJson();
        String responseJson = this.client.doPostRequest(endpoint, requestJson, cloudUser);

        CreateFirewallRuleResponse response = CreateFirewallRuleResponse.fromJson(responseJson);
        return response.getId();
    }

    public CreateFirewallRuleRequest buildCreateSecurityRuleRequest(SecurityRule securityRule, Order majorOrder, Integer securityRulePriority){
        String name = SystemConstants.PN_SECURITY_GROUP_PREFIX + majorOrder.getInstanceId();
        String network = ((NetworkOrder) majorOrder).getName();
        String cidr = securityRule.getCidr();
        String direction = securityRule.getDirection().toString();
        String portFrom = String.valueOf(securityRule.getPortFrom());
        String portTo = String.valueOf(securityRule.getPortTo());
        String protocol = securityRule.getProtocol().toString();
        int priority = securityRulePriority;

        CreateFirewallRuleRequest request = new CreateFirewallRuleRequest.Builder()
                .name(name)
                .network(network)
                .priority(priority)
                .direction(direction)
                .incomeCidr(cidr)
                .outcomeCidr(cidr)
                .ipProtocol(protocol)
                .ports(portFrom, portTo)
                .build();

        return request;
    }
    private CreateFirewallRuleRequest buildCreateSecurityRuleRequest(SecurityRule securityRule, Order majorOrder){
        return buildCreateSecurityRuleRequest(securityRule, majorOrder,
                GoogleCloudConstants.Network.Firewall.DEFAULT_PRIORITY);
    }

    @Override
    public List<SecurityRuleInstance> getSecurityRules(Order majorOrder, GoogleCloudUser cloudUser) throws FogbowException {
        String endpoint = this.prefixEndpoint
                + GoogleCloudConstants.GLOBAL_FIREWALL_ENDPOINT;
        String responseJson = this.client.doGetRequest(endpoint, cloudUser);
        GetFirewallRulesResponse firewallRuleResponse = GetFirewallRulesResponse.fromJson(responseJson);

        return getSecurityRuleInstances(firewallRuleResponse);
    }

    private List<SecurityRuleInstance> getSecurityRuleInstances(GetFirewallRulesResponse firewallRulesResponse) {
        List<SecurityRuleInstance> securityRuleInstances = new ArrayList<>();
        List<GetFirewallRulesResponse.FirewallRule> firewallRules = firewallRulesResponse.getSecurityGroupRules();

        for(GetFirewallRulesResponse.FirewallRule firewallRule: firewallRules) {
            String id = firewallRule.getId();
            String direction = firewallRule.getDirection();
            String portRange = firewallRule.getPort();
            String cidr = firewallRule.getCidr();
            String etherType = firewallRule.getEtherType();
            String protocol = firewallRule.getIpProtocol();

            SecurityRuleInstance securityRuleInstance = createSecurityRuleInstance(id, direction, portRange, cidr, etherType, protocol);
            securityRuleInstances.add(securityRuleInstance);
        }

        return securityRuleInstances;
    }

    private SecurityRuleInstance createSecurityRuleInstance(String id, String directionString, String portRange, String cidr,
                                                            String etherTypeString, String protocolString) {

        SecurityRule.Direction direction = null;
        if(directionString.equals(SecurityRule.Direction.IN)) {
            direction = SecurityRule.Direction.IN;
        } else {
            direction = SecurityRule.Direction.OUT;
        }

        Integer portFrom = null;
        Integer portTo = null;
        if(portRange.contains("-")) {
            String[] portPair = portRange.split("-");
            portFrom = Integer.valueOf(portPair[0]);
            portFrom = Integer.valueOf(portPair[1]);
        } else {
            portFrom = Integer.valueOf(portRange);
            portTo = Integer.valueOf(portRange);
        }

        SecurityRule.EtherType etherType = null;
        if(etherType.equals(SecurityRule.EtherType.IPv4)) {
            etherType = SecurityRule.EtherType.IPv4;
        } else {
            etherType = SecurityRule.EtherType.IPv4;
        }

        SecurityRule.Protocol protocol = null;
        if(protocol.equals(SecurityRule.Protocol.TCP)) {
            protocol = SecurityRule.Protocol.TCP;
        } else if(protocol.equals(SecurityRule.Protocol.UDP)) {
            protocol = SecurityRule.Protocol.UDP;
        } else if(protocol.equals(SecurityRule.Protocol.ICMP)) {
            protocol = SecurityRule.Protocol.ICMP;
        } else {
            protocol = SecurityRule.Protocol.ANY;
        }

        SecurityRuleInstance securityRuleInstance = new SecurityRuleInstance(id, direction, portFrom, portTo, cidr, etherType, protocol);
        return securityRuleInstance;
    }

    public SecurityRuleInstance getSecurityRule(String securityRuleId, GoogleCloudUser cloudUser) throws FogbowException {
        String endpoint = this.prefixEndpoint
                + GoogleCloudConstants.GLOBAL_FIREWALL_ENDPOINT
                + GoogleCloudConstants.ENDPOINT_SEPARATOR
                + securityRuleId;

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
        String endpoint = this.prefixEndpoint
                + GoogleCloudConstants.GLOBAL_FIREWALL_ENDPOINT
                + GoogleCloudConstants.ENDPOINT_SEPARATOR
                + securityRuleId;
        this.client.doDeleteRequest(endpoint, cloudUser);
    }
}
