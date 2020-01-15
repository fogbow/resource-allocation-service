package cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk;

import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.NoAvailableResourcesException;
import cloud.fogbow.common.exceptions.UnauthenticatedUserException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.AzureGetVirtualMachineRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.AzureVirtualMachineOperation;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureClientCacheManager;
import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineSize;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.List;

public class AzureVirtualMachineOperationSDK implements AzureVirtualMachineOperation {

    private static final Logger LOGGER = Logger.getLogger(AzureVirtualMachineOperationSDK.class);

    @Override
    public AzureGetVirtualMachineRef doGetInstance(String azureInstanceId, AzureUser azureCloudUser)
            throws UnauthenticatedUserException, UnexpectedException,
            NoAvailableResourcesException, InstanceNotFoundException {

        Azure azure = AzureClientCacheManager.getAzure(azureCloudUser);

        VirtualMachine virtualMachine = AzureVirtualMachineSDK
                .getVirtualMachineById(azure, azureInstanceId)
                .orElseThrow(InstanceNotFoundException::new);
        String virtualMachineSizeName = virtualMachine.size().toString();
        String cloudState = virtualMachine.provisioningState();
        String name = virtualMachine.name();
        String primaryPrivateIp = virtualMachine.getPrimaryNetworkInterface().primaryPrivateIP();
        List<String> ipAddresses = Arrays.asList(primaryPrivateIp);

        String regionName = azureCloudUser.getRegionName();
        VirtualMachineSize virtualMachineSize = findVirtualMachineSizeByName(virtualMachineSizeName, regionName, azure);
        int vCPU = virtualMachineSize.numberOfCores();
        int memory = virtualMachineSize.memoryInMB();
        int disk = virtualMachine.osDiskSize();

        return AzureGetVirtualMachineRef.builder()
                .cloudState(cloudState)
                .ipAddresses(ipAddresses)
                .disk(disk)
                .memory(memory)
                .name(name)
                .vCPU(vCPU)
                .build();
    }

    @VisibleForTesting
    VirtualMachineSize findVirtualMachineSizeByName(String virtualMachineSizeNameWanted,
                                                    String regionName, Azure azure)
            throws NoAvailableResourcesException, UnexpectedException {

        LOGGER.debug(String.format(Messages.Info.SEEK_VIRTUAL_MACHINE_SIZE_BY_NAME, virtualMachineSizeNameWanted, regionName));
        Region region = Region.findByLabelOrName(regionName);
        PagedList<VirtualMachineSize> virtualMachineSizes = AzureVirtualMachineSDK.getVirtualMachineSizes(azure, region);
        return virtualMachineSizes.stream()
                .filter((virtualMachineSize) -> virtualMachineSizeNameWanted.equals(virtualMachineSize.name()))
                .findFirst()
                .orElseThrow(() -> new NoAvailableResourcesException());
    }

}
