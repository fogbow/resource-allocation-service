package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.securityrule;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.plugins.interoperability.SecurityRulePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudUtils;

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

        String securityRuleInstanceId = emulatedSecurityRule.getId();

        String orderToAttachRule = majorOrder.getInstanceId();

        try {
            EmulatedCloudUtils.saveFileContent(securityRulePath, securityRuleJson);
        } catch (IOException e) {
            throw new FogbowException(e.getMessage());
        }

        return securityRuleInstanceId;
    }

    @Override
    public List<SecurityRuleInstance> getSecurityRules(Order majorOrder, CloudUser cloudUser) throws FogbowException {
        return null;
    }

    @Override
    public void deleteSecurityRule(String StringId, CloudUser cloudUser) throws FogbowException {

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
}
