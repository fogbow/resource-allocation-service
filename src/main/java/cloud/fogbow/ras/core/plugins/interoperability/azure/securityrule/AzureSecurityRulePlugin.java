package cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.plugins.interoperability.SecurityRulePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.sdk.AzureNetworkSecurityGroupOperationSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.sdk.model.AzureUpdateNetworkSecurityGroupRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.util.SecurityRuleIdContext;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureGeneralUtil;
import com.google.common.annotations.VisibleForTesting;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Properties;

public class AzureSecurityRulePlugin implements SecurityRulePlugin<AzureUser> {

    private static final Logger LOGGER = Logger.getLogger(AzureSecurityRulePlugin.class);

    private final String defaultResourceGroupName;
    private AzureNetworkSecurityGroupOperationSDK operation;

    public AzureSecurityRulePlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.defaultResourceGroupName = properties.getProperty(AzureConstants.DEFAULT_RESOURCE_GROUP_NAME_KEY);
        this.operation = new AzureNetworkSecurityGroupOperationSDK(this.defaultResourceGroupName);
    }

    @Override
    public String requestSecurityRule(SecurityRule securityRule, Order majorOrder, AzureUser azureUser)
            throws FogbowException {

        LOGGER.info(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER);
        String networkSecurityGroupName = AzureGeneralUtil.defineResourceName(majorOrder.getInstanceId());
        String cidr = securityRule.getCidr();
        int portFrom = securityRule.getPortFrom();
        int portTo = securityRule.getPortTo();
        SecurityRule.Direction direction = securityRule.getDirection();
        SecurityRule.Protocol protocol = securityRule.getProtocol();
        String ruleResourceName = AzureGeneralUtil.generateResourceName();

        AzureUpdateNetworkSecurityGroupRef networkSecurityGroupRef = AzureUpdateNetworkSecurityGroupRef.builder()
                .ruleResourceName(ruleResourceName)
                .securityGroupResourceName(networkSecurityGroupName)
                .protocol(protocol)
                .cidr(cidr)
                .direction(direction)
                .portFrom(portFrom)
                .portTo(portTo)
                .checkAndBuild();

        this.operation.doCreateInstance(networkSecurityGroupRef, azureUser);
        return SecurityRuleIdContext.buildInstanceId(networkSecurityGroupName, ruleResourceName);
    }

    @Override
    public List<SecurityRuleInstance> getSecurityRules(Order majorOrder, AzureUser azureUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Log.GETTING_INSTANCE_S, majorOrder.getInstanceId()));
        String instanceId = majorOrder.getInstanceId();
        String networkSecurityGroupName = AzureGeneralUtil.defineResourceName(instanceId);
        return this.operation.getNetworkSecurityRules(networkSecurityGroupName, azureUser);
    }

    @Override
    public void deleteSecurityRule(String securityRuleId, AzureUser azureUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Log.DELETING_INSTANCE_S, securityRuleId));
        SecurityRuleIdContext securityRuleIdContext = getSecurityRuleIdContext(securityRuleId);
        String networkSecurityGroupName = securityRuleIdContext.getNetworkSecurityGroupName();
        String securityRuleName = securityRuleIdContext.getSecurityRuleName();
        this.operation.deleteNetworkSecurityRule(networkSecurityGroupName, securityRuleName, azureUser);
    }

    @VisibleForTesting
    SecurityRuleIdContext getSecurityRuleIdContext(String securityRuleId) {
        return new SecurityRuleIdContext(securityRuleId);
    }

    @VisibleForTesting
    void setAzureNetworkSecurityGroupOperationSDK(AzureNetworkSecurityGroupOperationSDK operation) {
        this.operation = operation;
    }
}
