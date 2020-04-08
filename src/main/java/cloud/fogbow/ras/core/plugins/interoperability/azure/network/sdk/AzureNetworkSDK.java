package cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk;

import cloud.fogbow.common.exceptions.UnexpectedException;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.*;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.resources.fluentcore.model.Indexable;
import rx.Completable;
import rx.Observable;

import java.util.Map;
import java.util.Optional;

public class AzureNetworkSDK {

    private static final String DEFAULT_SECURITY_GROUPS_RULES_NAME = "default-security-group-rules-name";
    private static final String DEFAULT_SUBNET_NAME = "default-subnet-name";

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

    // TODO(chico) - Implement tests
    public static Optional<Network> getNetwork(Azure azure, String azureNetworkId)
            throws UnexpectedException {

        try {
            Networks networks = getNetworksSDK(azure);
            Network network = networks.getById(azureNetworkId);
            return Optional.ofNullable(network);
        } catch (RuntimeException e) {
            throw new UnexpectedException(e.getMessage(), e);
        }
    }

    public static Observable<Indexable> createSecurityGroupAsync(Azure azure, String securityGroupName, Region region,
                                                                 String resourceGroupName, String cidr, Map tags) {
        return azure.networkSecurityGroups()
                .define(securityGroupName)
                .withRegion(region)
                .withExistingResourceGroup(resourceGroupName)
                .withTags(tags)
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

    public static Network createNetworkSync(Azure azure, String networkName, Region region, String resourceGroupName,
                                            String cidr, NetworkSecurityGroup networkSecurityGroup, Map tags) {
        return azure.networks()
                .define(networkName)
                .withRegion(region)
                .withExistingResourceGroup(resourceGroupName)
                .withTags(tags)
                .withAddressSpace(cidr)
                .defineSubnet(DEFAULT_SUBNET_NAME)
                    .withAddressPrefix(cidr)
                    .withExistingNetworkSecurityGroup(networkSecurityGroup)
                    .attach()
                .create();
    }

    public static Completable buildDeleteVirtualNetworkCompletable(Azure azure, String virtualNetworkId) {
        return azure.networks().deleteByIdAsync(virtualNetworkId);
    }

    public static Completable buildDeleteNetworkSecurityGroupCompletable(Azure azure, String securityGroupId) {
        return azure.networkSecurityGroups().deleteByIdAsync(securityGroupId);
    }

    public static NetworkInterfaces getNetworkInterfacesSDK(Azure azure) {
        return azure.networkInterfaces();
    }

    public static Networks getNetworksSDK(Azure azure) {
        return azure.networks();
    }

}
