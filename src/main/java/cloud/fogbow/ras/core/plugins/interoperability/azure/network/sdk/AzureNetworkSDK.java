package cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureGeneralUtil;

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.*;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.resources.fluentcore.model.Indexable;

import rx.Completable;
import rx.Observable;

import java.util.Map;
import java.util.Optional;

public class AzureNetworkSDK {

    private static final String DEFAULT_SUBNET_NAME = "default-subnet";

    public static Optional<NetworkInterface> getNetworkInterface(Azure azure, String azureNetworkInterfaceId)
            throws InternalServerErrorException {

        try {
            NetworkInterfaces networkInterfaces = getNetworkInterfacesSDK(azure);
            NetworkInterface networkInterface = networkInterfaces.getById(azureNetworkInterfaceId);
            return Optional.ofNullable(networkInterface);
        } catch (RuntimeException e) {
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    public static Optional<Network> getNetwork(Azure azure, String azureNetworkId)
            throws InternalServerErrorException {

        try {
            Networks networks = getNetworksSDK(azure);
            Network network = networks.getById(azureNetworkId);
            return Optional.ofNullable(network);
        } catch (RuntimeException e) {
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    public static Observable<Indexable> createSecurityGroupAsync(Azure azure, String securityGroupName, Region region,
                                                                 String resourceGroupName, String cidr, Map tags) {
        String securityRuleName = AzureGeneralUtil.generateResourceName();
        return azure.networkSecurityGroups()
                .define(securityGroupName)
                .withRegion(region)
                .withExistingResourceGroup(resourceGroupName)
                .withTags(tags)
                .defineRule(securityRuleName)
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

    public static Completable buildDeleteNetworkInterfaceCompletable(Azure azure, String resourceId)
            throws InternalServerErrorException {

        try {
            NetworkInterfaces networkInterfaces = getNetworkInterfacesSDK(azure);
            return networkInterfaces.deleteByIdAsync(resourceId);
        } catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    @VisibleForTesting
    public static Networks getNetworksSDK(Azure azure) {
        return azure.networks();
    }

}
