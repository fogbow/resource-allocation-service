package cloud.fogbow.ras.core.plugins.interoperability.azure.volume;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.VolumeInstance;
import cloud.fogbow.ras.api.http.response.quotas.allocation.VolumeAllocation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureClientCacheManager;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureGeneralUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.azure.volume.sdk.AzureVolumeOperationSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.volume.sdk.AzureVolumeSDK;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.Disk;
import com.microsoft.azure.management.compute.Disks;
import com.microsoft.azure.management.compute.implementation.DiskInner;
import com.microsoft.azure.management.resources.fluentcore.model.Creatable;
import com.microsoft.azure.management.resources.fluentcore.model.Indexable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import rx.Completable;
import rx.Observable;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Azure.class, AzureClientCacheManager.class, AzureGeneralUtil.class, AzureVolumeSDK.class })
public class AzureVolumePluginTest {

    private String defaultRegionName;
    private String defaultResourceGroupName;
    private AzureVolumeOperationSDK operation;
    private AzureVolumePlugin plugin;
    private AzureUser azureUser;
    
    @Before
    public void setUp() throws Exception {
        String azureConfFilePath = HomeDir.getPath() 
                + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator 
                + AzureTestUtils.AZURE_CLOUD_NAME + File.separator
                + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
        
        Properties properties = PropertiesUtil.readProperties(azureConfFilePath);
        this.defaultRegionName = properties.getProperty(AzureConstants.DEFAULT_REGION_NAME_KEY);
        this.defaultResourceGroupName = properties.getProperty(AzureConstants.DEFAULT_RESOURCE_GROUP_NAME_KEY);
        this.operation = Mockito.mock(AzureVolumeOperationSDK.class);
        this.plugin = Mockito.spy(new AzureVolumePlugin(azureConfFilePath));
        this.plugin.setOperation(this.operation);
        this.azureUser = AzureTestUtils.createAzureUser();
    }
    
    // test case: When calling the isReady method and the instance state is
    // succeeded, it must verify than returns true value.
    @Test
    public void testIsReadyWhenInstanceStateIsSucceeded() {
        // set up
        String instanceState = AzureStateMapper.SUCCEEDED_STATE;

        // exercise
        boolean status = this.plugin.isReady(instanceState);

        // verify
        Assert.assertTrue(status);
    }

    // test case: When calling the isReady method and the instance state is not
    // succeeded, it must verify than returns false value.
    @Test
    public void testIsReadyWhenInstanceStateIsNotSucceeded() {
        // set up
        String[] instanceStates = { AzureStateMapper.CREATING_STATE, AzureStateMapper.FAILED_STATE };

        for (String instanceState : instanceStates) {
            // exercise
            boolean status = this.plugin.isReady(instanceState);

            // verify
            Assert.assertFalse(status);
        }
    }

    // test case: When calling the hasFailed method and the instance state is
    // failed, it must verify than returns true value.
    @Test
    public void testHasFailedWhenInstanceStateIsFailed() {
        // set up
        String instanceState = AzureStateMapper.FAILED_STATE;
        
        // exercise
        boolean status = this.plugin.hasFailed(instanceState);

        // exercise and verify
        Assert.assertTrue(status);
    }

    // test case: When calling the hasFailed method and the instance state is not
    // failed, it must verify than returns false value.
    @Test
    public void testHasFailedWhenInstanceStateIsNotFailed() {
        // set up
        String[] instanceStates = { AzureStateMapper.CREATING_STATE, AzureStateMapper.SUCCEEDED_STATE };

        for (String instanceState : instanceStates) {
            // exercise
            boolean status = this.plugin.hasFailed(instanceState);

            // verify
            Assert.assertFalse(status);
        }
    }
    
