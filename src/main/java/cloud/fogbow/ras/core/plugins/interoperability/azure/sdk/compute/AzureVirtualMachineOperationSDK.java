package cloud.fogbow.ras.core.plugins.interoperability.azure.sdk.compute;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.UnacceptableOperationException;
import cloud.fogbow.common.exceptions.UnauthenticatedUserException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.util.connectivity.cloud.azure.AzureClientCacheManager;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.azure.sdk.compute.model.AzureCreateVirtualMachineRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.sdk.compute.model.AzureGetImageRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.sdk.compute.model.AzureGetVirtualMachineRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.sdk.network.AzureNetworkSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.*;
import cloud.fogbow.ras.core.plugins.interoperability.azure.sdk.volume.AzureVolumeSDK;
import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.PowerState;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineSize;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.network.PublicIPAddress;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.resources.fluentcore.model.Indexable;
import org.apache.log4j.Logger;
import rx.Completable;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import java.util.*;
import java.util.concurrent.ExecutorService;

public class AzureVirtualMachineOperationSDK {

    private static final Logger LOGGER = Logger.getLogger(AzureVirtualMachineOperationSDK.class);
    private String regionName;
    private String defaultResourceGroupName;

    private Scheduler scheduler;

    public AzureVirtualMachineOperationSDK(String regionName, String defaultResourceGroupName) {
        ExecutorService virtualMachineExecutor = AzureSchedulerManager.getVirtualMachineExecutor();
        this.scheduler = Schedulers.from(virtualMachineExecutor);
        this.regionName = regionName;
        this.defaultResourceGroupName = defaultResourceGroupName;
    }

    /**
     * Create asynchronously because this operation takes a long time to finish.
     */
    public void doCreateInstance(AzureCreateVirtualMachineRef virtualMachineRef,
                                 AsyncInstanceCreationManager.Callbacks finishCreationCallbacks,
                                 AzureUser azureUser) throws FogbowException {

        Azure azure = AzureClientCacheManager.getAzure(azureUser);
        String subscriptionId = azureUser.getSubscriptionId();
        String virtualNetworkName = virtualMachineRef.getVirtualNetworkName();
        String networkId = buildNetworkId(azure, subscriptionId, virtualNetworkName);

        Observable<Indexable> virtualMachineObservable = buildAzureVirtualMachineObservable(azure, virtualMachineRef, networkId);
        subscribeCreateVirtualMachine(virtualMachineObservable, finishCreationCallbacks);
    }

    @VisibleForTesting
    Observable<Indexable> buildAzureVirtualMachineObservable(Azure azure,
            AzureCreateVirtualMachineRef virtualMachineRef, String virtualNetworkId) throws FogbowException {

        Network network = AzureNetworkSDK
                .getNetwork(azure, virtualNetworkId)
                .orElseThrow(() -> new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND));
        
        String subnetName = network.subnets().values().iterator().next().name();
        String resourceName = virtualMachineRef.getResourceName();
        String regionName = virtualMachineRef.getRegionName();
        String resourceGroupName = AzureGeneralUtil
                .defineResourceGroupName(azure, regionName, resourceName, this.defaultResourceGroupName);

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
    void subscribeCreateVirtualMachine(Observable<Indexable> virtualMachineObservable,
                                       AsyncInstanceCreationManager.Callbacks finishCreationCallbacks) {

        setCreateVirtualMachineBehaviour(virtualMachineObservable, finishCreationCallbacks)
                .subscribeOn(this.scheduler)
                .subscribe();
    }

    private Observable<Indexable> setCreateVirtualMachineBehaviour(Observable<Indexable> virtualMachineObservable,
                                                                   AsyncInstanceCreationManager.Callbacks finishCreationCallbacks) {

        return virtualMachineObservable
                .onErrorReturn((error -> {
                    finishCreationCallbacks.runOnError(error.getMessage());
                    LOGGER.error(Messages.Log.ERROR_CREATE_VM_ASYNC_BEHAVIOUR, error);
                    return null;
                }))
                .doOnCompleted(() -> {
                    finishCreationCallbacks.runOnComplete();
                    LOGGER.info(Messages.Log.END_CREATE_VM_ASYNC_BEHAVIOUR);
                });
    }

    @VisibleForTesting
    String buildNetworkId(Azure azure, String subscriptionId, String virtualNetworkName) {
        String resourceGroupName = AzureGeneralUtil
                .selectResourceGroupName(azure, virtualNetworkName, this.defaultResourceGroupName);

        return AzureResourceIdBuilder.networkId()
                .withSubscriptionId(subscriptionId)
                .withResourceGroupName(resourceGroupName)
                .withResourceName(virtualNetworkName)
                .build();
    }

    public VirtualMachineSize findVirtualMachineSize(int memoryRequired, int vCpuRequired, String regionName,
            AzureUser azureUser) throws FogbowException {

        LOGGER.debug(String.format(Messages.Log.SEEK_VIRTUAL_MACHINE_SIZE_NAME_S_S, memoryRequired, vCpuRequired, regionName));
        Azure azure = AzureClientCacheManager.getAzure(azureUser);
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
                .orElseThrow(UnacceptableOperationException::new);

        return firstVirtualMachineSize;
    }

