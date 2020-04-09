package cloud.fogbow.ras.core.plugins.interoperability.azure.publicip.sdk;

import java.util.Optional;

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.PublicIPAddress;
import com.microsoft.azure.management.network.PublicIPAddresses;
import com.microsoft.azure.management.resources.fluentcore.model.Creatable;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.ras.constants.Messages;
import rx.Completable;
import rx.Observable;

public class AzurePublicIPAddressSDK {

    public static void deleteSecurityRuleFrom(VirtualMachine virtualMachine) {
        NetworkInterface networkInterface = virtualMachine.getPrimaryNetworkInterface();
        NetworkSecurityGroup networkSecurityGroup = networkInterface.getNetworkSecurityGroup();
        networkSecurityGroup.update().withoutRule("ruleName").apply();
    }
    
    public static void associatePublicIPAddressCreatable(VirtualMachine virtualMachine,
            Creatable<PublicIPAddress> publicIPCreatable) throws UnexpectedException {
        try {
            NetworkInterface networkInterface = virtualMachine.getPrimaryNetworkInterface();
            networkInterface.update().withNewPrimaryPublicIPAddress(publicIPCreatable).apply();
        } catch (Exception e) {
            String message = String.format(Messages.Exception.GENERIC_EXCEPTION, e);
            throw new UnexpectedException(message, e);
        }
    }
    
    public static void disassociatePublicIPAddressFrom(VirtualMachine virtualMachine)
            throws UnexpectedException {
        try {
            NetworkInterface networkInterface = virtualMachine.getPrimaryNetworkInterface();
            networkInterface.update().withoutPrimaryPublicIPAddress().apply();
        } catch (Exception e) {
            String message = String.format(Messages.Exception.GENERIC_EXCEPTION, e);
            throw new UnexpectedException(message, e);
        }
    }
    
    public static Completable buildDeletePublicIpAddressCompletable(Azure azure, String resourceId)
            throws UnexpectedException {
        try {
            PublicIPAddresses publicIPAddresses = getPublicIPAddressesSDK(azure);
            return publicIPAddresses.deleteByIdAsync(resourceId);
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
