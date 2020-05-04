package cloud.fogbow.ras.core.plugins.interoperability.azure.volume;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.Disk;
import com.microsoft.azure.management.compute.Disks;
import com.microsoft.azure.management.compute.implementation.DiskInner;
import com.microsoft.azure.management.resources.fluentcore.model.Creatable;
import com.microsoft.azure.management.resources.fluentcore.model.Indexable;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.QuotaExceededException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.VolumeInstance;
import cloud.fogbow.ras.api.http.response.quotas.allocation.VolumeAllocation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.LoggerAssert;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureClientCacheManager;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureGeneralUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureResourceGroupUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.azure.volume.sdk.AzureVolumeOperationSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.volume.sdk.AzureVolumeSDK;
import rx.Completable;
import rx.Observable;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    Azure.class,
    AzureClientCacheManager.class,
    AzureGeneralUtil.class,
    AzureResourceGroupUtil.class,
    AzureVolumeSDK.class
})
public class AzureVolumePluginTest {

    private String defaultRegionName;
    private String defaultResourceGroupName;
    private AzureVolumeOperationSDK operation;
    private AzureVolumePlugin plugin;
    private AzureUser azureUser;
    private LoggerAssert loggerAssert;
    
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
        this.loggerAssert = new LoggerAssert(AzureVolumePlugin.class);
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

        String resourceGroupName = AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME;
        Mockito.doReturn(resourceGroupName).when(this.plugin).defineResourceGroupName(Mockito.eq(azure), Mockito.anyString());

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
        Mockito.verify(this.plugin, Mockito.timeout(TestUtils.RUN_ONCE)).defineResourceGroupName(Mockito.eq(azure),
                Mockito.anyString());
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
        PowerMockito.mockStatic(AzureGeneralUtil.class);
        PowerMockito.doReturn(resourceName).when(AzureGeneralUtil.class, "defineResourceName", Mockito.anyString());

        String resourceGroupName = AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME;
        Mockito.doReturn(resourceGroupName).when(this.plugin).getResourceGroupName(Mockito.eq(azure), Mockito.anyString());
        
        String resourceId = createResourceId();
        String subscriptionId = AzureTestUtils.DEFAULT_SUBSCRIPTION_ID;
        Mockito.doReturn(resourceId).when(this.plugin).buildResourceId(Mockito.eq(subscriptionId), Mockito.eq(resourceGroupName),
                Mockito.eq(resourceName));
        
        VolumeInstance volumeInstance = Mockito.mock(VolumeInstance.class);
        Mockito.doReturn(volumeInstance).when(this.plugin).doGetInstance(Mockito.eq(azure), Mockito.eq(resourceId));
        
        VolumeOrder volumeOrder = Mockito.mock(VolumeOrder.class);
        volumeOrder.setInstanceId(resourceName);
        
        // exercise
        this.plugin.getInstance(volumeOrder, this.azureUser);
        
        // verify
        PowerMockito.verifyStatic(AzureClientCacheManager.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureClientCacheManager.getAzure(Mockito.eq(this.azureUser));
        
        PowerMockito.verifyStatic(AzureGeneralUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureGeneralUtil.defineResourceName(Mockito.eq(volumeOrder.getInstanceId()));
        
        Mockito.verify(this.azureUser, Mockito.times(TestUtils.RUN_ONCE)).getSubscriptionId();
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).buildResourceId(Mockito.eq(subscriptionId),
                Mockito.eq(resourceGroupName), Mockito.eq(resourceName));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetInstance(Mockito.eq(azure), Mockito.eq(resourceId));
        Mockito.verify(this.plugin, Mockito.timeout(TestUtils.RUN_ONCE)).getResourceGroupName(Mockito.eq(azure),
                Mockito.anyString());
    }

    // test case: When calling the deleteInstance method, with the default
    // resource group, it must verify among others if the doDeleteInstance
    // method has been called.
    @Test
    public void testDeleteInstanceWithDefaultResourceGroup() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.doReturn(azure).when(AzureClientCacheManager.class, "getAzure", Mockito.eq(this.azureUser));
        
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        PowerMockito.mockStatic(AzureGeneralUtil.class);
        PowerMockito.doReturn(resourceName).when(AzureGeneralUtil.class, "defineResourceName", Mockito.anyString());
        
        String resourceGroupName = AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME;
        Mockito.doReturn(resourceGroupName).when(this.plugin).getResourceGroupName(Mockito.eq(azure),
                Mockito.anyString());

        Mockito.doReturn(true).when(this.plugin).isDefaultResourceGroup(Mockito.eq(resourceGroupName));

