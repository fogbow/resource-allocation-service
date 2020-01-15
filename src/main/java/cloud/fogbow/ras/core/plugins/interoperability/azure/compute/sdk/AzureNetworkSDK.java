package cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk;

import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.NetworkInterface;

public class AzureNetworkSDK {

    // TODO(chico) implement tests
    public static NetworkInterface getNetworkInterface(Azure azure, String azureNetworkInterfaceId)
            throws InstanceNotFoundException {

        try {
            return azure.networkInterfaces().getById(azureNetworkInterfaceId);
        } catch (RuntimeException e) {
            throw new InstanceNotFoundException(e.getMessage(), e);
        }
    }

}
