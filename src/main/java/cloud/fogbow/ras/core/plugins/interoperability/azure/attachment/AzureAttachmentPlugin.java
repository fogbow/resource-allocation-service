package cloud.fogbow.ras.core.plugins.interoperability.azure.attachment;

import java.util.Properties;

import org.apache.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.Disk;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineDataDisk;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.AttachmentInstance;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.plugins.interoperability.AttachmentPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.AzureVirtualMachineSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureClientCacheManager;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureGeneralUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureResourceIdBuilder;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.azure.volume.sdk.AzureVolumeSDK;

public class AzureAttachmentPlugin implements AttachmentPlugin<AzureUser> {

private static final Logger LOGGER = Logger.getLogger(AzureAttachmentPlugin.class);
    
    private final String defaultResourceGroupName;
    
    public AzureAttachmentPlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.defaultResourceGroupName = properties.getProperty(AzureConstants.DEFAULT_RESOURCE_GROUP_NAME_KEY);
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
        LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE_S, attachmentOrder.getInstanceId()));
        Azure azure = AzureClientCacheManager.getAzure(azureUser);
        String subscriptionId = azureUser.getSubscriptionId();
        String virtualMachineId = buildResourceId(subscriptionId, attachmentOrder.getComputeId());
        String diskId = buildResourceId(subscriptionId, attachmentOrder.getVolumeId());
        
        return doRequestInstance(azure, virtualMachineId, diskId);
    }

    @VisibleForTesting
    String doRequestInstance(Azure azure, String virtualMachineId, String diskId)
            throws FogbowException {
        
        VirtualMachine virtualMachine = AzureVirtualMachineSDK
                .getVirtualMachine(azure, virtualMachineId)
                .orElseThrow(() -> new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND));
        
        Disk disk = AzureVolumeSDK.getDisk(azure, diskId)
                .orElseThrow(() -> new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND));
        
        virtualMachine.update().withExistingDataDisk(disk).applyAsync(); // FIXME
        return AzureGeneralUtil.defineInstanceId(disk.name());
    }

    @VisibleForTesting
    String buildResourceId(String subscriptionId, String resourceName) {
        String resourceIdUrl = AzureResourceIdBuilder.diskId()
                .withSubscriptionId(subscriptionId)
                .withResourceGroupName(this.defaultResourceGroupName)
                .withResourceName(resourceName)
                .build();
        
        return resourceIdUrl;
    }

    @Override
    public void deleteInstance(AttachmentOrder attachmentOrder, AzureUser azureUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE_S, attachmentOrder.getInstanceId()));
        String subscriptionId = azureUser.getSubscriptionId();
        String virtualMachineId = buildResourceId(subscriptionId, attachmentOrder.getComputeId());
        String diskId = buildResourceId(subscriptionId, attachmentOrder.getVolumeId());
        
        Azure azure = AzureClientCacheManager.getAzure(azureUser);
        VirtualMachine virtualMachine = AzureVirtualMachineSDK
                .getVirtualMachine(azure, virtualMachineId)
                .orElseThrow(() -> new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND));
        
        VirtualMachineDataDisk virtualMachineDataDisk = virtualMachine.dataDisks().values().stream()
                .filter(dataDisk -> diskId.equals(dataDisk.id()))
                .findFirst()
                .orElseThrow(() -> new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND));
        
        int lun = virtualMachineDataDisk.lun();
        virtualMachine.update().withoutDataDisk(lun).applyAsync(); // FIXME
    }

    @Override
    public AttachmentInstance getInstance(AttachmentOrder attachmentOrder, AzureUser azureUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE_S, attachmentOrder.getInstanceId()));
        Azure azure = AzureClientCacheManager.getAzure(azureUser);
        String subscriptionId = azureUser.getSubscriptionId();
        String resourceId = buildResourceId(subscriptionId, attachmentOrder.getInstanceId());
        
        return doGetInstance(azure, resourceId);
    }

    @VisibleForTesting
    AttachmentInstance doGetInstance(Azure azure, String resourceId) throws FogbowException {
        Disk disk = AzureVolumeSDK.getDisk(azure, resourceId)
                .orElseThrow(() -> new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND));

        return buildAttachmentInstance(disk);
    }

    @VisibleForTesting
    AttachmentInstance buildAttachmentInstance(Disk disk) {
        String id = AzureGeneralUtil.defineInstanceId(disk.name());
        String cloudState = disk.isAttachedToVirtualMachine() 
                ? AzureStateMapper.ATTACHED_STATE
                : AzureStateMapper.UNATTACHED_STATE;
        
        String computeId = disk.virtualMachineId();
        String volumeId = disk.id();
        String device = disk.source().sourceId();
        return new AttachmentInstance(id, cloudState, computeId, volumeId, device);
    }

}
