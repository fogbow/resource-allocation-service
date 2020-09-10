package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.securityrule;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.plugins.interoperability.SecurityRulePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudConstants;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.securityrule.models.EmulatedSecurityRule;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.securityrule.EmulatedCloudSecurityRuleManager;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class EmulatedCloudSecurityRulePlugin implements SecurityRulePlugin<CloudUser> {

    private static final Logger LOGGER = Logger.getLogger(EmulatedCloudSecurityRulePlugin.class);
    private Properties properties;

    public EmulatedCloudSecurityRulePlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
    }

    @Override
    public String requestSecurityRule(SecurityRule securityRule, Order majorOrder, CloudUser cloudUser) throws FogbowException {
        LOGGER.info(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER);
        EmulatedCloudSecurityRuleManager securityRuleManager = EmulatedCloudSecurityRuleManager.getInstance();

        String securityGroupId = this.getSecurityGroupId(majorOrder);
        EmulatedSecurityRule emulatedSecurityRule = buildEmulatedSecurityRule(securityRule, securityGroupId);
        String instanceId = securityRuleManager.create(emulatedSecurityRule);
        return instanceId;
    }

    @Override
    public List<SecurityRuleInstance> getSecurityRules(Order majorOrder, CloudUser cloudUser) throws FogbowException {
        EmulatedCloudSecurityRuleManager securityRuleManager = EmulatedCloudSecurityRuleManager.getInstance();
        String securityGroupId = this.getSecurityGroupId(majorOrder);
        List<EmulatedSecurityRule> securityRules = securityRuleManager.listBySecurityGroup(securityGroupId);
        List<SecurityRuleInstance> securityRuleInstances = this.getSecurityRuleInstances(securityRules);
        return securityRuleInstances;
    }

    @Override
    public void deleteSecurityRule(String securityRuleId, CloudUser cloudUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Log.DELETING_INSTANCE_S, securityRuleId));
        EmulatedCloudSecurityRuleManager securityRuleManager = EmulatedCloudSecurityRuleManager.getInstance();
        securityRuleManager.delete(securityRuleId);
    }

    private String getSecurityGroupId(Order majorOrder) throws FogbowException {
        String instanceId = majorOrder.getInstanceId();
        switch (majorOrder.getType()) {
            case PUBLIC_IP: return EmulatedCloudUtils.getPublicIpSecurityGroupId(instanceId);
            case NETWORK: return EmulatedCloudUtils.getNetworkSecurityGroupId(instanceId);
            default: throw new FogbowException(Messages.Exception.INVALID_RESOURCE);
        }
    }

    private List<SecurityRuleInstance> getSecurityRuleInstances(List<EmulatedSecurityRule> securityRules) throws FogbowException {
        List<SecurityRuleInstance> securityRuleInstances = new ArrayList<>();

        for (EmulatedSecurityRule emulatedSecurityRule : securityRules) {
            SecurityRuleInstance instance = this.buildSecurityRuleInstance(emulatedSecurityRule);
            securityRuleInstances.add(instance);
        }

        return securityRuleInstances;
    }

    private SecurityRuleInstance buildSecurityRuleInstance(EmulatedSecurityRule emulatedSecurityRule) {
        String id = emulatedSecurityRule.getInstanceId();
        SecurityRule.Direction direction = defineDirection(emulatedSecurityRule.getDirection());
        SecurityRule.EtherType etherType = defineEtherType(emulatedSecurityRule.getEtherType());
        SecurityRule.Protocol protocol = defineProtocol(emulatedSecurityRule.getProtocol());
        String cidr = emulatedSecurityRule.getCidr();
        int portFrom = emulatedSecurityRule.getPortFrom();
        int portTo = emulatedSecurityRule.getPortTo();

        return new SecurityRuleInstance(id, direction, portFrom, portTo, cidr, etherType, protocol);
    }

    private SecurityRule.Protocol defineProtocol(String protocolStr) {
        if (protocolStr != null) {
            for (SecurityRule.Protocol protocol : SecurityRule.Protocol.values()) {
                if (protocolStr.equals(protocol.toString())) {
                    return protocol;
                }
            }
        }
        return SecurityRule.Protocol.ANY;
    }

    private SecurityRule.Direction defineDirection(String direction) {
        return direction.equalsIgnoreCase(EmulatedCloudConstants.Plugins.SecurityRule.INGRESS_DIRECTION) ?
                SecurityRule.Direction.IN : SecurityRule.Direction.OUT;
    }

    private SecurityRule.EtherType defineEtherType(String etherType) {
        return etherType.equalsIgnoreCase(EmulatedCloudConstants.Plugins.SecurityRule.IPV4_ETHER_TYPE) ?
                SecurityRule.EtherType.IPv4 : SecurityRule.EtherType.IPv6;
    }

    private EmulatedSecurityRule buildEmulatedSecurityRule(SecurityRule securityRule, String securityGroupId) {
        String instanceId = EmulatedCloudUtils.getRandomUUID();
        String cidr = securityRule.getCidr();
        int portFrom = securityRule.getPortFrom();
        int portTo = securityRule.getPortTo();
        String direction = securityRule.getDirection().toString();
        String etherType = securityRule.getEtherType().toString();
        String protocol = securityRule.getProtocol().toString();

        return new EmulatedSecurityRule.Builder()
                .instanceId(instanceId)
                .securityGroupId(securityGroupId)
                .cidr(cidr)
                .portFrom(portFrom)
                .portTo(portTo)
                .direction(direction)
                .etherType(etherType)
                .protocol(protocol)
                .build();
    }
}