    public AzureGetVirtualMachineRef doGetInstance(AzureUser azureUser, String resourceName)
            throws FogbowException {

        Azure azure = AzureClientCacheManager.getAzure(azureUser);
        String subscriptionId = azureUser.getSubscriptionId();
        String resourceId = buildResourceId(azure, subscriptionId, resourceName);

        VirtualMachine virtualMachine = AzureVirtualMachineSDK
                .getVirtualMachine(azure, resourceId)
                .orElseThrow(() -> new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND));
        
        String virtualMachineSizeName = virtualMachine.size().toString();
        String cloudState = getCloudState(virtualMachine);

        String id = virtualMachine.inner().id();
        Map tags = virtualMachine.tags();

        List<String> ipAddresses = new ArrayList<>();
        String primaryPrivateIp = virtualMachine.getPrimaryNetworkInterface().primaryPrivateIP();
        ipAddresses.add(primaryPrivateIp);

        PublicIPAddress primaryPublicIPAddress = virtualMachine.getPrimaryPublicIPAddress();
        if (primaryPublicIPAddress != null) {
            String publicIp = primaryPublicIPAddress.ipAddress();
            ipAddresses.add(publicIp);
        }

        VirtualMachineSize virtualMachineSize = findVirtualMachineSize(virtualMachineSizeName, this.regionName, azure);
        int vCPU = virtualMachineSize.numberOfCores();
        int memory = virtualMachineSize.memoryInMB();
        int disk = virtualMachine.osDiskSize();

