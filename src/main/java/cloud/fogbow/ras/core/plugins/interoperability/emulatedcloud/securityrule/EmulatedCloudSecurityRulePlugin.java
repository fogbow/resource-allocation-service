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
        EmulatedSecurityRule emulatedSecurityRule = buildEmulatedSecurityRule(securityRule);

        String instanceId = emulatedSecurityRule.getId();
        String securityGroupId = this.getSecurityGroupId(majorOrder);
        EmulatedSecurityGroup securityGroup = this.getSecurityGroup(securityGroupId);
        this.doRequestSecurityRule(emulatedSecurityRule, securityGroup);
        return instanceId;
    }

    private void doRequestSecurityRule(EmulatedSecurityRule securityRule, EmulatedSecurityGroup securityGroup) throws FogbowException {
        securityGroup.addSecurityRule(securityRule);

        String securityGroupId = securityGroup.getId();
        String path = EmulatedCloudUtils.getResourcePath(properties, securityGroupId);

        try {
            EmulatedCloudUtils.saveFileContent(path, securityGroup.toJson());
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

    @Override
    public List<SecurityRuleInstance> getSecurityRules(Order majorOrder, CloudUser cloudUser) throws FogbowException {
        String securityGroupId = this.getSecurityGroupId(majorOrder);
        EmulatedSecurityGroup securityGroup = this.getSecurityGroup(securityGroupId);
        List<SecurityRuleInstance> securityRuleInstances = this.getSecurityRuleInstances(securityGroup);
        return securityRuleInstances;
    }

    private List<SecurityRuleInstance> getSecurityRuleInstances(EmulatedSecurityGroup securityGroup) {
        List<SecurityRuleInstance> securityRuleInstances = new ArrayList<>();

        for (EmulatedSecurityRule emulatedSecurityRule: securityGroup.getSecurityRules()) {
            SecurityRuleInstance instance = this.buildSecurityRuleInstance(emulatedSecurityRule);
            securityRuleInstances.add(instance);
        }

        return securityRuleInstances;
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

    @Override
    public void deleteSecurityRule(String securityRuleId, CloudUser cloudUser) throws FogbowException {
        String majorOrderId = getMajorOrderIdFromSecurityRule(securityRuleId);
        this.removeSecurityRuleFromOrder(majorOrderId, securityRuleId);

        String securityRulePath = EmulatedCloudUtils.getResourcePath(this.properties, securityRuleId);
        try {
            EmulatedCloudUtils.deleteFile(securityRulePath);
        } catch (Exception e) {
            throw new FogbowException(e.getMessage());
        }
    }

    private EmulatedSecurityRule buildEmulatedSecurityRule(SecurityRule securityRule) {
        String instanceId = EmulatedCloudUtils.getRandomUUID();
        String cidr = securityRule.getCidr();
        int portFrom = securityRule.getPortFrom();
        int portTo = securityRule.getPortTo();
        String direction = securityRule.getDirection().toString();
        String etherType = securityRule.getEtherType().toString();
        String protocol = securityRule.getProtocol().toString();

        return new EmulatedSecurityRule.Builder()
                .id(instanceId)
                .cidr(cidr)
                .portFrom(portFrom)
                .portTo(portTo)
                .direction(direction)
                .etherType(etherType)
                .protocol(protocol)
                .build();
    }

    private EmulatedPublicIp getPublicIpById(String instanceId) throws FogbowException {
        String publicIpPath = EmulatedCloudUtils.getResourcePath(this.properties, instanceId);
        String publicIpJson = null;

        try {
            publicIpJson = EmulatedCloudUtils.getFileContent(publicIpPath);
        } catch (IOException e) {
            throw new FogbowException(e.getMessage());
        }

        EmulatedPublicIp publicIp = EmulatedPublicIp.fromJson(publicIpJson);

        return publicIp;
    }

    private void removeSecurityRuleFromOrder(String majorOrderId, String securityRuleId) throws FogbowException {
        try {
            EmulatedPublicIp publicIp = this.getPublicIpById(majorOrderId);
            publicIp.removeSecurityRule(securityRuleId);

        } catch (Exception e) {
            throw new FogbowException(e.getMessage());
        }
    }

    private EmulatedSecurityRuleBinding getEmulatedSecurityRuleBindingById(String securityRuleId) throws FogbowException {
        String securityRulePath = EmulatedCloudUtils.getResourcePath(this.properties, securityRuleId);

        String securityRuleJson = null;
        EmulatedSecurityRuleBinding emulatedSecurityRuleBinding = null;

        try {

            securityRuleJson = EmulatedCloudUtils.getFileContent(securityRulePath);
            emulatedSecurityRuleBinding = EmulatedSecurityRuleBinding.fromJson(securityRuleJson);
        } catch (IOException e) {

            throw new FogbowException(e.getMessage());
        }

        return emulatedSecurityRuleBinding;
    }

    private String getMajorOrderIdFromSecurityRule(String securityRuleId) throws FogbowException {
        EmulatedSecurityRuleBinding emulatedSecurityRuleBinding = getEmulatedSecurityRuleBindingById(securityRuleId);

        return emulatedSecurityRuleBinding.getMajorOrderId();
    }
}
