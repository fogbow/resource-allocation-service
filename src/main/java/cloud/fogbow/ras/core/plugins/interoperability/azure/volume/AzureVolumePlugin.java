package cloud.fogbow.ras.core.plugins.interoperability.azure.volume;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.Disk;
import com.microsoft.azure.management.resources.fluentcore.model.Creatable;
import com.microsoft.azure.management.resources.fluentcore.model.Indexable;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.VolumeInstance;
import cloud.fogbow.ras.api.http.response.quotas.allocation.VolumeAllocation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.VolumePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureClientCacheManager;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureGeneralUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureResourceIdBuilder;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.azure.volume.sdk.AzureVolumeOperationSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.volume.sdk.AzureVolumeSDK;
import rx.Completable;
import rx.Observable;

public class AzureVolumePlugin implements VolumePlugin<AzureUser>{

    private static final Logger LOGGER = Logger.getLogger(AzureVolumePlugin.class);
    
    private final String defaultRegionName;
    private final String defaultResourceGroupName;
    
    private AzureVolumeOperationSDK operation;

    public AzureVolumePlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.defaultRegionName = properties.getProperty(AzureConstants.DEFAULT_REGION_NAME_KEY);
        this.defaultResourceGroupName = properties.getProperty(AzureConstants.DEFAULT_RESOURCE_GROUP_NAME_KEY);
        this.operation = new AzureVolumeOperationSDK();
    }

    @Override
    public boolean isReady(String instanceState) {
        return AzureStateMapper.map(ResourceType.VOLUME, instanceState).equals(InstanceState.READY);
    }

    @Override
    public boolean hasFailed(String instanceState) {
        return AzureStateMapper.map(ResourceType.VOLUME, instanceState).equals(InstanceState.FAILED);
    }

    @Override
    public String requestInstance(VolumeOrder volumeOrder, AzureUser azureUser) throws FogbowException {
        LOGGER.info(Messages.Info.REQUESTING_INSTANCE_FROM_PROVIDER);
        Azure azure = AzureClientCacheManager.getAzure(azureUser);
        String resourceName = AzureGeneralUtil.generateResourceName();
        int sizeInGB = volumeOrder.getVolumeSize();
        Map tags = Collections.singletonMap(AzureConstants.TAG_NAME, volumeOrder.getName());
        
        Creatable<Disk> diskCreatable = azure.disks().define(resourceName)
                .withRegion(this.defaultRegionName)
                .withExistingResourceGroup(this.defaultResourceGroupName)
                .withData()
                .withSizeInGB(sizeInGB)
                .withTags(tags);
        
        return doRequestInstance(volumeOrder, diskCreatable);
    }

    @Override
    public VolumeInstance getInstance(VolumeOrder volumeOrder, AzureUser azureUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE_S, volumeOrder.getInstanceId()));
        Azure azure = AzureClientCacheManager.getAzure(azureUser);
        String resourceName = AzureGeneralUtil.defineResourceName(volumeOrder.getInstanceId());
        String subscriptionId = azureUser.getSubscriptionId();
        String resourceId = buildResourceId(subscriptionId, resourceName);
        
        return doGetInstance(azure, resourceId);
    }
    
    @Override
    public void deleteInstance(VolumeOrder volumeOrder, AzureUser azureUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE_S, volumeOrder.getInstanceId()));
        Azure azure = AzureClientCacheManager.getAzure(azureUser);
        String resourceName = AzureGeneralUtil.defineResourceName(volumeOrder.getInstanceId());
        String subscriptionId = azureUser.getSubscriptionId();
        String resourceId = buildResourceId(subscriptionId, resourceName);
        
        doDeleteInstance(azure, resourceId); 
    }

    private void doDeleteInstance(Azure azure, String instanceId) {
        Completable completable = AzureVolumeSDK.buildDeleteDiskCompletable(azure, instanceId);
        this.operation.subscribeDeleteDisk(completable);
    }
    

    @VisibleForTesting
    VolumeInstance doGetInstance(Azure azure, String resourceId) throws FogbowException {
        Disk disk = AzureVolumeSDK.getDisk(azure, resourceId)
                .orElseThrow(() -> new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND));

        return buildVolumeInstance(disk);
    }
    
    @VisibleForTesting
    VolumeInstance buildVolumeInstance(Disk disk) {
        String id = AzureGeneralUtil.defineInstanceId(disk.name());
        String cloudState = disk.inner().provisioningState();
        String name = disk.tags().get(AzureConstants.TAG_NAME);
        int size = disk.sizeInGB();
        return new VolumeInstance(id, cloudState, name , size);
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
    
    @VisibleForTesting
    String doRequestInstance(VolumeOrder volumeOrder, Creatable<Disk> diskCreatable) {
        Observable<Indexable> observable = AzureVolumeSDK.buildCreateDiskObservable(diskCreatable);
        this.operation.subscribeCreateDisk(observable);
        updateInstanceAllocation(volumeOrder);
        return AzureGeneralUtil.defineInstanceId(diskCreatable.name());
    }

    @VisibleForTesting
    void updateInstanceAllocation(VolumeOrder volumeOrder) {
        synchronized (volumeOrder) {
            int sizeInGB = volumeOrder.getVolumeSize();
            VolumeAllocation actualAllocation = new VolumeAllocation(sizeInGB);
            volumeOrder.setActualAllocation(actualAllocation);
        }
    }

}
