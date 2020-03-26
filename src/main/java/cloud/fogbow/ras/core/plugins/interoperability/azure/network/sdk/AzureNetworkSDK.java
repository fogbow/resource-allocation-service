package cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.ras.constants.Messages;

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.NetworkInterfaces;
import com.microsoft.azure.management.network.Networks;

import java.util.Optional;

public class AzureNetworkSDK {

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

    public static NetworkInterfaces getNetworkInterfacesSDK(Azure azure) {
        return azure.networkInterfaces();
    }

    public static Optional<Network> getNetwork(Azure azure, String resourceId)
            throws UnexpectedException {
        try {
            Networks networks = getNetworkSDK(azure);
            return Optional.ofNullable(networks.getById(resourceId));
        } catch (Exception e) {
            String message = String.format(Messages.Exception.GENERIC_EXCEPTION, e);
            throw new UnexpectedException(message, e);
        }
    }

    @VisibleForTesting
    static Networks getNetworkSDK(Azure azure) {
        return azure.networks();
    }

}