        return AzureGetVirtualMachineRef.builder()
                .id(id)
                .cloudState(cloudState)
                .ipAddresses(ipAddresses)
                .disk(disk)
                .memory(memory)
                .vCPU(vCPU)
                .tags(tags)
                .build();
    }

    private String getCloudState(VirtualMachine virtualMachine) {
        String cloudState = virtualMachine.provisioningState();
        
        // The virtual machine provisioning state does not represent
        // well virtual machine state changes such as stopping and deallocating.
        // Therefore, when a virtual machine is being deallocated or started,
        // we must use the power state as the cloud state instead.
        PowerState powerState = virtualMachine.powerState();
        
        if (powerState.equals(PowerState.DEALLOCATING)) {
            cloudState = AzureStateMapper.DEALLOCATING_STATE;
        } else if (powerState.equals(PowerState.DEALLOCATED)) {
            cloudState = AzureStateMapper.DEALLOCATED_STATE;
        } else if (powerState.equals(PowerState.STARTING)) {
            cloudState = AzureStateMapper.STARTING_STATE;
        }
        
        return cloudState;
    }
    
    @VisibleForTesting
    VirtualMachineSize findVirtualMachineSize(String virtualMachineSizeWanted, String regionName, Azure azure)
            throws FogbowException {

        LOGGER.debug(String.format(Messages.Log.SEEK_VIRTUAL_MACHINE_SIZE_BY_NAME_S_S, virtualMachineSizeWanted, regionName));
        Region region = Region.findByLabelOrName(regionName);
        
        PagedList<VirtualMachineSize> virtualMachineSizes = AzureVirtualMachineSDK.getVirtualMachineSizes(azure, region);
        return virtualMachineSizes.stream()
                .filter((virtualMachineSize) -> virtualMachineSizeWanted.equals(virtualMachineSize.name()))
                .findFirst()
                .orElseThrow(UnacceptableOperationException::new);
    }

    @VisibleForTesting
    String buildResourceId(Azure azure, String subscriptionId, String resourceName) {
        String resourceGroupName = AzureGeneralUtil
                .selectResourceGroupName(azure, resourceName, this.defaultResourceGroupName);

        return AzureResourceIdBuilder.virtualMachineId()
                .withSubscriptionId(subscriptionId)
                .withResourceGroupName(resourceGroupName)
                .withResourceName(resourceName)
                .build();
    }

    /**
     * Delete asynchronously because this operation takes a long time to finish.
     */
    public void doDeleteInstance(AzureUser azureUser, String resourceName) throws FogbowException {
        Azure azure = AzureClientCacheManager.getAzure(azureUser);
        if (AzureResourceGroupOperationUtil.existsResourceGroup(azure, resourceName)) {
            doDeleteResourceGroupAsync(azure, resourceName);
        } else {
            String subscriptionId = azureUser.getSubscriptionId();
            String resourceId = buildResourceId(azure, subscriptionId, resourceName);
            doDeleteVirtualMachineAndResourcesAsync(azure, resourceId);
        }
    }

    @VisibleForTesting
    void doDeleteVirtualMachineAndResourcesAsync(Azure azure, String resourceId) throws FogbowException {
        VirtualMachine virtualMachine = AzureVirtualMachineSDK
                .getVirtualMachine(azure, resourceId)
                .orElseThrow(() -> new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND));

        Completable deleteVirtualMachine = buildDeleteVirtualMachineCompletable(azure, resourceId);
        Completable deleteVirtualMachineDisk = buildDeleteDiskCompletable(azure, virtualMachine);
        Completable deleteVirtualMachineNic = buildDeleteNicCompletable(azure, virtualMachine);

        Completable.concat(deleteVirtualMachine, deleteVirtualMachineDisk, deleteVirtualMachineNic)
                .subscribeOn(this.scheduler)
                .subscribe();
    }

    @VisibleForTesting
    void doDeleteResourceGroupAsync(Azure azure, String resourceName) {
        Completable completable = AzureResourceGroupOperationUtil
                .deleteResourceGroupAsync(azure, resourceName);

        subscribeDeleteVirtualMachine(completable);
    }

    @VisibleForTesting
    void subscribeDeleteVirtualMachine(Completable completable) {
        setDeleteVirtualMachineBehaviour(completable)
                .subscribeOn(this.scheduler)
                .subscribe();
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
    Completable buildDeleteVirtualMachineCompletable(Azure azure, String resourceId) {
        Completable deleteVirtualMachine = AzureVirtualMachineSDK
                .buildDeleteVirtualMachineCompletable(azure, resourceId);
        
        return setDeleteVirtualMachineBehaviour(deleteVirtualMachine);
    }

    private Completable setDeleteVirtualMachineNicBehaviour(Completable deleteVirtualMachineNic) {
        return deleteVirtualMachineNic
                .doOnError((error -> {
                    LOGGER.error(Messages.Log.ERROR_DELETE_NIC_ASYNC_BEHAVIOUR);
                }))
                .doOnCompleted(() -> {
                    LOGGER.info(Messages.Log.END_DELETE_NIC_ASYNC_BEHAVIOUR);
                });
    }

    private Completable setDeleteVirtualMachineDiskBehaviour(Completable deleteVirutalMachineDisk) {
        return deleteVirutalMachineDisk
                .doOnError((error -> {
                    LOGGER.error(Messages.Log.ERROR_DELETE_DISK_ASYNC_BEHAVIOUR);
                }))
                .doOnCompleted(() -> {
                    LOGGER.info(Messages.Log.END_DELETE_DISK_ASYNC_BEHAVIOUR);
                });
    }

    @VisibleForTesting
    Completable setDeleteVirtualMachineBehaviour(Completable deleteVirtualMachineCompletable) {
        return deleteVirtualMachineCompletable
                .doOnError((error -> {
                    LOGGER.error(Messages.Log.ERROR_DELETE_VM_ASYNC_BEHAVIOUR, error);
                }))
                .doOnCompleted(() -> {
                    LOGGER.info(Messages.Log.END_DELETE_VM_ASYNC_BEHAVIOUR);
                });
    }

    @VisibleForTesting
    void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

	public void doStopInstance(AzureUser azureUser, String resourceName) throws UnauthenticatedUserException {
		Azure azure = AzureClientCacheManager.getAzure(azureUser);
		
		String subscriptionId = azureUser.getSubscriptionId();
		String resourceId = buildResourceId(azure, subscriptionId, resourceName);

        Completable stopVirtualMachine = AzureVirtualMachineSDK
                .buildStopVirtualMachineCompletable(azure, resourceId);
        
        stopVirtualMachine = setStopVirtualMachineBehaviour(stopVirtualMachine);

        Completable.concat(stopVirtualMachine)
                .subscribeOn(this.scheduler)
                .subscribe();
	}
	
	public void doResumeInstance(AzureUser azureUser, String resourceName) throws UnauthenticatedUserException {
		Azure azure = AzureClientCacheManager.getAzure(azureUser);
		
		String subscriptionId = azureUser.getSubscriptionId();
		String resourceId = buildResourceId(azure, subscriptionId, resourceName);

        Completable resumeVirtualMachine = AzureVirtualMachineSDK
                .buildResumeVirtualMachineCompletable(azure, resourceId);
        
        resumeVirtualMachine = setResumeVirtualMachineBehaviour(resumeVirtualMachine);

        Completable.concat(resumeVirtualMachine)
                .subscribeOn(this.scheduler)
                .subscribe();
	}

	private Completable setStopVirtualMachineBehaviour(Completable stopVirtualMachineCompletable) {
		return stopVirtualMachineCompletable.doOnError((error -> {
			LOGGER.error(Messages.Log.ERROR_STOP_VM_ASYNC_BEHAVIOUR, error);
		})).doOnCompleted(() -> {
			LOGGER.info(Messages.Log.END_STOP_VM_ASYNC_BEHAVIOUR);
		});
	}
	
	private Completable setResumeVirtualMachineBehaviour(Completable resumeVirtualMachineCompletable) {
		return resumeVirtualMachineCompletable.doOnError((error -> {
			LOGGER.error(Messages.Log.ERROR_RESUME_VM_ASYNC_BEHAVIOUR, error);
		})).doOnCompleted(() -> {
			LOGGER.info(Messages.Log.END_RESUME_VM_ASYNC_BEHAVIOUR);
		});
	}
}
