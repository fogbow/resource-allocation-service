package cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.sdk;

import cloud.fogbow.common.exceptions.UnexpectedException;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.NetworkSecurityGroups;

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
    public void updateNetworkSecurityGroup(NetworkSecurityGroup networkSecurityGroup, String cidr) {
        networkSecurityGroup.update()
                .defineRule("Name")
                .allowInbound()
                .fromAddress(cidr)
                .fromAnyPort()
                .toAnyAddress()
                .toAnyPort()
                .withAnyProtocol()
                .attach();
    }

    public static NetworkSecurityGroups getNetworkSecurityGroupsSDK(Azure azure) {
        return azure.networkSecurityGroups();
    }

}
