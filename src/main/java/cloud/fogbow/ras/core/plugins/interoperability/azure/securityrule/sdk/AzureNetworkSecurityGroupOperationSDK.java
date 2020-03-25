package cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.sdk;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.sdk.model.AzureUpdateNetworkSecurityGroupRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureClientCacheManager;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9.CidrUtils;
import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.SecurityRuleProtocol;

import java.util.List;
import java.util.stream.Collectors;

public class AzureNetworkSecurityGroupOperationSDK {

    public AzureNetworkSecurityGroupOperationSDK() { }

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

        AzureNetworkSecurityGroupSDK.updateNetworkSecurityGroup(networkSecurityGroup, cidr, portFrom,
                portTo, ruleName, securityRuleProtocol, direction);
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
                // TODO (chico) - review it
                throw new FogbowException();
        }
    }

    // TODO (chico) - Finish implementation and Implement tests
    public List<SecurityRuleInstance> getNetworkSecurityRules(AzureUser azureUser, String networkSecurityGroupId) throws FogbowException {
        Azure azure = AzureClientCacheManager.getAzure(azureUser);
        NetworkSecurityGroup networkSecurityGroup = AzureNetworkSecurityGroupSDK
                .getNetworkSecurityGroup(azure, networkSecurityGroupId)
                .orElseThrow(InstanceNotFoundException::new);

        return networkSecurityGroup.securityRules().values().stream()
                .map(networkSecurityRule -> {
                    String cidr = networkSecurityRule.sourceAddressPrefix();
//                    SecurityRuleDirection direction = networkSecurityRule.direction();
                    SecurityRule.Direction direction = SecurityRule.Direction.IN;
                    int portFrom = 0;
                    int portTo = 0;
                    String ipAddress = "";
                    SecurityRule.EtherType etherType = inferEtherType(ipAddress);
                    SecurityRule.Protocol protocol = null;
                    String instanceId = "";

                    return new SecurityRuleInstance(instanceId, direction, portFrom, portTo, cidr, etherType, protocol);
                }).collect(Collectors.toList());
    }

    // TODO (chico) - Move it to the Util class
    @VisibleForTesting
    SecurityRule.EtherType inferEtherType(String ipAddress) {
        if (CidrUtils.isIpv4(ipAddress)) {
            return SecurityRule.EtherType.IPv4;
        } else if (CidrUtils.isIpv6(ipAddress)) {
            return SecurityRule.EtherType.IPv6;
        } else {
            return null;
        }
    }

    // TODO (chico) - Finish implementation and Implement tests
    public void deleteNetworkSecurityRule(AzureUser azureUser, String securityRuleId) throws FogbowException {
        Azure azure = AzureClientCacheManager.getAzure(azureUser);
        AzureNetworkSecurityGroupSDK.deleteNetworkSecurityRule(azure, securityRuleId);
    }
}
