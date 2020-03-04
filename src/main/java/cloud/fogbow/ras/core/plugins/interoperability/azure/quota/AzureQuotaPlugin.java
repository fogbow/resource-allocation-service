package cloud.fogbow.ras.core.plugins.interoperability.azure.quota;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.constants.FogbowConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.util.AzureClientCacheManager;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.quotas.ResourceQuota;
import cloud.fogbow.ras.api.http.response.quotas.allocation.*;
import cloud.fogbow.ras.core.plugins.interoperability.QuotaPlugin;
import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.ComputeUsage;
import com.microsoft.azure.management.compute.Disk;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineSize;
import com.microsoft.azure.management.network.NetworkUsage;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

public class AzureQuotaPlugin implements QuotaPlugin<AzureUser> {

    private static final String QUOTA_VM_INSTANCES_KEY = "virtualMachines";
    private static final String QUOTA_VM_CORES_KEY = "cores";
    private static final String QUOTA_NETWORK_INSTANCES = "VirtualNetworks";
    private static final String QUOTA_PUBLIC_IP_ADDRESSES = "PublicIPAddresses";
    private static final int NO_USAGE = 0;
    private static final int ONE_PETABYTE_IN_GIGABYTES = 1048576;

    /**
     * This value is hardcoded because at the time this plugin was developed, a value for maximum storage capacity was
     * not provided by the SDK. The current value is informed in the documentation.
     *
     * https://docs.microsoft.com/en-us/azure/azure-resource-manager/management/azure-subscription-service-limits#storage-limits
     */
    private static final int MAXIMUM_STORAGE_ACCOUNT_CAPACITY = 50 * ONE_PETABYTE_IN_GIGABYTES;

    private final String defaultRegionName;

    public AzureQuotaPlugin(@NotNull String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.defaultRegionName = properties.getProperty(AzureConstants.DEFAULT_REGION_NAME_KEY);
    }

    @Override
    public ResourceQuota getUserQuota(AzureUser cloudUser) throws FogbowException {
        Azure azure = AzureClientCacheManager.getAzure(cloudUser);

        Map<String, ComputeUsage> computeUsages = this.getComputeUsageMap(azure);
        Map<String, NetworkUsage> networkUsages = this.getNetworkUsageMap(azure);
        PagedList<Disk> disks = this.getDisks(azure);

        ResourceAllocation totalQuota = this.getTotalQuota(computeUsages, networkUsages);
        ResourceAllocation usedQuota = this.getUsedQuota(computeUsages, networkUsages, disks, azure);
        return new ResourceQuota(totalQuota, usedQuota);
    }

    @VisibleForTesting
    ResourceAllocation getUsedQuota(Map<String, ComputeUsage> computeUsages, Map<String, NetworkUsage> networkUsages, PagedList<Disk> disks, Azure azure) {
        ComputeAllocation computeAllocation = this.getUsedComputeAllocation(computeUsages, azure);
        NetworkAllocation networkAllocation = this.getUsedNetworkAllocation(networkUsages);
        PublicIpAllocation publicIpAllocation = this.getUsedPublicIpAllocation(networkUsages);
        VolumeAllocation volumeAllocation = this.getUsedVolumeAllocation(disks);
        return this.buildQuota(computeAllocation, networkAllocation, publicIpAllocation, volumeAllocation);
    }

    @VisibleForTesting
    VolumeAllocation getUsedVolumeAllocation(PagedList<Disk> disks) {
        int volumes = disks.size();
        int storage = this.getStorageUsage(disks);
        return new VolumeAllocation(volumes, storage);
    }

    @VisibleForTesting
    int getStorageUsage(PagedList<Disk> disks) {
        int initialValue = NO_USAGE;
        return disks.stream()
                .map(disk -> disk.sizeInGB())
                .reduce(initialValue, Integer::sum);
    }

    @VisibleForTesting
    NetworkAllocation getUsedNetworkAllocation(Map<String, NetworkUsage> networkUsages) {
        NetworkUsage networkUsage = networkUsages.get(QUOTA_NETWORK_INSTANCES);
        int instances = networkUsage == null ? NO_USAGE : (int) networkUsage.currentValue();
        return new NetworkAllocation(instances);
    }

