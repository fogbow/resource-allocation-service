package cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.sdk;

import cloud.fogbow.common.exceptions.UnexpectedException;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.NetworkSecurityGroups;
import com.microsoft.azure.management.network.SecurityRuleProtocol;

import java.util.Optional;

public class AzureNetworkSecurityGroupSDK {

    // TODO (chico) - Implement tests
    public static Optional<NetworkSecurityGroup> getNetworkSecurityGroup(Azure azure, String azureNetworkInterfaceId)
            throws UnexpectedException {

        try {
            NetworkSecurityGroups securityGroupsSDK = getNetworkSecurityGroupsSDK(azure);
            NetworkSecurityGroup networkSecurityGroup = securityGroupsSDK.getById(azureNetworkInterfaceId);
            return Optional.ofNullable(networkSecurityGroup);
        } catch (RuntimeException e) {
            throw new UnexpectedException(e.getMessage(), e);
        }
    }

    // TODO (chico) - Finish implementation; Implement tests
    public static void updateNetworkSecurityGroup(NetworkSecurityGroup networkSecurityGroup, String cidr,
                                                  int portFrom, int portTo, String ruleName, SecurityRuleProtocol securityRuleProtocol) {
        networkSecurityGroup.update()
                .defineRule(ruleName)
                .allowInbound()
                .fromAddress(cidr)
                .fromAnyPort()
                .toAddress(cidr)
                .toPortRange(portFrom, portTo)
                .withProtocol(securityRuleProtocol)
                .attach();
    }

    public static NetworkSecurityGroups getNetworkSecurityGroupsSDK(Azure azure) {
        return azure.networkSecurityGroups();
    }

}
