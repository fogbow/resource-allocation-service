package cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk;

import cloud.fogbow.common.exceptions.UnexpectedException;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.NetworkInterfaces;
import com.microsoft.azure.management.resources.fluentcore.model.Indexable;
import rx.Observable;

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

    // TODO implement
    public static Observable<Indexable> createSecurityGroupAsync() {
        return null;
    }

    // TODO implement
    public static Observable<Indexable> createNetworkAsync() {
        return null;
    }

    public static NetworkInterfaces getNetworkInterfacesSDK(Azure azure) {
        return azure.networkInterfaces();
    }

}
