package cloud.fogbow.ras.core.plugins.interoperability.azure.publicip.sdk;

import java.util.Optional;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.NetworkSecurityGroups;
import com.microsoft.azure.management.network.PublicIPAddress;
import com.microsoft.azure.management.network.PublicIPAddresses;
import com.microsoft.azure.management.network.SecurityRuleProtocol;
import com.microsoft.azure.management.resources.fluentcore.model.Creatable;

import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureGeneralUtil;
import rx.Completable;
import rx.Observable;

public class AzurePublicIPAddressSDK {

    @VisibleForTesting
    static final String RESOURCE_NAMES_SEPARATOR = "_";
    @VisibleForTesting
    static final int SSH_ACCESS_PORT = 22;

    public static Observable<NetworkInterface> associatePublicIPAddressAsync(
            NetworkInterface networkInterface, Creatable<PublicIPAddress> creatable) {

        return networkInterface.update()
                .withNewPrimaryPublicIPAddress(creatable)
                .applyAsync();
    }
    
    public static Creatable<NetworkSecurityGroup> buildNetworkSecurityGroupCreatable(
            Azure azure, PublicIPAddress publicIPAddress) {

        String regionName = publicIPAddress.regionName();
        String resourceGroupName = publicIPAddress.resourceGroupName();
        String securityGroupName = publicIPAddress.name();
        String securityRuleName = AzureGeneralUtil.generateResourceName();
        String ipAddress = publicIPAddress.ipAddress();

        return azure.networkSecurityGroups()
                .define(securityGroupName)
                .withRegion(regionName)
                .withExistingResourceGroup(resourceGroupName)
                .defineRule(securityRuleName)
                    .allowInbound()
                    .fromAnyAddress()
                    .fromAnyPort()
                    .toAddress(ipAddress)
                    .toPort(SSH_ACCESS_PORT)
                    .withProtocol(SecurityRuleProtocol.ASTERISK)
                    .attach();
    }
    
    public static Observable<NetworkInterface> associateNetworkSecurityGroupAsync(
            NetworkInterface networkInterface, Creatable<NetworkSecurityGroup> creatable) {

        return networkInterface.update()
                .withNewNetworkSecurityGroup(creatable)
                .applyAsync();
    }

    public static Observable<NetworkInterface> disassociateNetworkSecurityGroupAsync(
            NetworkInterface networkInterface) {

        return networkInterface.update()
                .withoutNetworkSecurityGroup()
                .applyAsync();
    }
    
    public static Observable<NetworkInterface> disassociatePublicIPAddressAsync(
            NetworkInterface networkInterface) {

        return networkInterface.update()
                .withoutPrimaryPublicIPAddress()
                .applyAsync();
    }

    public static Completable deleteNetworkSecurityGroupAsync(Azure azure, String resourceId) {
        NetworkSecurityGroups networkSecurityGroups = getNetworkSecurityGroupsSDK(azure);
        return networkSecurityGroups.deleteByIdAsync(resourceId);
    }

    // This class is used only for test proposes.
    // It is necessary because was not possible mock the Azure(final class)
    @VisibleForTesting
    static NetworkSecurityGroups getNetworkSecurityGroupsSDK(Azure azure) {
        return azure.networkSecurityGroups();
    }

    public static Completable deletePublicIpAddressAsync(Azure azure, String resourceId) {
        PublicIPAddresses publicIPAddresses = getPublicIPAddressesSDK(azure);
        return publicIPAddresses.deleteByIdAsync(resourceId);
    }

    public static Optional<NetworkSecurityGroup> getNetworkSecurityGroupFrom(NetworkInterface networkInterface)
            throws InternalServerErrorException {
        try {
            NetworkSecurityGroup networkSecurityGroup = networkInterface.getNetworkSecurityGroup();
            return Optional.ofNullable(networkSecurityGroup);
        } catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    public static Optional<NetworkInterface> getPrimaryNetworkInterfaceFrom(VirtualMachine virtualMachine)
            throws InternalServerErrorException {
        try {
            NetworkInterface networkInterface = virtualMachine.getPrimaryNetworkInterface();
            return Optional.ofNullable(networkInterface);
        } catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage());
        }
    }
    
    public static Optional<PublicIPAddress> getPublicIpAddress(Azure azure, String resourceId) 
            throws InternalServerErrorException {
        try {
            PublicIPAddresses publicIPAddresses = getPublicIPAddressesSDK(azure);
            return Optional.ofNullable(publicIPAddresses.getById(resourceId));
        } catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    // This class is used only for test proposes.
    // It is necessary because was not possible mock the Azure(final class)
    @VisibleForTesting
    static PublicIPAddresses getPublicIPAddressesSDK(Azure azure) {
        return azure.publicIPAddresses();
    }

}
