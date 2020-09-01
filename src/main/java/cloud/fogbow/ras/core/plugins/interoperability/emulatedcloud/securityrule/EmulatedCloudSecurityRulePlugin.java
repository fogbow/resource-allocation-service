package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.securityrule;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.JsonSerializable;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.plugins.interoperability.SecurityRulePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudConstants;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.emulatedmodels.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class EmulatedCloudSecurityRulePlugin implements SecurityRulePlugin<CloudUser> {

    private Properties properties;

    public EmulatedCloudSecurityRulePlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
    }

    @Override
    public String requestSecurityRule(SecurityRule securityRule, Order majorOrder, CloudUser cloudUser) throws FogbowException {
        String securityGroupId = this.getSecurityGroupId(majorOrder);
        EmulatedSecurityRule emulatedSecurityRule = buildEmulatedSecurityRule(securityRule, securityGroupId);
        EmulatedSecurityGroup securityGroup = this.getSecurityGroup(securityGroupId);
        this.doRequestSecurityRule(emulatedSecurityRule, securityGroup);
        String instanceId = emulatedSecurityRule.getId();
        return instanceId;
    }

    @Override
    public List<SecurityRuleInstance> getSecurityRules(Order majorOrder, CloudUser cloudUser) throws FogbowException {
        String securityGroupId = this.getSecurityGroupId(majorOrder);
        EmulatedSecurityGroup securityGroup = this.getSecurityGroup(securityGroupId);
        List<SecurityRuleInstance> securityRuleInstances = this.getSecurityRuleInstances(securityGroup);
        return securityRuleInstances;
    }

    @Override
    public void deleteSecurityRule(String securityRuleId, CloudUser cloudUser) throws FogbowException {
        EmulatedSecurityRule securityRule = getSecurityRule(securityRuleId);
        String securityGroupId = securityRule.getSecurityGroupId();
        this.doDeleteSecurityRule(securityRuleId);
        this.removeFromSecurityGroup(securityGroupId, securityRuleId);
    }

    private void doRequestSecurityRule(EmulatedSecurityRule securityRule, EmulatedSecurityGroup securityGroup) throws FogbowException {
        String securityRuleId = securityRule.getId();
        String securityGroupId = securityGroup.getId();

        String securityGroupPath = EmulatedCloudUtils.getResourcePath(properties, securityGroupId);
        String securityRulePath = EmulatedCloudUtils.getResourcePath(properties, securityRuleId);

        try {
            EmulatedCloudUtils.saveFileContent(securityRulePath, securityRule.toJson());
            securityGroup.addSecurityRule(securityRuleId);
            EmulatedCloudUtils.saveFileContent(securityGroupPath, securityGroup.toJson());
        } catch (IOException e) {
            throw new FogbowException(e.getMessage());
        }
    }

    private EmulatedSecurityGroup getSecurityGroup(String id) throws FogbowException {
        try {
            String json = EmulatedCloudUtils.getFileContentById(properties, id);
            return EmulatedSecurityGroup.fromJson(json);
        } catch (IOException e) {
            throw new FogbowException(e.getMessage());
        }
    }

    private String getSecurityGroupId(Order majorOrder) throws FogbowException {
        String instanceId = majorOrder.getInstanceId();
        switch (majorOrder.getType()) {
            case PUBLIC_IP: return EmulatedCloudUtils.getPublicIpSecurityGroupId(instanceId);
            case NETWORK: return EmulatedCloudUtils.getNetworkSecurityGroupId(instanceId);
            default: throw new FogbowException(Messages.Exception.INVALID_RESOURCE);
        }
    }

    private List<SecurityRuleInstance> getSecurityRuleInstances(EmulatedSecurityGroup securityGroup) throws FogbowException {
        List<SecurityRuleInstance> securityRuleInstances = new ArrayList<>();

        for (String securityRuleId: securityGroup.getSecurityRules()) {
            EmulatedSecurityRule emulatedSecurityRule = this.getSecurityRule(securityRuleId);
            SecurityRuleInstance instance = this.buildSecurityRuleInstance(emulatedSecurityRule);
            securityRuleInstances.add(instance);
        }

        return securityRuleInstances;
    }

    private EmulatedSecurityRule getSecurityRule(String id) throws FogbowException {
        try {
            String json = EmulatedCloudUtils.getFileContentById(properties, id);
            return EmulatedSecurityRule.fromJson(json);
        } catch (IOException e) {
            throw new FogbowException(e.getMessage());
        }
    }

    private SecurityRuleInstance buildSecurityRuleInstance(EmulatedSecurityRule emulatedSecurityRule) {
        String id = emulatedSecurityRule.getId();
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

    private void removeFromSecurityGroup(String securityGroupId, String securityRuleId) throws FogbowException {
        try {
            String content = EmulatedCloudUtils.getFileContentById(properties, securityGroupId);
            EmulatedSecurityGroup securityGroup = EmulatedSecurityGroup.fromJson(content);
            securityGroup.removeSecurityRule(securityRuleId);

            String path = EmulatedCloudUtils.getResourcePath(properties, securityGroupId);
            EmulatedCloudUtils.saveFileContent(path, securityGroup.toJson());
        } catch (IOException e) {
            throw new FogbowException(e.getMessage());
        }
    }

    private void doDeleteSecurityRule(String id) throws FogbowException {
        String path = EmulatedCloudUtils.getResourcePath(this.properties, id);

        try {
            EmulatedCloudUtils.deleteFile(path);
        } catch (Exception e) {
            throw new FogbowException(e.getMessage());
        }
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
                .id(instanceId)
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
