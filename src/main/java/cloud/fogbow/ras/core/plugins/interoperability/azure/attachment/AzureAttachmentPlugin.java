package cloud.fogbow.ras.core.plugins.interoperability.azure.attachment;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.azure.AzureClientCacheManager;
import cloud.fogbow.ras.api.http.response.AttachmentInstance;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.plugins.interoperability.AttachmentPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureAsync;
import cloud.fogbow.ras.core.plugins.interoperability.azure.attachment.sdk.AzureAttachmentOperationSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.attachment.sdk.AzureAttachmentSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.AzureVirtualMachineSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AsyncInstanceCreationManager;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureGeneralUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureResourceIdBuilder;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.azure.volume.sdk.AzureVolumeSDK;
import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.Disk;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineDataDisk;
import com.microsoft.azure.management.compute.implementation.DiskInner;
import org.apache.log4j.Logger;
import rx.Observable;

import java.util.Properties;

public class AzureAttachmentPlugin implements AttachmentPlugin<AzureUser>, AzureAsync<AttachmentInstance> {

    private static final Logger LOGGER = Logger.getLogger(AzureAttachmentPlugin.class);

    private final String defaultResourceGroupName;

    private AzureAttachmentOperationSDK operation;

    public AzureAttachmentPlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.defaultResourceGroupName = properties.getProperty(AzureConstants.DEFAULT_RESOURCE_GROUP_NAME_KEY);
        this.operation = new AzureAttachmentOperationSDK();
    }

    @Override
    public boolean isReady(String instanceState) {
        return AzureStateMapper.map(ResourceType.ATTACHMENT, instanceState).equals(InstanceState.READY);
    }

    @Override
    public boolean hasFailed(String instanceState) {
        return AzureStateMapper.map(ResourceType.ATTACHMENT, instanceState).equals(InstanceState.FAILED);
    }

    @Override
    public String requestInstance(AttachmentOrder attachmentOrder, AzureUser azureUser) throws FogbowException {
        LOGGER.info(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER);
        Azure azure = AzureClientCacheManager.getAzure(azureUser);
        String subscriptionId = azureUser.getSubscriptionId();
        String virtualMachineName = AzureGeneralUtil.defineResourceName(attachmentOrder.getComputeId());
        String virtualMachineId = buildVirtualMachineId(azure, subscriptionId, virtualMachineName);
        String diskName = AzureGeneralUtil.defineResourceName(attachmentOrder.getVolumeId());
        String diskId = buildResourceId(azure, subscriptionId, diskName);
        
        return doRequestInstance(azure, virtualMachineId, diskId);
    }

    @Override
    public AttachmentInstance getInstance(AttachmentOrder attachmentOrder, AzureUser azureUser) throws FogbowException {
        String instanceId = attachmentOrder.getInstanceId();
        LOGGER.info(String.format(Messages.Log.GETTING_INSTANCE_S, instanceId));

        AttachmentInstance creatingInstance = getCreatingInstance(instanceId);
        if (creatingInstance != null) {
            return creatingInstance;
        }

        Azure azure = AzureClientCacheManager.getAzure(azureUser);
        String subscriptionId = azureUser.getSubscriptionId();
        String resourceName = AzureGeneralUtil.defineResourceName(instanceId);
        String resourceId = buildResourceId(azure, subscriptionId, resourceName);
        
        return doGetInstance(azure, resourceId);
    }

    @Override
    public void deleteInstance(AttachmentOrder attachmentOrder, AzureUser azureUser) throws FogbowException {
        String instanceId = attachmentOrder.getInstanceId();
        LOGGER.info(String.format(Messages.Log.DELETING_INSTANCE_S, instanceId));
        Azure azure = AzureClientCacheManager.getAzure(azureUser);
        String subscriptionId = azureUser.getSubscriptionId();
        String virtualMachineName = AzureGeneralUtil.defineResourceName(attachmentOrder.getComputeId());
        String virtualMachineId = buildVirtualMachineId(azure, subscriptionId, virtualMachineName);
        String resourceName = AzureGeneralUtil.defineResourceName(instanceId);
        String resourceId = buildResourceId(azure, subscriptionId, resourceName);

        doDeleteInstance(azure, virtualMachineId, resourceId);
        endInstanceCreation(instanceId);
    }

    @VisibleForTesting
    void doDeleteInstance(Azure azure, String virtualMachineId, String resourceId) throws FogbowException {
        VirtualMachine virtualMachine = doGetVirtualMachineSDK(azure, virtualMachineId);
        VirtualMachineDataDisk virtualMachineDataDisk = findVirtualMachineDataDisk(virtualMachine, resourceId);
        int lun = virtualMachineDataDisk.lun();

        Observable<VirtualMachine> observable = AzureAttachmentSDK.detachDisk(virtualMachine, lun);
        this.operation.subscribeDetachDiskFrom(observable);
    }

    @VisibleForTesting
    VirtualMachineDataDisk findVirtualMachineDataDisk(VirtualMachine virtualMachine, String resourceId)
            throws FogbowException {

        return virtualMachine.dataDisks().values().stream()
                .filter(dataDisk -> resourceId.equals(dataDisk.id()))
                .findFirst()
                .orElseThrow(() -> new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND));
    }

    @VisibleForTesting
    AttachmentInstance doGetInstance(Azure azure, String resourceId) throws FogbowException {
        Disk disk = doGetDiskSDK(azure, resourceId);
        AttachmentInstance attachmentInstance = buildAttachmentInstance(disk);
        // It happens because o Fogbow Core waits an InstanceNotFoundException as instance unattached.
        if (attachmentInstance.getCloudState().equals(AzureStateMapper.UNATTACHED_STATE)) {
            throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
        }
        return attachmentInstance;
    }

    @VisibleForTesting
    AttachmentInstance buildAttachmentInstance(Disk disk) {
        DiskInner diskInner = disk.inner();
        String id = diskInner.id();
        String cloudState = disk.isAttachedToVirtualMachine()
                ? AzureStateMapper.ATTACHED_STATE
                : AzureStateMapper.UNATTACHED_STATE;

        String computeId = disk.virtualMachineId();
        String volumeId = disk.id();
        String device = AzureGeneralUtil.NO_INFORMATION;
        return new AttachmentInstance(id, cloudState, computeId, volumeId, device);
    }

    @VisibleForTesting
    String doRequestInstance(Azure azure, String virtualMachineId, String diskId) throws FogbowException {
        VirtualMachine virtualMachine = doGetVirtualMachineSDK(azure, virtualMachineId);
        Disk disk = doGetDiskSDK(azure, diskId);

        Observable<VirtualMachine> observable = AzureAttachmentSDK.attachDisk(virtualMachine, disk);
        String instanceId = AzureGeneralUtil.defineInstanceId(disk.name());
        AsyncInstanceCreationManager.Callbacks finishCreationCallbacks = startInstanceCreation(instanceId);
        this.operation.subscribeAttachDiskFrom(observable, finishCreationCallbacks);
        waitAndCheckForInstanceCreationFailed(instanceId);
        return instanceId;
    }

    @VisibleForTesting
    Disk doGetDiskSDK(Azure azure, String diskId) throws FogbowException {
        return AzureVolumeSDK
                .getDisk(azure, diskId)
                .orElseThrow(() -> new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND));
    }

    @VisibleForTesting
    VirtualMachine doGetVirtualMachineSDK(Azure azure, String virtualMachineId) throws FogbowException {
        return AzureVirtualMachineSDK
                .getVirtualMachine(azure, virtualMachineId)
                .orElseThrow(() -> new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND));
    }

    @VisibleForTesting
    String buildResourceId(Azure azure, String subscriptionId, String resourceName) {
        String resourceGroupName = AzureGeneralUtil
                .selectResourceGroupName(azure, resourceName, this.defaultResourceGroupName);

        String resourceId = AzureResourceIdBuilder.diskId()
                .withSubscriptionId(subscriptionId)
                .withResourceGroupName(resourceGroupName)
                .withResourceName(resourceName)
                .build();

        return resourceId;
    }

    @VisibleForTesting
    String buildVirtualMachineId(Azure azure, String subscriptionId, String virtualMachineName) {
        String resourceGroupName = AzureGeneralUtil
                .selectResourceGroupName(azure, virtualMachineName, this.defaultResourceGroupName);

        String virtualMachineId = AzureResourceIdBuilder.virtualMachineId()
                .withSubscriptionId(subscriptionId)
                .withResourceGroupName(resourceGroupName)
                .withResourceName(virtualMachineName)
                .build();

        return virtualMachineId;
    }

    @VisibleForTesting
    void setOperation(AzureAttachmentOperationSDK operation) {
        this.operation = operation;
    }

    @Override
    public AttachmentInstance buildCreatingInstance(String instanceId) {
        return new AttachmentInstance(instanceId);
    }
}