    @VisibleForTesting
    ComputeAllocation getUsedComputeAllocation(Map<String, ComputeUsage> computeUsages, Azure azure) {
        ComputeUsage vmUsage = computeUsages.get(QUOTA_VM_INSTANCES_KEY);
        ComputeUsage coreUsage = computeUsages.get(QUOTA_VM_CORES_KEY);

        int instances = vmUsage == null ? NO_USAGE : vmUsage.currentValue();
        int cores = coreUsage == null ? NO_USAGE : coreUsage.currentValue();
        int ram = this.getMemoryUsage(azure);

        return new ComputeAllocation(cores, ram, instances);
    }

    @VisibleForTesting
    PublicIpAllocation getUsedPublicIpAllocation(Map<String, NetworkUsage> networkUsages) {
        NetworkUsage publicIpUsage = networkUsages.get(QUOTA_PUBLIC_IP_ADDRESSES);
        int instances = publicIpUsage == null ? NO_USAGE : (int) publicIpUsage.currentValue();
        return new PublicIpAllocation(instances);
    }

    @VisibleForTesting
    ResourceAllocation getTotalQuota(Map<String, ComputeUsage> computeUsages,
                                     Map<String, NetworkUsage> networkUsages) {
        ComputeAllocation computeAllocation = this.getTotalComputeAllocation(computeUsages);
        NetworkAllocation networkAllocation = this.getTotalNetworkAllocation(networkUsages);
        PublicIpAllocation publicIpAllocation = this.getTotalPublicIpAllocation(networkUsages);
        VolumeAllocation volumeAllocation = this.getTotalVolumeAllocation();
        return this.buildQuota(computeAllocation, networkAllocation, publicIpAllocation, volumeAllocation);
    }

    @VisibleForTesting
    VolumeAllocation getTotalVolumeAllocation() {
        int volumes = FogbowConstants.UNLIMITED_RESOURCE;
        int storage = MAXIMUM_STORAGE_ACCOUNT_CAPACITY;
        return new VolumeAllocation(volumes, storage);
    }

    @VisibleForTesting
    PublicIpAllocation getTotalPublicIpAllocation(Map<String, NetworkUsage> networkUsages) {
        NetworkUsage publicIpUsage = networkUsages.get(QUOTA_PUBLIC_IP_ADDRESSES);
        int instances = publicIpUsage == null ? NO_USAGE : (int) publicIpUsage.limit();
        return new PublicIpAllocation(instances);
    }

    @VisibleForTesting
    NetworkAllocation getTotalNetworkAllocation(Map<String, NetworkUsage> networkUsages) {
        NetworkUsage networkUsage = networkUsages.get(QUOTA_NETWORK_INSTANCES);
        int instances = networkUsage == null ? NO_USAGE : (int) networkUsage.limit();
        return new NetworkAllocation(instances);
    }

    @VisibleForTesting
    ComputeAllocation getTotalComputeAllocation(Map<String, ComputeUsage> computeUsages) {
        ComputeUsage vmUsage = computeUsages.get(QUOTA_VM_INSTANCES_KEY);
        ComputeUsage coreUsage = computeUsages.get(QUOTA_VM_CORES_KEY);

        int instances = vmUsage == null ? NO_USAGE : (int) vmUsage.limit();
        int cores = coreUsage == null ? NO_USAGE : (int) coreUsage.limit();
        int ram = FogbowConstants.UNLIMITED_RESOURCE;

        return new ComputeAllocation(cores, ram, instances);
    }

    @VisibleForTesting
    ResourceAllocation buildQuota(ComputeAllocation computeAllocation,
                                  NetworkAllocation networkAllocation,
                                  PublicIpAllocation publicIpAllocation,
                                  VolumeAllocation volumeAllocation) {
        int instances = computeAllocation.getInstances();
        int ram = computeAllocation.getRam();
        int vCPU = computeAllocation.getvCPU();
        int volumes = volumeAllocation.getInstances();
        int storage = volumeAllocation.getStorage();
        int networks = networkAllocation.getInstances();
        int publicIps = publicIpAllocation.getInstances();

        return ResourceAllocation.builder()
                .instances(instances)
                .vCPU(vCPU)
                .ram(ram)
                .volumes(volumes)
                .storage(storage)
                .networks(networks)
                .publicIps(publicIps)
                .build();
    }

