package cloud.fogbow.ras.core.plugins.interoperability.azure.publicip.sdk;

import java.util.Optional;

import org.apache.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.Networks;
import com.microsoft.azure.management.network.PublicIPAddress;
import com.microsoft.azure.management.network.PublicIPAddresses;
import com.microsoft.azure.management.network.SecurityRuleProtocol;
import com.microsoft.azure.management.network.Subnet;
import com.microsoft.azure.management.network.implementation.NetworkManager;
import com.microsoft.azure.management.resources.fluentcore.model.Creatable;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.ras.constants.Messages;
import rx.Completable;
import rx.Observable;

public class AzurePublicIPAddressSDK {

    @VisibleForTesting
    static final Logger LOGGER = Logger.getLogger(AzurePublicIPAddressSDK.class);
    @VisibleForTesting
    static final String SECURITY_RULE_NAME_SUFIX = "_security-rule";
    @VisibleForTesting
    static final int SSH_ACCESS_PORT = 22;

    public static Observable<NetworkInterface> associatePublicIPAddressAsync(
            NetworkInterface networkInterface, Creatable<PublicIPAddress> creatable) {

        return networkInterface.update()
                .withNewPrimaryPublicIPAddress(creatable)
                .applyAsync();
    }
    
    public static Creatable<NetworkSecurityGroup> buildSecurityRuleCreatable(Azure azure,
            PublicIPAddress publicIPAddress, String networkSecurityGroupName) {

        String regionName = publicIPAddress.regionName();
        String resourceGroupName = publicIPAddress.resourceGroupName();
        String securityRuleName = publicIPAddress.name() + SECURITY_RULE_NAME_SUFIX;
        String ipAddress = publicIPAddress.ipAddress();

        return azure.networkSecurityGroups()
                .define(networkSecurityGroupName)
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

    public static Observable<NetworkSecurityGroup> deleteSecurityRuleAsync(
            NetworkSecurityGroup networkSecurityGroup, String resourceName) {

        String securityRuleName = resourceName + SECURITY_RULE_NAME_SUFIX;
        return networkSecurityGroup.update()
                .withoutRule(securityRuleName)
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

    public static Completable deletePublicIpAddressAsync(Azure azure, String resourceId) {
        PublicIPAddresses publicIPAddresses = getPublicIPAddressesSDK(azure);
        return publicIPAddresses.deleteByIdAsync(resourceId);
    }

    public static Optional<NetworkSecurityGroup> getNetworkSecurityGroupFrom(NetworkInterface networkInterface) {
        NetworkSecurityGroup networkSecurityGroup = null;
        try {
            NetworkManager networkManager = networkInterface.manager();
            Networks networks = networkManager.networks();
            Network network = networks.list().listIterator().next();
            Subnet subnet = network.subnets().values().iterator().next();
            networkSecurityGroup = subnet.getNetworkSecurityGroup();
        } catch (Exception e) {
            LOGGER.error(Messages.Error.UNEXPECTED_ERROR, e);
        }
        return Optional.ofNullable(networkSecurityGroup);
    }

    public static Optional<NetworkInterface> getPrimaryNetworkInterfaceFrom(VirtualMachine virtualMachine)
            throws UnexpectedException {
        try {
            NetworkInterface networkInterface = virtualMachine.getPrimaryNetworkInterface();
            return Optional.ofNullable(networkInterface);
        } catch (Exception e) {
            String message = String.format(Messages.Exception.GENERIC_EXCEPTION, e);
            throw new UnexpectedException(message, e);
        }
    }
    
    public static Optional<PublicIPAddress> getPublicIpAddress(Azure azure, String resourceId) 
            throws UnexpectedException {
        try {
            PublicIPAddresses publicIPAddresses = getPublicIPAddressesSDK(azure);
            return Optional.ofNullable(publicIPAddresses.getById(resourceId));
        } catch (Exception e) {
            String message = String.format(Messages.Exception.GENERIC_EXCEPTION, e);
            throw new UnexpectedException(message, e);
        }
    }

    // This class is used only for test proposes.
    // It is necessary because was not possible mock the Azure(final class)
    @VisibleForTesting
    static PublicIPAddresses getPublicIPAddressesSDK(Azure azure) {
        return azure.publicIPAddresses();
    }

}
