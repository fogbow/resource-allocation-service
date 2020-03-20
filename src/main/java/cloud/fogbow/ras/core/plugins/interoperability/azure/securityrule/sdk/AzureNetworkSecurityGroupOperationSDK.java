package cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.sdk;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.sdk.model.AzureUpdateNetworkSecurityGroupRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureClientCacheManager;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.SecurityRuleProtocol;

public class AzureNetworkSecurityGroupOperationSDK {

    public AzureNetworkSecurityGroupOperationSDK() { }

    // TODO (chico) - Implement tests
    public void doCreateInstance(AzureUpdateNetworkSecurityGroupRef azureUpdateNetworkSecurityRef, AzureUser azureUser)
            throws FogbowException {

        Azure azure = AzureClientCacheManager.getAzure(azureUser);
        String networkSecurityGroupId = azureUpdateNetworkSecurityRef.getNetworkSecurityGroupId();
        NetworkSecurityGroup networkSecurityGroup = AzureNetworkSecurityGroupSDK.getNetworkSecurityGroup(azure, networkSecurityGroupId)
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

}
