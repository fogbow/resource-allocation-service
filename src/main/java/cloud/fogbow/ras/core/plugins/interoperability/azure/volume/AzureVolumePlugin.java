package cloud.fogbow.ras.core.plugins.interoperability.azure.volume;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.Disk;
import com.microsoft.azure.management.compute.Disks;
import com.microsoft.azure.management.resources.fluentcore.model.Creatable;
import com.microsoft.azure.management.resources.fluentcore.model.Indexable;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
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
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureInstancePolicy;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureResourceIdBuilder;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureSchedulerManager;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureStateMapper;
import rx.Completable;
import rx.Observable;
import rx.schedulers.Schedulers;

public class AzureVolumePlugin implements VolumePlugin<AzureUser>{

    private static final Logger LOGGER = Logger.getLogger(AzureVolumePlugin.class);
    
    private final String defaultRegionName;
    private final String defaultResourceGroupName;

    public AzureVolumePlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.defaultRegionName = properties.getProperty(AzureConstants.DEFAULT_REGION_NAME_KEY);
        this.defaultResourceGroupName = properties.getProperty(AzureConstants.DEFAULT_RESOURCE_GROUP_NAME_KEY);
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
        String volumeName = AzureInstancePolicy.defineAzureResourceName(volumeOrder);
        int sizeInGB = volumeOrder.getVolumeSize();
        
        Creatable<Disk> creatableDisk = azure.disks().define(volumeName)
                .withRegion(this.defaultRegionName)
                .withExistingResourceGroup(this.defaultResourceGroupName)
                .withData()
                .withSizeInGB(sizeInGB);
        
        Observable<Indexable> observable = creatableDisk.createAsync();
        observable.doOnError((error -> {
            LOGGER.error(Messages.Error.ERROR_CREATE_DISK_ASYNC_BEHAVIOUR, error);
        }))
        .doOnCompleted(() -> {
            LOGGER.info(Messages.Info.END_CREATE_DISK_ASYNC_BEHAVIOUR);
        })
        .subscribeOn(Schedulers.from(AzureSchedulerManager.getVolumeExecutor()))
        .subscribe();
        
        updateInstanceAllocation(volumeOrder, sizeInGB);
        return getInstanceId(creatableDisk);
    }

    @VisibleForTesting
    String getInstanceId(Creatable<Disk> creatableDisk) {
        String resourceName = creatableDisk.name();
        List<String> identifiers = Arrays.asList(resourceName.split(AzureConstants.RESOURCE_NAME_SEPARATOR));
        return identifiers.listIterator().next();
    }

    @VisibleForTesting
    void updateInstanceAllocation(VolumeOrder volumeOrder, int sizeInGB) {
        synchronized (volumeOrder) {
            VolumeAllocation actualAllocation = new VolumeAllocation(sizeInGB);
            volumeOrder.setActualAllocation(actualAllocation);
        }
    }

    @Override
    public VolumeInstance getInstance(VolumeOrder volumeOrder, AzureUser azureUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE_S, volumeOrder.getInstanceId()));
        String resourceName = volumeOrder.getInstanceId() 
                + AzureConstants.RESOURCE_NAME_SEPARATOR
                + volumeOrder.getName();
        
        String subscriptionId = azureUser.getSubscriptionId();
        String instanceIdUrl = buildResourceIdUrl(subscriptionId, resourceName);
        
        Azure azure = AzureClientCacheManager.getAzure(azureUser);
        
        Disk disk = null;
        try {
            Disks disks = azure.disks();
            disk = disks.getById(instanceIdUrl);
        } catch (RuntimeException e) {
            throw new UnexpectedException(e.getMessage(), e);
        }

        String cloudState = disk.inner().provisioningState();
        String name = disk.name();
        int size = disk.sizeInGB();
        return new VolumeInstance(instanceIdUrl, cloudState, name , size);
    }

    @VisibleForTesting
    String buildResourceIdUrl(String subscriptionId, String resourceName) {
        String resourceIdUrl = AzureResourceIdBuilder
                .configure(AzureConstants.VOLUME_STRUCTURE)
                .withSubscriptionId(subscriptionId)
                .withResourceGroupName(this.defaultResourceGroupName)
                .withResourceName(resourceName)
                .build();
        
        return resourceIdUrl;
    }

    @Override
    public void deleteInstance(VolumeOrder volumeOrder, AzureUser azureUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE_S, volumeOrder.getInstanceId()));
        String resourceName = volumeOrder.getInstanceId() 
                + AzureConstants.RESOURCE_NAME_SEPARATOR
                + volumeOrder.getName();
        
        String subscriptionId = azureUser.getSubscriptionId();
        String instanceIdUrl = buildResourceIdUrl(subscriptionId, resourceName);
        
        Azure azure = AzureClientCacheManager.getAzure(azureUser);
        Completable completable = azure.disks().deleteByIdAsync(instanceIdUrl);
        completable.doOnError((error -> {
            LOGGER.error(Messages.Error.ERROR_DELETE_DISK_ASYNC_BEHAVIOUR);
        }))
        .doOnCompleted(() -> {
            LOGGER.info(Messages.Info.END_DELETE_DISK_ASYNC_BEHAVIOUR);
        })
        .subscribeOn(Schedulers.from(AzureSchedulerManager.getVolumeExecutor()))
        .subscribe(); 
    }

}
