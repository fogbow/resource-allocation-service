package cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.apache.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineSize;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.resources.fluentcore.model.Indexable;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.NoAvailableResourcesException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model.AzureCreateVirtualMachineRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model.AzureGetImageRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model.AzureGetVirtualMachineRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.AzureNetworkSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureClientCacheManager;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureGeneralUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureResourceGroupOperationUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureResourceIdBuilder;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureSchedulerManager;
import cloud.fogbow.ras.core.plugins.interoperability.azure.volume.sdk.AzureVolumeSDK;
import rx.Completable;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

public class AzureVirtualMachineOperationSDK {

    private static final Logger LOGGER = Logger.getLogger(AzureVirtualMachineOperationSDK.class);
    private String regionName;

    private Scheduler scheduler;

    public AzureVirtualMachineOperationSDK(String regionName) {
        ExecutorService virtualMachineExecutor = AzureSchedulerManager.getVirtualMachineExecutor();
        this.scheduler = Schedulers.from(virtualMachineExecutor);
        this.regionName = regionName;
    }

    /**
     * Create asynchronously because this operation takes a long time to finish.
     */
    public void doCreateInstance(AzureCreateVirtualMachineRef virtualMachineRef, AzureUser azureUser)
            throws FogbowException {

        Azure azure = AzureClientCacheManager.getAzure(azureUser);
        Observable<Indexable> virtualMachineAsync = buildAzureVirtualMachineObservable(virtualMachineRef, azure);
        subscribeCreateVirtualMachine(virtualMachineAsync);
    }

    @VisibleForTesting
    Observable<Indexable> buildAzureVirtualMachineObservable(
            AzureCreateVirtualMachineRef virtualMachineRef,
            Azure azure) throws FogbowException {

        String virtualNetworkId = virtualMachineRef.getVirtualNetworkId();
        Network network = AzureNetworkSDK.getNetwork(azure, virtualNetworkId)
                .orElseThrow(() -> new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND));
        
        String subnetName = network.subnets().values().iterator().next().name();
        String resourceName = virtualMachineRef.getResourceName();
        String regionName = virtualMachineRef.getRegionName();
        // FIXME rename getResourceGroupName to getDefaultResourceGroupName method
        String defaultResourceGroupName = virtualMachineRef.getResourceGroupName();
        String resourceGroupName = AzureGeneralUtil
                .defineResourceGroupName(azure, regionName, resourceName, defaultResourceGroupName);

        String osUserName = virtualMachineRef.getOsUserName();
        String osUserPassword = virtualMachineRef.getOsUserPassword();
        String osComputeName = virtualMachineRef.getOsComputeName();
        String userData = virtualMachineRef.getUserData();
        String size = virtualMachineRef.getSize();
        int diskSize = virtualMachineRef.getDiskSize();
        AzureGetImageRef azureImage = virtualMachineRef.getAzureGetImageRef();
        Region region = Region.findByLabelOrName(regionName);
        String imagePublished = azureImage.getPublisher();
        String imageOffer = azureImage.getOffer();
        String imageSku = azureImage.getSku();
        Map tags = virtualMachineRef.getTags();

