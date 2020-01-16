package cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk;

import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.NoAvailableResourcesException;
import cloud.fogbow.common.exceptions.UnauthenticatedUserException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.AzureGetVirtualMachineRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.AzureVirtualMachineOperation;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model.AzureCreateVirtualMachineRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model.AzureGetImageRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.AzureNetworkSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureClientCacheManager;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureSchedulerManager;
import cloud.fogbow.ras.core.plugins.interoperability.azure.volume.sdk.AzureVolumeSDK;
import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineSize;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.resources.fluentcore.model.Indexable;
import org.apache.log4j.Logger;
import rx.Completable;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class AzureVirtualMachineOperationSDK implements AzureVirtualMachineOperation {

    private static final Logger LOGGER = Logger.getLogger(AzureVirtualMachineOperationSDK.class);

    private Scheduler scheduler;

    public AzureVirtualMachineOperationSDK() {
        ExecutorService virtualMachineExecutor = AzureSchedulerManager.getVirtualMachineExecutor();
        this.scheduler = Schedulers.from(virtualMachineExecutor);
    }

    /**
     * Create asynchronously because this operation takes a long time to finish.
     */
    @Override
    public void doCreateInstance(AzureCreateVirtualMachineRef azureCreateVirtualMachineRef,
                                 AzureUser azureCloudUser)
            throws UnauthenticatedUserException, InstanceNotFoundException, UnexpectedException {

        Azure azure = AzureClientCacheManager.getAzure(azureCloudUser);

        Observable<Indexable> virtualMachineAsync = buildAzureVirtualMachineObservable(
                azureCreateVirtualMachineRef, azure);

        subscribeCreateVirtualMachine(virtualMachineAsync);
    }

    @VisibleForTesting
    Observable<Indexable> buildAzureVirtualMachineObservable(
            AzureCreateVirtualMachineRef azureCreateVirtualMachineRef,
            Azure azure) throws InstanceNotFoundException, UnexpectedException {

        String networkInterfaceId = azureCreateVirtualMachineRef.getNetworkInterfaceId();
        NetworkInterface networkInterface = AzureNetworkSDK
                .getNetworkInterface(azure, networkInterfaceId)
                .orElseThrow(InstanceNotFoundException::new);
        String resourceGroupName = azureCreateVirtualMachineRef.getResourceGroupName();
        String regionName = azureCreateVirtualMachineRef.getRegionName();
        String virtualMachineName = azureCreateVirtualMachineRef.getVirtualMachineName();
        String osUserName = azureCreateVirtualMachineRef.getOsUserName();
        String osUserPassword = azureCreateVirtualMachineRef.getOsUserPassword();
        String osComputeName = azureCreateVirtualMachineRef.getOsComputeName();
        String userData = azureCreateVirtualMachineRef.getUserData();
        String size = azureCreateVirtualMachineRef.getSize();
        int diskSize = azureCreateVirtualMachineRef.getDiskSize();
        AzureGetImageRef azureVirtualMachineImage = azureCreateVirtualMachineRef.getAzureGetImageRef();
        Region region = Region.findByLabelOrName(regionName);
        String imagePublished = azureVirtualMachineImage.getPublisher();
        String imageOffer = azureVirtualMachineImage.getOffer();
        String imageSku = azureVirtualMachineImage.getSku();

        return AzureVirtualMachineSDK.buildVirtualMachineObservable(
                azure, virtualMachineName, region, resourceGroupName, networkInterface,
                imagePublished, imageOffer, imageSku, osUserName, osUserPassword, osComputeName,
                userData, diskSize, size);
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

    @Override
    public String findVirtualMachineSizeName(int memoryRequired, int vCpuRequired,
                                             String regionName, AzureUser azureCloudUser)
            throws UnauthenticatedUserException, NoAvailableResourcesException,
            UnexpectedException {

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
                .orElseThrow(() -> new NoAvailableResourcesException());

        return firstVirtualMachineSize.name();
    }

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

    /**
     * Delete asynchronously because this operation takes a long time to finish.
     */
    @Override
    public void doDeleteInstance(String azureInstanceId, AzureUser azureCloudUser)
            throws UnauthenticatedUserException, InstanceNotFoundException, UnexpectedException {

        Azure azure = AzureClientCacheManager.getAzure(azureCloudUser);

        Completable firstDeleteVirtualMachine = buildDeleteVirtualMachineCompletable(azure, azureInstanceId);
        Completable secondDeleteVirtualMachineDisk = buildDeleteVirtualMachineDiskCompletable(azure, azureInstanceId);

        Completable.concat(firstDeleteVirtualMachine, secondDeleteVirtualMachineDisk)
                .subscribeOn(this.scheduler)
                .subscribe();
    }

    @VisibleForTesting
    Completable buildDeleteVirtualMachineDiskCompletable(Azure azure, String azureInstanceId)
            throws InstanceNotFoundException, UnexpectedException {

        VirtualMachine virtualMachine = AzureVirtualMachineSDK
                .getVirtualMachineById(azure, azureInstanceId)
                .orElseThrow(InstanceNotFoundException::new);
        String osDiskId = virtualMachine.osDiskId();
        Completable deleteVirutalMachineDisk = AzureVolumeSDK.buildDeleteDiskCompletable(azure, osDiskId);

        return setDeleteVirtualMachineDiskBehaviour(deleteVirutalMachineDisk);
    }

    @VisibleForTesting
    Completable buildDeleteVirtualMachineCompletable(Azure azure, String azureInstanceId) {
        Completable deleteVirtualMachine = AzureVirtualMachineSDK
                .buildDeleteVirtualMachineCompletable(azure, azureInstanceId);
        return setDeleteVirtualMachineBehaviour(deleteVirtualMachine);
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