    // test case: When calling the requestInstance method, it must verify that is
    // call was successful.
    @Test
    public void testRequestInstanceSuccessfully() throws Exception {
        // set up
        String name = AzureTestUtils.ORDER_NAME;
        VolumeOrder volumeOrder = Mockito.mock(VolumeOrder.class);
        volumeOrder.setName(name);

        Azure azure = PowerMockito.mock(Azure.class);
        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.doReturn(azure).when(AzureClientCacheManager.class, "getAzure", Mockito.eq(this.azureUser));

        String resourceName = AzureTestUtils.RESOURCE_NAME;
        PowerMockito.mockStatic(AzureGeneralUtil.class);
        PowerMockito.doReturn(resourceName).when(AzureGeneralUtil.class, "generateResourceName");

        Disks disks = Mockito.mock(Disks.class);
        Mockito.when(azure.disks()).thenReturn(disks);

        Disk.DefinitionStages.Blank define = Mockito.mock(Disk.DefinitionStages.Blank.class);
        Mockito.when(disks.define(Mockito.anyString())).thenReturn(define);

        Disk.DefinitionStages.WithGroup withRegion = Mockito.mock(Disk.DefinitionStages.WithGroup.class);
        Mockito.when(define.withRegion(Mockito.eq(this.defaultRegionName))).thenReturn(withRegion);

        Disk.DefinitionStages.WithDiskSource withExistingResourceGroup = Mockito
                .mock(Disk.DefinitionStages.WithDiskSource.class);
        Mockito.when(withRegion.withExistingResourceGroup(Mockito.eq(this.defaultResourceGroupName)))
                .thenReturn(withExistingResourceGroup);

        Disk.DefinitionStages.WithDataDiskSource withData = Mockito
                .mock(Disk.DefinitionStages.WithDataDiskSource.class);
        Mockito.when(withExistingResourceGroup.withData()).thenReturn(withData);

        Disk.DefinitionStages.WithCreate withSizeInGbAndTags = Mockito.mock(Disk.DefinitionStages.WithCreate.class);
        Mockito.when(withData.withSizeInGB(Mockito.anyInt())).thenReturn(withSizeInGbAndTags);

        Map tags = Collections.singletonMap(AzureConstants.TAG_NAME, name);
        Mockito.when(withSizeInGbAndTags.withTags(Mockito.eq(tags))).thenReturn(withSizeInGbAndTags);

        String instanceId = resourceName;
        Mockito.doReturn(instanceId).when(this.plugin).doRequestInstance(Mockito.eq(volumeOrder),
                Mockito.any(Creatable.class));

        // exercise
        this.plugin.requestInstance(volumeOrder, azureUser);

        // verify
        PowerMockito.verifyStatic(AzureClientCacheManager.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureClientCacheManager.getAzure(Mockito.eq(this.azureUser));

        PowerMockito.verifyStatic(AzureGeneralUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureGeneralUtil.generateResourceName();

        Mockito.verify(volumeOrder, Mockito.times(TestUtils.RUN_ONCE)).getVolumeSize();
        Mockito.verify(volumeOrder, Mockito.times(TestUtils.RUN_ONCE)).getName();
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doRequestInstance(Mockito.eq(volumeOrder),
                Mockito.any(Creatable.class));
    }
    
    // test case: When calling the getInstance method, it must verify that is call
    // was successful.
    @Test
    public void testGetInstanceSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.doReturn(azure).when(AzureClientCacheManager.class, "getAzure", Mockito.eq(this.azureUser));
        
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        String instanceId = AzureGeneralUtil.defineInstanceId(resourceName);
        PowerMockito.mockStatic(AzureGeneralUtil.class);
        PowerMockito.doReturn(resourceName).when(AzureGeneralUtil.class, "defineResourceName", Mockito.anyString());
        
        String resourceId = createResourceId();
        String subscriptionId = AzureTestUtils.DEFAULT_SUBSCRIPTION_ID;
        Mockito.doReturn(resourceId).when(this.plugin).buildResourceId(Mockito.eq(subscriptionId), Mockito.eq(resourceName));
        
        VolumeInstance volumeInstance = Mockito.mock(VolumeInstance.class);
        Mockito.doReturn(volumeInstance).when(this.plugin).doGetInstance(Mockito.eq(azure), Mockito.eq(resourceId));
        
        VolumeOrder volumeOrder = Mockito.mock(VolumeOrder.class);
        Mockito.when(volumeOrder.getInstanceId()).thenReturn(instanceId);

        VolumeInstance volumeInstanceCreated = null;
        Mockito.doReturn(volumeInstanceCreated).when(this.plugin).getCreatingInstance(Mockito.eq(instanceId));
        
        // exercise
        this.plugin.getInstance(volumeOrder, this.azureUser);
        
        // verify
        PowerMockito.verifyStatic(AzureClientCacheManager.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureClientCacheManager.getAzure(Mockito.eq(this.azureUser));
        
        PowerMockito.verifyStatic(AzureGeneralUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureGeneralUtil.defineResourceName(Mockito.eq(volumeOrder.getInstanceId()));
        
        Mockito.verify(this.azureUser, Mockito.times(TestUtils.RUN_ONCE)).getSubscriptionId();
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).buildResourceId(Mockito.eq(subscriptionId), Mockito.eq(resourceName));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetInstance(Mockito.eq(azure), Mockito.eq(resourceId));
    }

    // test case: When calling the getInstance method with instance creating, it must verify that is call
    // was successful.
    @Test
    public void testGetInstanceSuccessfullyWhenInstanceCreating() throws Exception {
        // set up
        String instanceId = AzureGeneralUtil.defineInstanceId(AzureTestUtils.RESOURCE_NAME);
        VolumeOrder volumeOrder = Mockito.mock(VolumeOrder.class);
        Mockito.when(volumeOrder.getInstanceId()).thenReturn(instanceId);

        VolumeInstance volumeInstanceCreating = Mockito.mock(VolumeInstance.class);
        Mockito.doReturn(volumeInstanceCreating).when(this.plugin).getCreatingInstance(Mockito.eq(instanceId));

        // exercise
        VolumeInstance volumeInstance = this.plugin.getInstance(volumeOrder, this.azureUser);

        // verify
        Assert.assertEquals(volumeInstanceCreating, volumeInstance);
    }

    // test case: When calling the deleteInstance method, it must verify that is call
    // was successful.
    @Test
    public void testDeleteInstanceSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.doReturn(azure).when(AzureClientCacheManager.class, "getAzure", Mockito.eq(this.azureUser));
        
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        PowerMockito.mockStatic(AzureGeneralUtil.class);
        PowerMockito.doReturn(resourceName).when(AzureGeneralUtil.class, "defineResourceName", Mockito.anyString());
        
        String resourceId = createResourceId();
        String subscriptionId = AzureTestUtils.DEFAULT_SUBSCRIPTION_ID;
        Mockito.doReturn(resourceId).when(this.plugin).buildResourceId(Mockito.eq(subscriptionId), Mockito.eq(resourceName));
        Mockito.doNothing().when(this.plugin).doDeleteInstance(Mockito.eq(azure), Mockito.eq(resourceId));
        
        VolumeOrder volumeOrder = Mockito.mock(VolumeOrder.class);
        volumeOrder.setInstanceId(resourceName);

        // exercise
        this.plugin.deleteInstance(volumeOrder, this.azureUser);

        // verify
        PowerMockito.verifyStatic(AzureClientCacheManager.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureClientCacheManager.getAzure(Mockito.eq(this.azureUser));
        
        PowerMockito.verifyStatic(AzureGeneralUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureGeneralUtil.defineResourceName(Mockito.eq(volumeOrder.getInstanceId()));
        
        Mockito.verify(this.azureUser, Mockito.times(TestUtils.RUN_ONCE)).getSubscriptionId();
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).buildResourceId(Mockito.eq(subscriptionId), Mockito.eq(resourceName));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doDeleteInstance(Mockito.eq(azure), Mockito.eq(resourceId));
    }
    
    // test case: When calling the doDeleteInstance method, it must verify that is call
    // was successful.
    @Test
    public void testDoDeleteInstanceSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String resourceId = createResourceId();
        
        Completable completable = Mockito.mock(Completable.class);
        PowerMockito.mockStatic(AzureVolumeSDK.class);
        PowerMockito.doReturn(completable).when(AzureVolumeSDK.class, "buildDeleteDiskCompletable", Mockito.eq(azure), Mockito.eq(resourceId));
        
        Mockito.doNothing().when(this.operation).subscribeDeleteDisk(Mockito.eq(completable));
        
        // exercise
        this.plugin.doDeleteInstance(azure, resourceId);
        
        // verify
        PowerMockito.verifyStatic(AzureVolumeSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureVolumeSDK.buildDeleteDiskCompletable(Mockito.eq(azure), Mockito.eq(resourceId));
        
        Mockito.verify(this.operation).subscribeDeleteDisk(Mockito.eq(completable));
    }
    
    // test case: When calling the doGetInstance method, it must verify that is call
    // was successful.
    @Test
    public void testDoGetInstanceSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String resourceId = createResourceId();
        
        Disk disk = Mockito.mock(Disk.class);
        Optional<Disk> diskOptional = Optional.ofNullable(disk);
        
        PowerMockito.mockStatic(AzureVolumeSDK.class);
        PowerMockito.doReturn(diskOptional).when(AzureVolumeSDK.class, "getDisk", Mockito.eq(azure), Mockito.eq(resourceId));
        
        VolumeInstance volumeInstance = Mockito.mock(VolumeInstance.class);
        Mockito.doReturn(volumeInstance).when(this.plugin).buildVolumeInstance(Mockito.eq(disk));
        
        // exercise
        this.plugin.doGetInstance(azure, resourceId);
        
        // verify
        PowerMockito.verifyStatic(AzureVolumeSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureVolumeSDK.getDisk(Mockito.eq(azure), Mockito.eq(resourceId));
        
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).buildVolumeInstance(Mockito.eq(disk));
    }
    
    // test case: When calling the doGetInstance method with an invalid resource ID,
    // it must verify if an InstanceNotFoundException has been thrown.
    @Test
    public void testDoGetInstanceFail() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String resourceId = TestUtils.ANY_VALUE;

        Optional<Disk> diskOptional = Optional.ofNullable(null);
        PowerMockito.mockStatic(AzureVolumeSDK.class);
        PowerMockito.doReturn(diskOptional).when(AzureVolumeSDK.class, "getDisk", Mockito.eq(azure),
                Mockito.eq(resourceId));

        String expected = Messages.Exception.INSTANCE_NOT_FOUND;

        try {
            // exercise
            this.plugin.doGetInstance(azure, resourceId);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the buildVolumeInstance method, it must verify that
    // is call was successful and created the volume instance.
    @Test
    public void testBuildVolumeInstanceSuccessfully() throws Exception {
        // set up
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        Disk disk = Mockito.mock(Disk.class);
        Mockito.when(disk.name()).thenReturn(resourceName);

        String resourceId = createResourceId();
        String state = AzureStateMapper.SUCCEEDED_STATE;
        DiskInner diskInner = Mockito.mock(DiskInner.class);
        Mockito.when(diskInner.id()).thenReturn(resourceId);
        Mockito.when(diskInner.provisioningState()).thenReturn(state);
        Mockito.when(disk.inner()).thenReturn(diskInner);

        String orderName = AzureTestUtils.ORDER_NAME;
        Map tags = Collections.singletonMap(AzureConstants.TAG_NAME, orderName);
        Mockito.when(disk.tags()).thenReturn(tags);

        Integer sizeInGB = TestUtils.DISK_VALUE;
        Mockito.when(disk.sizeInGB()).thenReturn(sizeInGB);

        VolumeInstance expected = createVolumeInstance();

        // exercise
        VolumeInstance volumeInstance = this.plugin.buildVolumeInstance(disk);

        // verify
        Mockito.verify(disk, Mockito.times(TestUtils.RUN_ONCE)).inner();
        Mockito.verify(diskInner, Mockito.times(TestUtils.RUN_ONCE)).id();
        Mockito.verify(diskInner, Mockito.times(TestUtils.RUN_ONCE)).provisioningState();
        Mockito.verify(disk, Mockito.times(TestUtils.RUN_ONCE)).tags();
        Mockito.verify(disk, Mockito.times(TestUtils.RUN_ONCE)).sizeInGB();

        Assert.assertEquals(expected, volumeInstance);
    }
    
    // test case: When calling the buildResourceId method, it must verify that the
    // resource ID was assembled correctly.
    @Test
    public void testBuildResourceIdSuccessfully() {
        // set up
        String subscriptionId = AzureTestUtils.DEFAULT_SUBSCRIPTION_ID;
        String resourceName = AzureTestUtils.RESOURCE_NAME;

        String expected = createResourceId();

        // exercise
        String resourceId = this.plugin.buildResourceId(subscriptionId, resourceName);

        // verify
        Assert.assertEquals(expected, resourceId);
    }
    
    // test case: When calling the doRequestInstance method, it must verify that is
    // call was successful.
    @Test
    public void testDoRequestInstanceSuccessfully() throws Exception {
        // set up
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        String instanceId = AzureGeneralUtil.defineInstanceId(resourceName);
        Creatable<Disk> diskCreatable = Mockito.mock(Creatable.class);
        Mockito.when(diskCreatable.name()).thenReturn(resourceName);
        
        Observable<Indexable> observable = Mockito.mock(Observable.class);
        PowerMockito.mockStatic(AzureVolumeSDK.class);
        PowerMockito.doReturn(observable).when(AzureVolumeSDK.class, "buildCreateDiskObservable", Mockito.any(Creatable.class));

        Runnable doOnComplete = Mockito.mock(Runnable.class);
        Mockito.doReturn(doOnComplete).when(this.plugin).startIntanceCreation(Mockito.eq(instanceId));

        VolumeOrder volumeOrder = Mockito.mock(VolumeOrder.class);
        Mockito.doNothing().when(this.operation).subscribeCreateDisk(Mockito.eq(observable), Mockito.eq(doOnComplete));
        Mockito.doNothing().when(this.plugin).updateInstanceAllocation(Mockito.eq(volumeOrder));
        
        PowerMockito.mockStatic(AzureGeneralUtil.class);
        PowerMockito.doReturn(resourceName).when(AzureGeneralUtil.class, "defineInstanceId", Mockito.anyString());
        
        // exercise
        this.plugin.doRequestInstance(volumeOrder, diskCreatable);
        
        // verify
        PowerMockito.verifyStatic(AzureVolumeSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureVolumeSDK.buildCreateDiskObservable(Mockito.eq(diskCreatable));
        
        Mockito.verify(this.operation, Mockito.times(TestUtils.RUN_ONCE)).subscribeCreateDisk(
                Mockito.eq(observable), Mockito.eq(doOnComplete));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).updateInstanceAllocation(Mockito.eq(volumeOrder));
        
        PowerMockito.verifyStatic(AzureGeneralUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureGeneralUtil.defineInstanceId(Mockito.eq(resourceName));
    }
    
    // test case: When calling the updateInstanceAllocation method, it must verify
    // that is call was successful.
    @Test
    public void testUpdateInstanceAllocationSuccessfully() {
        // set up
        int sizeInGB = TestUtils.DISK_VALUE;
        VolumeOrder volumeOrder = Mockito.mock(VolumeOrder.class);
        Mockito.when(volumeOrder.getVolumeSize()).thenReturn(sizeInGB);

        // exercise
        this.plugin.updateInstanceAllocation(volumeOrder);

        // verify
        Mockito.verify(volumeOrder, Mockito.times(TestUtils.RUN_ONCE)).getVolumeSize();
        Mockito.verify(volumeOrder, Mockito.times(TestUtils.RUN_ONCE))
                .setActualAllocation(Mockito.any(VolumeAllocation.class));
    }

    private VolumeInstance createVolumeInstance() {
        String resourceId = createResourceId();
        return new VolumeInstance(
                resourceId,
                AzureStateMapper.SUCCEEDED_STATE, 
                AzureTestUtils.ORDER_NAME, 
                TestUtils.DISK_VALUE);
    }
    
    
    private String createResourceId() {
        String diskIdFormat = "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Compute/disks/%s";
        return String.format(diskIdFormat, 
                AzureTestUtils.DEFAULT_SUBSCRIPTION_ID, this.defaultResourceGroupName,
                AzureTestUtils.RESOURCE_NAME);
    }
    
}
