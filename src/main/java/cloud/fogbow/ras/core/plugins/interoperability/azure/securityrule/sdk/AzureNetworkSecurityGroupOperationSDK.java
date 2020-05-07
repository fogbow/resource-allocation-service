package cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.sdk;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.sdk.model.AzureUpdateNetworkSecurityGroupRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.util.AzureSecurityRuleUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.util.SecurityRuleIdContext;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureClientCacheManager;
import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.NetworkSecurityRule;
import com.microsoft.azure.management.network.SecurityRuleDirection;
import com.microsoft.azure.management.network.SecurityRuleProtocol;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class AzureNetworkSecurityGroupOperationSDK {

    private final int FIRST_PRIORITY_VALUE = 100;
    private static final int UNKNOWN_PRIORITY_VALUE = -1;
    private static int currentPriority = UNKNOWN_PRIORITY_VALUE;

    public void doCreateInstance(AzureUpdateNetworkSecurityGroupRef azureUpdateNetworkSecurityRef, AzureUser azureUser)
            throws FogbowException {

        Azure azure = AzureClientCacheManager.getAzure(azureUser);
        String networkSecurityGroupId = azureUpdateNetworkSecurityRef.getNetworkSecurityGroupId();
        NetworkSecurityGroup networkSecurityGroup = AzureNetworkSecurityGroupSDK
                .getNetworkSecurityGroup(azure, networkSecurityGroupId)
                .orElseThrow(InstanceNotFoundException::new);

        String cidr = azureUpdateNetworkSecurityRef.getCidr();
        int portFrom = azureUpdateNetworkSecurityRef.getPortFrom();
        int portTo = azureUpdateNetworkSecurityRef.getPortTo();
        String ruleName = azureUpdateNetworkSecurityRef.getRuleResourceName();
        SecurityRuleProtocol securityRuleProtocol = AzureSecurityRuleUtil.getProtocol(azureUpdateNetworkSecurityRef.getProtocol());
        AzureNetworkSecurityGroupSDK.Direction direction = AzureSecurityRuleUtil.getDirection(azureUpdateNetworkSecurityRef.getDirection());
        int priority = getPriority(networkSecurityGroup);

        AzureNetworkSecurityGroupSDK.updateNetworkSecurityGroup(networkSecurityGroup, cidr, portFrom,
                portTo, ruleName, securityRuleProtocol, direction, priority);
    }

    @VisibleForTesting
    int getPriority(NetworkSecurityGroup networkSecurityGroup) {
        int priority = currentPriority;
        if (priority == UNKNOWN_PRIORITY_VALUE) {
            priority = getLastPriority(networkSecurityGroup);
        }
        return priority + 1;
    }

    public List<SecurityRuleInstance> getNetworkSecurityRules(String networkSecurityGroupId, String networkSecurityGroupName, AzureUser azureUser) throws FogbowException {
        Azure azure = AzureClientCacheManager.getAzure(azureUser);
        NetworkSecurityGroup networkSecurityGroup = AzureNetworkSecurityGroupSDK
                .getNetworkSecurityGroup(azure, networkSecurityGroupId)
                .orElseThrow(InstanceNotFoundException::new);

        return networkSecurityGroup.securityRules().values().stream()
                .filter(networkSecurityRule -> networkSecurityRule.name().startsWith(SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX))
                .map(networkSecurityRule -> buildSecurityRuleInstance(networkSecurityRule, networkSecurityGroupName))
                .collect(Collectors.toList());
    }

    @VisibleForTesting
    SecurityRuleInstance buildSecurityRuleInstance(NetworkSecurityRule networkSecurityRule, String networkSecurityGroupName) {
        String cidr = networkSecurityRule.sourceAddressPrefix();
        SecurityRuleDirection securityRuleDirection = networkSecurityRule.direction();
        SecurityRule.Direction direction = AzureSecurityRuleUtil.getDirection(securityRuleDirection);
        String portRange = networkSecurityRule.destinationPortRange();
        AzureSecurityRuleUtil.Ports ports = AzureSecurityRuleUtil.getPorts(portRange);
        int portFrom = ports.getFrom();
        int portTo = ports.getTo();
        String ipAddress = AzureSecurityRuleUtil.getIpAddress(cidr);
        SecurityRule.EtherType etherType = AzureSecurityRuleUtil.inferEtherType(ipAddress);
        SecurityRuleProtocol securityRuleProtocol = networkSecurityRule.protocol();
        SecurityRule.Protocol protocol = AzureSecurityRuleUtil.getProtocol(securityRuleProtocol);
        String ruleName = networkSecurityRule.name();
        String instanceId = SecurityRuleIdContext.buildInstanceId(networkSecurityGroupName, ruleName);
        return new SecurityRuleInstance(instanceId, direction, portFrom, portTo, cidr, etherType, protocol);
    }

    public void deleteNetworkSecurityRule(String networkSecurityGroupId, String securityRuleName, AzureUser azureUser)
            throws FogbowException {

        Azure azure = AzureClientCacheManager.getAzure(azureUser);

        NetworkSecurityGroup networkSecurityGroup = AzureNetworkSecurityGroupSDK
                .getNetworkSecurityGroup(azure, networkSecurityGroupId)
                .orElseThrow(InstanceNotFoundException::new);

        AzureNetworkSecurityGroupSDK.deleteNetworkSecurityRule(networkSecurityGroup, securityRuleName);
    }

    private int getLastPriority(NetworkSecurityGroup networkSecurityGroup) {
        try {
            return networkSecurityGroup.securityRules().values().stream()
                    .filter(networkSecurityRule -> networkSecurityRule.name().startsWith(SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX))
                    .map(networkSecurityRule -> networkSecurityRule.priority())
                    .reduce((priorityOne, priorityTwo) -> priorityOne >= priorityTwo ? priorityOne : priorityTwo)
                    .get();
        } catch (NoSuchElementException e) {
            return FIRST_PRIORITY_VALUE;
        }
    }

}
