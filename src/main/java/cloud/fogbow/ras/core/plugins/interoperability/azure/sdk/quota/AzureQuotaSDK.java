package cloud.fogbow.ras.core.plugins.interoperability.azure.sdk.quota;

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.ComputeUsage;
import com.microsoft.azure.management.compute.Disk;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineSize;
import com.microsoft.azure.management.network.NetworkUsage;

public class AzureQuotaSDK {

    public static PagedList<NetworkUsage> getNetworkUsageByRegion(Azure azure, String region) {
        return azure.networkUsages().listByRegion(region);
    }

    public static PagedList<ComputeUsage> getComputeUsageByRegion(Azure azure, String region) {
        return azure.computeUsages().listByRegion(region);
    }

    public static PagedList<Disk> getDisks(Azure azure) {
        return azure.disks().list();
    }

    public static PagedList<VirtualMachineSize> getVirtualMachineSizesByRegion(Azure azure, String region) {
        return azure.virtualMachines().sizes().listByRegion(region);
    }

    public static PagedList<VirtualMachine> getVirtualMachines(Azure azure) {
        return azure.virtualMachines().list();
    }

}