    @VisibleForTesting
    Map<String, ComputeUsage> getComputeUsageMap(Azure azure) {
        Map<String, ComputeUsage> computeUsageMap = new HashMap<>();
        List<String> validComputeUsages = Arrays.asList(QUOTA_VM_INSTANCES_KEY, QUOTA_VM_CORES_KEY);

        this.getComputeUsage(azure)
                .stream()
                .filter(computeUsage -> validComputeUsages.contains(computeUsage.name().value()))
                .forEach(computeUsage -> computeUsageMap.put(computeUsage.name().value(), computeUsage));

        return computeUsageMap;
    }

    @VisibleForTesting
    PagedList<ComputeUsage> getComputeUsage(Azure azure) {
        return azure.computeUsages().listByRegion(this.defaultRegionName);
    }

    @VisibleForTesting
    Map<String, NetworkUsage> getNetworkUsageMap(Azure azure) {
        Map<String, NetworkUsage> networkUsageMap = new HashMap<>();
        List<String> validNetworkUsages = Arrays.asList(QUOTA_NETWORK_INSTANCES, QUOTA_PUBLIC_IP_ADDRESSES);

        this.getNetworkUsage(azure)
                .stream()
                .filter(networkUsage -> validNetworkUsages.contains(networkUsage.name().value()))
                .forEach(networkUsage -> networkUsageMap.put(networkUsage.name().value(), networkUsage));

        return networkUsageMap;
    }

    @VisibleForTesting
    PagedList<NetworkUsage> getNetworkUsage(Azure azure) {
        return azure.networkUsages().listByRegion(this.defaultRegionName);
    }

    @VisibleForTesting
    int getMemoryUsage(Azure azure) {
        List<String> sizeNamesInUse = this.getVirtualMachineSizeNamesInUse(azure);
        Map<String, VirtualMachineSize> virtualMachineSizes = this.getVirtualMachineSizes(sizeNamesInUse, azure);
        return this.doGetMemoryUsage(sizeNamesInUse, virtualMachineSizes);
    }

    /**
     * Return the total memory in use by all created virtual machines
     *
     * @param sizeNamesInUse list of virtual machine size names
     * @param virtalMachineSizes list of virtual machine sizes in use
     * @return total memory in use
     */
    @VisibleForTesting
    int doGetMemoryUsage(List<String> sizeNamesInUse, Map<String, VirtualMachineSize> virtalMachineSizes) {
        Integer initialValue = NO_USAGE;
        return sizeNamesInUse.stream()
                .map(sizeName -> virtalMachineSizes.get(sizeName))
                .map(virtualMachineSize -> virtualMachineSize.memoryInMB())
                .reduce(initialValue, Integer::sum);
    }

    /**
     * Return a map of virtual machine sizes in use. The virtual machine size name is the key.
     * @param sizeNames a list of virtual machine size names in use
     * @param azure the azure client
     * @return map where
     */
    @VisibleForTesting
    Map<String, VirtualMachineSize> getVirtualMachineSizes(List<String> sizeNames, Azure azure) {
        Map<String, VirtualMachineSize> sizes = new HashMap<>();
        this.getVirtualMachineSizes(azure)
                .stream()
                .filter(virtualMachineSize -> sizeNames.contains(virtualMachineSize.name()))
                .forEach(virtualMachineSize -> sizes.put(virtualMachineSize.name(), virtualMachineSize));
        return sizes;
    }

    /**
     * Return the names of the sizes used by all the virtual machines created
     * @param azure the azure client
     * @return a list of virtual machine size names
     */
    @VisibleForTesting
    List<String> getVirtualMachineSizeNamesInUse(Azure azure) {
        return this.getVirtualMachines(azure)
                .stream()
                .map(virtualMachine -> virtualMachine.size().toString())
                .collect(Collectors.toList());
    }

    /**
     * Return a list of all virtual machines created
     * @param azure the azure client
     * @return a list of virtual machines
     */
    @VisibleForTesting
    PagedList<VirtualMachine> getVirtualMachines(Azure azure) {
        return azure.virtualMachines().list();
    }

    /**
     * Return a list with all virtual machine sizes of the default region
     * @param azure the azure client
     * @return a list of virtual machine sizes
     */
    @VisibleForTesting
    PagedList<VirtualMachineSize> getVirtualMachineSizes(Azure azure) {
        return azure.virtualMachines().sizes().listByRegion(this.defaultRegionName);
    }

    /**
     * Return a list of all disks created
     * @param azure the azure client
     * @return a list of disks
     */
    @VisibleForTesting
    PagedList<Disk> getDisks(Azure azure) {
        return azure.disks().list();
    }
}
