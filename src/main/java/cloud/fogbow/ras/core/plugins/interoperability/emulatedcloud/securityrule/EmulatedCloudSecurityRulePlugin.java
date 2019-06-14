package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.securityrule;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.JsonSerializable;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.plugins.interoperability.SecurityRulePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.emulatedmodels.EmulatedOrderWithSecurityRule;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.emulatedmodels.EmulatedPublicIp;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.emulatedmodels.EmulatedSecurityRule;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.emulatedmodels.EmulatedSecurityRuleBinding;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

public class EmulatedCloudSecurityRulePlugin implements SecurityRulePlugin<CloudUser> {

    private Properties properties;

    public EmulatedCloudSecurityRulePlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
    }

    @Override
    public String requestSecurityRule(SecurityRule String, Order majorOrder, CloudUser cloudUser) throws FogbowException {

        EmulatedSecurityRule emulatedSecurityRule = generateJsonEntityToCreateInstance(String);

        String majorOrderInstanceId = majorOrder.getInstanceId();
        this.addSecurityRuleToOrder(majorOrderInstanceId, emulatedSecurityRule);

        String securityRuleInstanceId = emulatedSecurityRule.getId();
        this.bindSecurityRuleToOrder(securityRuleInstanceId, majorOrderInstanceId);

        return securityRuleInstanceId;
    }

    @Override
    public List<SecurityRuleInstance> getSecurityRules(Order majorOrder, CloudUser cloudUser) throws FogbowException {
        String instanceId = majorOrder.getInstanceId();

        List<SecurityRuleInstance> securityRuleInstances = null;
        try {

            EmulatedPublicIp publicIp = this.getPublicIpById(instanceId);
            List<EmulatedSecurityRule> securityRules = publicIp.getSecurityRules();
            securityRuleInstances = EmulatedOrderWithSecurityRule.getFogbowSecurityRules(securityRules);

        } catch (FogbowException e) {

            throw new FogbowException(e.getMessage());
        }

        return securityRuleInstances;
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

    private EmulatedSecurityRule generateJsonEntityToCreateInstance(SecurityRule String) {
        String instanceId = EmulatedCloudUtils.getRandomUUID();
        String cidr = String.getCidr();
        int portFrom = String.getPortFrom();
        int portTo = String.getPortTo();
        String direction = String.getDirection().toString();
        String etherType = String.getEtherType().toString();
        String protocol = String.getProtocol().toString();

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

    private void updateResourceOnDisk(String instanceId, JsonSerializable serializableOrder) throws IOException {
        updateResourceOnDisk(instanceId, serializableOrder, true);
    }

    private void updateResourceOnDisk(String instanceId, JsonSerializable serializableOrder, boolean deletePrevious) throws IOException {
        String contents = serializableOrder.toJson();
        String resourcePath = EmulatedCloudUtils.getResourcePath(this.properties, instanceId);

        if (deletePrevious) {

            EmulatedCloudUtils.deleteFile(resourcePath);
        }

        EmulatedCloudUtils.saveFileContent(resourcePath, contents);
    }

    private void addSecurityRuleToOrder(String instanceId, EmulatedSecurityRule securityRule) throws FogbowException {
        try {
            EmulatedPublicIp publicIp = this.getPublicIpById(instanceId);
            publicIp.addSecurityRule(securityRule);

            updateResourceOnDisk(publicIp.getId(), publicIp);
        } catch (IOException e) {
            throw new FogbowException(e.getMessage());
        }
    }

    private void removeSecurityRuleFromOrder(String majorOrderId, String securityRuleId) throws FogbowException {
        try {
            EmulatedPublicIp publicIp = this.getPublicIpById(majorOrderId);
            publicIp.removeSecurityRule(securityRuleId);

        } catch (Exception e) {
            throw new FogbowException(e.getMessage());
        }
    }

    private void bindSecurityRuleToOrder(String securityRuleInstanceId, String majorOrderInstanceId) throws FogbowException {
        EmulatedSecurityRuleBinding securityRuleBinding = new EmulatedSecurityRuleBinding.Builder()
                .majorOrderId(majorOrderInstanceId)
                .build();

        try {
            updateResourceOnDisk(securityRuleInstanceId, securityRuleBinding);
        } catch (IOException e) {
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
