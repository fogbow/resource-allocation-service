package cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.sdk;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.sdk.model.AzureUpdateNetworkSecurityGroupRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.util.AzureSecurityRuleUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureClientCacheManager;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.SecurityRuleDirection;
import com.microsoft.azure.management.network.SecurityRuleProtocol;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class AzureNetworkSecurityGroupOperationSDK {

    private final int FIRST_PRIORITY_VALUE = 1;

    // TODO (chico) - Implement tests
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
        SecurityRuleProtocol securityRuleProtocol = getProtocol(azureUpdateNetworkSecurityRef.getProtocol());
        AzureNetworkSecurityGroupSDK.Direction direction = getDirection(azureUpdateNetworkSecurityRef.getDirection());
        // TODO(chico) - Improve it because it may cause performance problems
        int priority = getLastPriority(networkSecurityGroup) + 1;

        AzureNetworkSecurityGroupSDK.updateNetworkSecurityGroup(networkSecurityGroup, cidr, portFrom,
                portTo, ruleName, securityRuleProtocol, direction, priority);
    }

    // TODO (chico) - Implement tests
    private AzureNetworkSecurityGroupSDK.Direction getDirection(SecurityRule.Direction direction) {
        if (direction.equals(SecurityRule.Direction.IN)) {
            return AzureNetworkSecurityGroupSDK.Direction.IN_BOUND;
        }
        return AzureNetworkSecurityGroupSDK.Direction.OUT_BOUND;
    }

    // TODO (chico) - Implement tests
    public SecurityRuleProtocol getProtocol(SecurityRule.Protocol protocol) throws FogbowException {
        switch (protocol) {
            case ANY:
                return SecurityRuleProtocol.ASTERISK;
            case TCP:
                return SecurityRuleProtocol.TCP;
            case UDP:
                return SecurityRuleProtocol.UDP;
            default:
                throw new UnexpectedException();
        }
    }

    // TODO (chico) - Finish implementation and Implement tests
    public List<SecurityRuleInstance> getNetworkSecurityRules(String networkSecurityGroupId, AzureUser azureUser) throws FogbowException {
        Azure azure = AzureClientCacheManager.getAzure(azureUser);
        NetworkSecurityGroup networkSecurityGroup = AzureNetworkSecurityGroupSDK
                .getNetworkSecurityGroup(azure, networkSecurityGroupId)
                .orElseThrow(InstanceNotFoundException::new);

        return networkSecurityGroup.securityRules().values().stream()
                .filter(networkSecurityRule -> networkSecurityRule.name().startsWith(SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX))
                .map(networkSecurityRule -> {
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
                    String instanceId = networkSecurityRule.inner().id();

                    return new SecurityRuleInstance(instanceId, direction, portFrom, portTo, cidr, etherType, protocol);
                }).collect(Collectors.toList());
    }

    // TODO (chico) - Finish implementation and Implement tests
    public void deleteNetworkSecurityRule(String networkSecurityGroupId, String securityRuleName, AzureUser azureUser)
            throws FogbowException {

        Azure azure = AzureClientCacheManager.getAzure(azureUser);

        NetworkSecurityGroup networkSecurityGroup = AzureNetworkSecurityGroupSDK
                .getNetworkSecurityGroup(azure, networkSecurityGroupId)
                .orElseThrow(InstanceNotFoundException::new);

        AzureNetworkSecurityGroupSDK.deleteNetworkSecurityRule(networkSecurityGroup, securityRuleName);
    }

    // TODO (chico) - Implement tests
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
