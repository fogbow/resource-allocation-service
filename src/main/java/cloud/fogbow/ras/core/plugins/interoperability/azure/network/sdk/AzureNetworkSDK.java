package cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk;

import cloud.fogbow.common.exceptions.UnexpectedException;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.NetworkInterfaces;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.resources.fluentcore.model.Indexable;
import rx.Observable;

import java.util.Optional;

public class AzureNetworkSDK {

    private static final String DEFAULT_SECURITY_GROUPS_RULES_NAME = "default-security-group-rules-name";

    public static Optional<NetworkInterface> getNetworkInterface(Azure azure, String azureNetworkInterfaceId)
            throws UnexpectedException {

        try {
            NetworkInterfaces networkInterfaces = getNetworkInterfacesSDK(azure);
            NetworkInterface networkInterface = networkInterfaces.getById(azureNetworkInterfaceId);
            return Optional.ofNullable(networkInterface);
        } catch (RuntimeException e) {
            throw new UnexpectedException(e.getMessage(), e);
        }
    }

    public static Observable<Indexable> createSecurityGroupAsync(Azure azure, String securityGroupName, Region region,
                                                                 String resourceGroupName, String cidr) {
        return azure.networkSecurityGroups()
                .define(securityGroupName)
                .withRegion(region)
                .withExistingResourceGroup(resourceGroupName)
                .defineRule(DEFAULT_SECURITY_GROUPS_RULES_NAME)
                    .allowInbound()
                    .fromAddress(cidr)
                    .fromAnyPort()
                    .toAnyAddress()
                    .toAnyPort()
                    .withAnyProtocol()
                    .attach()
                .createAsync();
    }

    public static Observable<Indexable> createNetworkAsync(Azure azure, String networkName, Region region, String resourceGroupName,
                                                           String subnetName, String cidr, NetworkSecurityGroup networkSecurityGroup) {
        return azure.networks()
                .define(networkName)
                .withRegion(region)
                .withExistingResourceGroup(resourceGroupName)
                .withAddressSpace(cidr)
                .defineSubnet(subnetName)
                    .withAddressPrefix(cidr)
                    .withExistingNetworkSecurityGroup(networkSecurityGroup)
                    .attach()
                .createAsync();
    }

    public static NetworkInterfaces getNetworkInterfacesSDK(Azure azure) {
        return azure.networkInterfaces();
    }

}