        String resourceId = createResourceId();
        String subscriptionId = AzureTestUtils.DEFAULT_SUBSCRIPTION_ID;
        Mockito.doReturn(resourceId).when(this.plugin).buildResourceId(Mockito.eq(subscriptionId),
                Mockito.eq(resourceGroupName), Mockito.eq(resourceName));
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
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).buildResourceId(Mockito.eq(subscriptionId),
                Mockito.eq(resourceGroupName), Mockito.eq(resourceName));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doDeleteInstance(Mockito.eq(azure), Mockito.eq(resourceId));
        Mockito.verify(this.plugin, Mockito.timeout(TestUtils.RUN_ONCE)).getResourceGroupName(Mockito.eq(azure),
                Mockito.anyString());
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).isDefaultResourceGroup(Mockito.eq(resourceGroupName));
    }

    // test case: When calling the deleteInstance method, without the default
    // resource group, it must verify among others if the doDeleteResourceGroup
    // method has been called.
    @Test
    public void testDeleteInstanceWithoutDefaultResourceGroup() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.doReturn(azure).when(AzureClientCacheManager.class, "getAzure", Mockito.eq(this.azureUser));

        String resourceName = AzureTestUtils.RESOURCE_NAME;
        PowerMockito.mockStatic(AzureGeneralUtil.class);
        PowerMockito.doReturn(resourceName).when(AzureGeneralUtil.class, "defineResourceName", Mockito.anyString());

        String resourceGroupName = AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME;
        Mockito.doReturn(resourceGroupName).when(this.plugin).getResourceGroupName(Mockito.eq(azure),
                Mockito.anyString());

        Mockito.doReturn(false).when(this.plugin).isDefaultResourceGroup(Mockito.eq(resourceGroupName));

        Mockito.doNothing().when(this.plugin).doDeleteResourceGroup(Mockito.eq(azure), Mockito.eq(resourceGroupName));

        VolumeOrder volumeOrder = Mockito.mock(VolumeOrder.class);
        volumeOrder.setInstanceId(resourceName);

        // exercise
        this.plugin.deleteInstance(volumeOrder, this.azureUser);

        // verify
        PowerMockito.verifyStatic(AzureClientCacheManager.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureClientCacheManager.getAzure(Mockito.eq(this.azureUser));

        PowerMockito.verifyStatic(AzureGeneralUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureGeneralUtil.defineResourceName(Mockito.eq(volumeOrder.getInstanceId()));

        Mockito.verify(this.plugin, Mockito.timeout(TestUtils.RUN_ONCE)).getResourceGroupName(Mockito.eq(azure),
                Mockito.anyString());
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).isDefaultResourceGroup(Mockito.eq(resourceGroupName));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doDeleteResourceGroup(Mockito.eq(azure),
                Mockito.eq(resourceGroupName));
    }

    // test case: When calling the doDeleteResourceGroup method, it must verify
    // that is call was successful.
    @Test
    public void testDoDeleteResourceGroupSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String resourceGroupName = AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME;

        Completable completable = AzureTestUtils.createSimpleCompletableSuccess();
        PowerMockito.mockStatic(AzureResourceGroupUtil.class);
        PowerMockito.doReturn(completable).when(AzureResourceGroupUtil.class, "deleteAsync", Mockito.eq(azure), Mockito.eq(resourceGroupName));

        Mockito.doNothing().when(this.operation).subscribeDeleteDisk(Mockito.eq(completable));

        // exercise
        this.plugin.doDeleteResourceGroup(azure, resourceGroupName);

        // verify
        PowerMockito.verifyStatic(AzureResourceGroupUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureResourceGroupUtil.deleteAsync(Mockito.eq(azure), Mockito.eq(resourceGroupName));

        Mockito.verify(this.operation).subscribeDeleteDisk(Mockito.eq(completable));
    }

    // test case: When calling the isDefaultResourceGroup method, it must verify
    // that it returned the true value.
    @Test
    public void testIsDefaultResourceGroupReturnTrue() {
        // set up
        String resourceGroupName = AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME;

        // exercise
        boolean result = this.plugin.isDefaultResourceGroup(resourceGroupName);

        // verify
        Assert.assertTrue(result);
    }

    // test case: When calling the isDefaultResourceGroup method, it must verify
    // that it returned the false value.
    @Test
    public void testIsDefaultResourceGroupReturnFalse() {
        // set up
        String resourceGroupName = AzureTestUtils.RESOURCE_NAME;

        // exercise
        boolean result = this.plugin.isDefaultResourceGroup(resourceGroupName);

        // verify
        Assert.assertFalse(result);
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
        String resourceGroupName = AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME;

        String expected = createResourceId();

        // exercise
        String resourceId = this.plugin.buildResourceId(subscriptionId, resourceGroupName, resourceName);

        // verify
        Assert.assertEquals(expected, resourceId);
    }
    
    // test case: When calling the getResourceGroupName method, from an existing
    // resource group, it must verify that it returned the same resource name
    // passed by parameter.
    @Test
    public void testGetResourceGroupNameFromOneExisting() throws Exception {
        //set up
        Azure azure = PowerMockito.mock(Azure.class);
        String expected = AzureTestUtils.RESOURCE_NAME;

        PowerMockito.mockStatic(AzureResourceGroupUtil.class);
        PowerMockito.doReturn(true).when(AzureResourceGroupUtil.class, "exists", Mockito.eq(azure),
                Mockito.eq(expected));

        // exercise
        String result = this.plugin.getResourceGroupName(azure, expected);

        // verify
        PowerMockito.verifyStatic(AzureResourceGroupUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureResourceGroupUtil.exists(Mockito.eq(azure), Mockito.eq(expected));

        Assert.assertSame(expected, result);
    }

    // test case: When calling the getResourceGroupName method, from a
    // non-existent resource group, it must return the default resource group
    // name.
    @Test
    public void testGetResourceGroupNameDefault() throws Exception {
        //set up
        Azure azure = PowerMockito.mock(Azure.class);
        String resourceGroupName = AzureTestUtils.RESOURCE_NAME;

        PowerMockito.mockStatic(AzureResourceGroupUtil.class);
        PowerMockito.doReturn(false).when(AzureResourceGroupUtil.class, "exists", Mockito.eq(azure),
                Mockito.eq(resourceGroupName));

        String expected = AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME;

        // exercise
        String result = this.plugin.getResourceGroupName(azure, resourceGroupName);

        // verify
        PowerMockito.verifyStatic(AzureResourceGroupUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureResourceGroupUtil.exists(Mockito.eq(azure), Mockito.eq(resourceGroupName));

        Assert.assertEquals(expected, result);
    }

    // test case: When calling the doRequestInstance method, it must verify that is
    // call was successful.
    @Test
    public void testDoRequestInstanceSuccessfully() throws Exception {
        // set up
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        Creatable<Disk> diskCreatable = Mockito.mock(Creatable.class);
        Mockito.when(diskCreatable.name()).thenReturn(resourceName);
        
        Observable<Indexable> observable = Mockito.mock(Observable.class);
        PowerMockito.mockStatic(AzureVolumeSDK.class);
        PowerMockito.doReturn(observable).when(AzureVolumeSDK.class, "buildCreateDiskObservable", Mockito.any(Creatable.class));
        
        VolumeOrder volumeOrder = Mockito.mock(VolumeOrder.class);
        Mockito.doNothing().when(this.operation).subscribeCreateDisk(Mockito.eq(observable));
        Mockito.doNothing().when(this.plugin).updateInstanceAllocation(Mockito.eq(volumeOrder));
        
        PowerMockito.mockStatic(AzureGeneralUtil.class);
        PowerMockito.doReturn(resourceName).when(AzureGeneralUtil.class, "defineInstanceId", Mockito.anyString());
        
        // exercise
        this.plugin.doRequestInstance(volumeOrder, diskCreatable);
        
        // verify
        PowerMockito.verifyStatic(AzureVolumeSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureVolumeSDK.buildCreateDiskObservable(Mockito.eq(diskCreatable));
        
        Mockito.verify(this.operation, Mockito.times(TestUtils.RUN_ONCE)).subscribeCreateDisk(Mockito.eq(observable));
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

    // test case: When calling the defineResourceGroupName method, and the
    // creation of the resource group is successful, it must return the name of
    // the created resource group.
    @Test
    public void testDefineResourceGroupNameSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String expected = AzureTestUtils.RESOURCE_NAME;

        PowerMockito.mockStatic(AzureResourceGroupUtil.class);
        PowerMockito.doReturn(expected).when(AzureResourceGroupUtil.class, "create",
                Mockito.eq(azure), Mockito.anyString(), Mockito.eq(expected));

        // exercise
        String resourceGroupName = this.plugin.defineResourceGroupName(azure, expected);

        // verify
        PowerMockito.verifyStatic(AzureResourceGroupUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureResourceGroupUtil.create(Mockito.eq(azure), Mockito.anyString(), Mockito.eq(expected));

        Assert.assertSame(expected, resourceGroupName);
    }

    // test case: When calling the defineResourceGroupName method and a problem
    // occurs when exceeding the creation limit for this resource, it must
    // return the name of the default resource group.
    @Test
    public void testDefineResourceGroupNameFail() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String resourceGroup = AzureTestUtils.RESOURCE_NAME;
        String expected = AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME;

        Exception exception = Mockito.mock(QuotaExceededException.class);
        PowerMockito.mockStatic(AzureResourceGroupUtil.class);
        PowerMockito.doThrow(exception).when(AzureResourceGroupUtil.class, "create",
                Mockito.eq(azure), Mockito.anyString(), Mockito.eq(resourceGroup));

        // exercise
        String resourceGroupName = this.plugin.defineResourceGroupName(azure, resourceGroup);

        // verify
        PowerMockito.verifyStatic(AzureResourceGroupUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureResourceGroupUtil.create(Mockito.eq(azure), Mockito.anyString(), Mockito.eq(resourceGroup));

        this.loggerAssert.assertEqualsInOrder(Level.ERROR, String.format(Messages.Error.ERROR_MESSAGE, exception));

        Assert.assertEquals(expected, resourceGroupName);
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
