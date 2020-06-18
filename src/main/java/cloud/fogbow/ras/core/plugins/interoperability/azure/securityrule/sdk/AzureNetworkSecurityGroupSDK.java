package cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.sdk;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.NetworkSecurityGroups;
import com.microsoft.azure.management.network.SecurityRuleProtocol;

import java.util.Optional;

import static com.microsoft.azure.management.network.NetworkSecurityRule.UpdateDefinitionStages.Blank;
import static com.microsoft.azure.management.network.NetworkSecurityRule.UpdateDefinitionStages.WithSourceAddress;

public class AzureNetworkSecurityGroupSDK {

    public static Optional<NetworkSecurityGroup> getNetworkSecurityGroup(Azure azure, String networkSecurityGroupId)
            throws InternalServerErrorException {

        try {
            NetworkSecurityGroups securityGroupsSDK = getNetworkSecurityGroupsSDK(azure);
            NetworkSecurityGroup networkSecurityGroup = securityGroupsSDK.getById(networkSecurityGroupId);
            return Optional.ofNullable(networkSecurityGroup);
        } catch (RuntimeException e) {
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    public static void updateNetworkSecurityGroup(NetworkSecurityGroup networkSecurityGroup, String cidr,
                                                  int portFrom, int portTo, String ruleName,
                                                  SecurityRuleProtocol securityRuleProtocol,
                                                  Direction direction, int priority) {

        Blank<NetworkSecurityGroup.Update> updateBlank = networkSecurityGroup
                .update()
                .defineRule(ruleName);
        WithSourceAddress<NetworkSecurityGroup.Update> updateWithSourceAddress;
        if (direction == Direction.IN_BOUND) {
            updateWithSourceAddress = updateBlank.allowInbound();
        } else {
            updateWithSourceAddress = updateBlank.allowOutbound();
        }
        updateWithSourceAddress
                .fromAddress(cidr)
                .fromAnyPort()
                .toAnyAddress()
                .toPortRange(portFrom, portTo)
                .withProtocol(securityRuleProtocol)
                .withPriority(priority)
                .attach()
                .apply();
    }

    public static NetworkSecurityGroups getNetworkSecurityGroupsSDK(Azure azure) {
        return azure.networkSecurityGroups();
    }

    public static void deleteNetworkSecurityRule(NetworkSecurityGroup networkSecurityGroup, String securityRuleName)
            throws InternalServerErrorException {

        try {
            networkSecurityGroup.update().withoutRule(securityRuleName).apply();
        } catch (RuntimeException e) {
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    public enum Direction {
        IN_BOUND, OUT_BOUND
    }

}
