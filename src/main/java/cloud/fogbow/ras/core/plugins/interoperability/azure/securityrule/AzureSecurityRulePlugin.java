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
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureResourceIdBuilder;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Properties;

public class AzureSecurityRulePlugin implements SecurityRulePlugin<AzureUser> {

    private static final Logger LOGGER = Logger.getLogger(AzureSecurityRulePlugin.class);

    private final String defaultResourceGroupName;
    private AzureNetworkSecurityGroupOperationSDK azureNetworkSecurityGroupOperationSDK;

    public AzureSecurityRulePlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.azureNetworkSecurityGroupOperationSDK = new AzureNetworkSecurityGroupOperationSDK();
        this.defaultResourceGroupName = properties.getProperty(AzureConstants.DEFAULT_RESOURCE_GROUP_NAME_KEY);
    }

    @Override
    public String requestSecurityRule(SecurityRule securityRule, Order majorOrder, AzureUser azureUser)
            throws FogbowException {

        LOGGER.info(Messages.Info.REQUESTING_INSTANCE_FROM_PROVIDER);

        String networkSecurityGroupName = AzureGeneralUtil.defineResourceName(majorOrder.getInstanceId());
        String networkSecurityGroupId = AzureResourceIdBuilder.networkSecurityGroupId()
                .withSubscriptionId(azureUser.getSubscriptionId())
                .withResourceGroupName(this.defaultResourceGroupName)
                .withResourceName(networkSecurityGroupName)
                .build();
        String cidr = securityRule.getCidr();
        int portFrom = securityRule.getPortFrom();
        int portTo = securityRule.getPortTo();
        SecurityRule.Direction direction = securityRule.getDirection();
        SecurityRule.Protocol protocol = securityRule.getProtocol();
        String securityRuleName = AzureGeneralUtil.generateResourceName();

        AzureUpdateNetworkSecurityGroupRef azureUpdateNetworkSecurityRef = AzureUpdateNetworkSecurityGroupRef.builder()
                .ruleResourceName(securityRuleName)
                .networkSecurityGroupId(networkSecurityGroupId)
                .protocol(protocol)
                .cidr(cidr)
                .direction(direction)
                .portFrom(portFrom)
                .portTo(portTo)
                .checkAndBuild();

        this.azureNetworkSecurityGroupOperationSDK.doCreateInstance(azureUpdateNetworkSecurityRef, azureUser);

        return SecurityRuleIdContext.buildInstanceId(networkSecurityGroupName, securityRuleName);
    }

    @Override
    public List<SecurityRuleInstance> getSecurityRules(Order majorOrder, AzureUser azureUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE_S, majorOrder.getInstanceId()));

        String networkSecurityGroupName = AzureGeneralUtil.defineResourceName(majorOrder.getInstanceId());
        String networkSecurityGroupId = AzureResourceIdBuilder.networkSecurityGroupId()
                .withSubscriptionId(azureUser.getSubscriptionId())
                .withResourceGroupName(this.defaultResourceGroupName)
                .withResourceName(networkSecurityGroupName)
                .build();

        return this.azureNetworkSecurityGroupOperationSDK.getNetworkSecurityRules(networkSecurityGroupId,
                networkSecurityGroupName, azureUser);
    }

    @Override
    public void deleteSecurityRule(String securityRuleId, AzureUser azureUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE_S, securityRuleId));

        SecurityRuleIdContext securityRuleIdContext = this.getSecurityRuleIdContext(securityRuleId);

        String networkSecurityGroupName = securityRuleIdContext.getNetworkSecurityGroupName();
        String networkSecurityGroupId = AzureResourceIdBuilder.networkSecurityGroupId()
                .withSubscriptionId(azureUser.getSubscriptionId())
                .withResourceGroupName(this.defaultResourceGroupName)
                .withResourceName(networkSecurityGroupName)
                .build();

        String securityRuleName = securityRuleIdContext.getSecurityRuleName();
        this.azureNetworkSecurityGroupOperationSDK.deleteNetworkSecurityRule(networkSecurityGroupId, securityRuleName, azureUser);
    }

    @VisibleForTesting
    SecurityRuleIdContext getSecurityRuleIdContext(String securityRuleId) {
        return new SecurityRuleIdContext(securityRuleId);
    }

    @VisibleForTesting
    void setAzureNetworkSecurityGroupOperationSDK(AzureNetworkSecurityGroupOperationSDK operation) {
        this.azureNetworkSecurityGroupOperationSDK = operation;
    }
}