        return AzureVirtualMachineSDK.buildVirtualMachineObservable(
                azure, resourceName, region, resourceGroupName, network, subnetName,
                imagePublished, imageOffer, imageSku, osUserName, osUserPassword, osComputeName,
                userData, diskSize, size, tags);
    }

    /**
     * Execute create Virtual Machine observable and set its behaviour.
     */
    @VisibleForTesting
    void subscribeCreateVirtualMachine(Observable<Indexable> virtualMachineObservable) {
        setCreateVirtualMachineBehaviour(virtualMachineObservable)
                .subscribeOn(this.scheduler)
                .subscribe();
    }

    private Observable<Indexable> setCreateVirtualMachineBehaviour(Observable<Indexable> virtualMachineObservable) {
        return virtualMachineObservable
                .onErrorReturn((error -> {
                    LOGGER.error(Messages.Error.ERROR_CREATE_VM_ASYNC_BEHAVIOUR, error);
                    return null;
                }))
                .doOnCompleted(() -> {
                    LOGGER.info(Messages.Info.END_CREATE_VM_ASYNC_BEHAVIOUR);
                });
    }

    public VirtualMachineSize findVirtualMachineSize(int memoryRequired, int vCpuRequired, String regionName,
            AzureUser azureCloudUser) throws FogbowException {

        LOGGER.debug(String.format(Messages.Info.SEEK_VIRTUAL_MACHINE_SIZE_NAME, memoryRequired, vCpuRequired, regionName));
        Azure azure = AzureClientCacheManager.getAzure(azureCloudUser);
        Region region = Region.findByLabelOrName(regionName);
        
        PagedList<VirtualMachineSize> virtualMachineSizes =
                AzureVirtualMachineSDK.getVirtualMachineSizes(azure, region);
        
        VirtualMachineSize firstVirtualMachineSize = virtualMachineSizes.stream()
                .filter((virtualMachineSize) ->
                        virtualMachineSize.memoryInMB() >= memoryRequired &&
                                virtualMachineSize.numberOfCores() >= vCpuRequired
                )
                .sorted(Comparator
                        .comparingInt(VirtualMachineSize::memoryInMB)
                        .thenComparingInt(VirtualMachineSize::numberOfCores))
                .findFirst()
                .orElseThrow(NoAvailableResourcesException::new);

        return firstVirtualMachineSize;
    }

    public AzureGetVirtualMachineRef doGetInstance(AzureUser azureCloudUser,
            String resourceName, String defaultResourceGroupName) throws FogbowException {

        Azure azure = AzureClientCacheManager.getAzure(azureCloudUser);
        String subscriptionId = azureCloudUser.getSubscriptionId();
        String resourceGroupName = AzureGeneralUtil
                .selectResourceGroupName(azure, resourceName, defaultResourceGroupName);

        String resourceId = AzureResourceIdBuilder.virtualMachineId()
                .withSubscriptionId(subscriptionId)
                .withResourceGroupName(resourceGroupName)
                .withResourceName(resourceName)
                .build();

        VirtualMachine virtualMachine = AzureVirtualMachineSDK
                .getVirtualMachine(azure, resourceId)
                .orElseThrow(() -> new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND));
        
        String virtualMachineSizeName = virtualMachine.size().toString();
        String cloudState = virtualMachine.provisioningState();
        String name = virtualMachine.name();
        String primaryPrivateIp = virtualMachine.getPrimaryNetworkInterface().primaryPrivateIP();
        List<String> ipAddresses = Arrays.asList(primaryPrivateIp);
        Map tags = virtualMachine.tags();

        VirtualMachineSize virtualMachineSize = findVirtualMachineSize(virtualMachineSizeName, this.regionName, azure);
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
                .tags(tags)
                .build();
    }

    @VisibleForTesting
    VirtualMachineSize findVirtualMachineSize(String virtualMachineSizeWanted, String regionName, Azure azure)
            throws FogbowException {

        LOGGER.debug(String.format(Messages.Info.SEEK_VIRTUAL_MACHINE_SIZE_BY_NAME, virtualMachineSizeWanted, regionName));
        Region region = Region.findByLabelOrName(regionName);
        
        PagedList<VirtualMachineSize> virtualMachineSizes = AzureVirtualMachineSDK.getVirtualMachineSizes(azure, region);
        return virtualMachineSizes.stream()
                .filter((virtualMachineSize) -> virtualMachineSizeWanted.equals(virtualMachineSize.name()))
                .findFirst()
                .orElseThrow(NoAvailableResourcesException::new);
    }

    /**
     * Delete asynchronously because this operation takes a long time to finish.
     */
    public void doDeleteInstance(AzureUser azureCloudUser, String resourceName, String defaultResourceGroupName) throws FogbowException {
        Azure azure = AzureClientCacheManager.getAzure(azureCloudUser);
        
        if (AzureResourceGroupOperationUtil.existsResourceGroup(azure, resourceName)) {
            Completable deleteVirtualMachineCompletable = AzureResourceGroupOperationUtil.deleteResourceGroupAsync(azure, resourceName);
            setDeleteVirtualMachineBehaviour(deleteVirtualMachineCompletable)
                    .subscribeOn(this.scheduler)
                    .subscribe();
        } else {
            String subscriptionId = azureCloudUser.getSubscriptionId();
            String resourceGroupName = AzureGeneralUtil
                    .selectResourceGroupName(azure, resourceName, defaultResourceGroupName);

            String resourceId = AzureResourceIdBuilder.virtualMachineId()
                    .withSubscriptionId(subscriptionId)
                    .withResourceGroupName(resourceGroupName)
                    .withResourceName(resourceName)
                    .build();

            VirtualMachine virtualMachine = AzureVirtualMachineSDK
                    .getVirtualMachine(azure, resourceId)
                    .orElseThrow(() -> new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND));

            Completable firstDeleteVirtualMachine = buildDeleteVirtualMachineCompletable(azure, resourceId);
            Completable secondDeleteVirtualMachineDisk = buildDeleteDiskCompletable(azure, virtualMachine);
            Completable thirdDeleteVirtualMachineNic = buildDeleteNicCompletable(azure, virtualMachine);
            Completable.concat(firstDeleteVirtualMachine, secondDeleteVirtualMachineDisk, thirdDeleteVirtualMachineNic)
                    .subscribeOn(this.scheduler)
                    .subscribe();
        }
    }

    @VisibleForTesting
    Completable buildDeleteNicCompletable(Azure azure, VirtualMachine virtualMachine) throws FogbowException {
        String networkInterfaceId = virtualMachine.primaryNetworkInterfaceId();
        Completable deleteVirtualMachineNic = AzureNetworkSDK
                .buildDeleteNetworkInterfaceCompletable(azure, networkInterfaceId);

        return setDeleteVirtualMachineNicBehaviour(deleteVirtualMachineNic);
    }

    @VisibleForTesting
    Completable buildDeleteDiskCompletable(Azure azure, VirtualMachine virtualMachine) throws FogbowException {
        String osDiskId = virtualMachine.osDiskId();
        Completable deleteVirutalMachineDisk = AzureVolumeSDK
                .buildDeleteDiskCompletable(azure, osDiskId);

        return setDeleteVirtualMachineDiskBehaviour(deleteVirutalMachineDisk);
    }

    @VisibleForTesting
    Completable buildDeleteVirtualMachineCompletable(Azure azure, String azureInstanceId) {
        Completable deleteVirtualMachine = AzureVirtualMachineSDK
                .buildDeleteVirtualMachineCompletable(azure, azureInstanceId);
        
        return setDeleteVirtualMachineBehaviour(deleteVirtualMachine);
    }

    private Completable setDeleteVirtualMachineNicBehaviour(Completable deleteVirtualMachineNic) {
        return deleteVirtualMachineNic
                .doOnError((error -> {
                    LOGGER.error(Messages.Error.ERROR_DELETE_NIC_ASYNC_BEHAVIOUR);
                }))
                .doOnCompleted(() -> {
                    LOGGER.info(Messages.Info.END_DELETE_NIC_ASYNC_BEHAVIOUR);
                });
    }

    private Completable setDeleteVirtualMachineDiskBehaviour(Completable deleteVirutalMachineDisk) {
        return deleteVirutalMachineDisk
                .doOnError((error -> {
                    LOGGER.error(Messages.Error.ERROR_DELETE_DISK_ASYNC_BEHAVIOUR);
                }))
                .doOnCompleted(() -> {
                    LOGGER.info(Messages.Info.END_DELETE_DISK_ASYNC_BEHAVIOUR);
                });
    }

    private Completable setDeleteVirtualMachineBehaviour(Completable deleteVirtualMachineCompletable) {
        return deleteVirtualMachineCompletable
                .doOnError((error -> {
                    LOGGER.error(Messages.Error.ERROR_DELETE_VM_ASYNC_BEHAVIOUR, error);
                }))
                .doOnCompleted(() -> {
                    LOGGER.info(Messages.Info.END_DELETE_VM_ASYNC_BEHAVIOUR);
                });
    }

    @VisibleForTesting
    void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

}
