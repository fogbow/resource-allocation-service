package cloud.fogbow.ras.core.plugins.interoperability.azure.attachment;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.Disk;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineDataDisk;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.util.AzureClientCacheManager;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.AttachmentInstance;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.attachment.sdk.AzureAttachmentOperationSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.attachment.sdk.AzureAttachmentSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureGeneralUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureStateMapper;
import rx.Observable;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Azure.class, AzureAttachmentSDK.class, AzureClientCacheManager.class, AzureGeneralUtil.class })
public class AzureAttachmentPluginTest {

    private String defaultResourceGroupName;
    private AzureAttachmentOperationSDK operation;
    private AzureAttachmentPlugin plugin;
    private AzureUser azureUser;
    
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        String azureConfFilePath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME
                + File.separator + AzureTestUtils.AZURE_CLOUD_NAME + File.separator
                + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

        Properties properties = PropertiesUtil.readProperties(azureConfFilePath);
        this.defaultResourceGroupName = properties.getProperty(AzureConstants.DEFAULT_RESOURCE_GROUP_NAME_KEY);
        this.operation = Mockito.mock(AzureAttachmentOperationSDK.class);
        this.plugin = Mockito.spy(new AzureAttachmentPlugin(azureConfFilePath));
        this.plugin.setOperation(this.operation);
        this.azureUser = AzureTestUtils.createAzureUser();
    }

    // test case: When calling the isReady method and the instance state is
    // Attached, it must verify that returns the true value.
    @Test
    public void testIsReadyWhenInstanceStateIsAttached() {
        // set up
        String instanceState = AzureStateMapper.ATTACHED_STATE;

        // exercise
        boolean status = this.plugin.isReady(instanceState);

        // verify
        Assert.assertTrue(status);
    }

    // test case: When calling the isReady method and the instance state is not
    // Attached, it must verify than returns false value.
    @Test
    public void testIsReadyWhenInstanceStateIsNotAttached() {
        // set up
        String[] instanceStates = { AzureStateMapper.FAILED_STATE, AzureStateMapper.UNATTACHED_STATE };

        for (String instanceState : instanceStates) {
            // exercise
            boolean status = this.plugin.isReady(instanceState);

            // verify
            Assert.assertFalse(status);
        }
    }

    // test case: When calling the hasFailed method and the instance state is
    // Unattached or failed, it must verify than returns true value.
    @Test
    public void testHasFailedWhenInstanceStateIsFailed() {
        // set up
        String[] instanceStates = { AzureStateMapper.FAILED_STATE, AzureStateMapper.UNATTACHED_STATE };

        for (String instanceState : instanceStates) {
            // exercise
            boolean status = this.plugin.hasFailed(instanceState);

            // verify
            Assert.assertTrue(status);
        }
    }

    // test case: When calling the hasFailed method and the instance state is not
    // failed, it must verify than returns false value.
    @Test
    public void testHasFailedWhenInstanceStateIsNotFailed() {
        // set up
        String instanceState = AzureStateMapper.ATTACHED_STATE;

        // exercise
        boolean status = this.plugin.hasFailed(instanceState);

        // verify
        Assert.assertFalse(status);
    }
    
    // test case: When calling the requestInstance method, it must verify that is
    // call was successful.
    @Test
    public void testRequestInstanceSuccessfully() throws Exception {
        // set up
        AttachmentOrder attachmentOrder = mockAttachmentOrder();

        Azure azure = PowerMockito.mock(Azure.class);
        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.doReturn(azure).when(AzureClientCacheManager.class, "getAzure", Mockito.eq(this.azureUser));

        String instanceId = "attachment-id";
        Mockito.doReturn(instanceId).when(this.plugin).doRequestInstance(Mockito.eq(azure), Mockito.anyString(),
                Mockito.anyString());

        // exercise
        this.plugin.requestInstance(attachmentOrder, this.azureUser);

        // verify
        PowerMockito.verifyStatic(AzureClientCacheManager.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureClientCacheManager.getAzure(Mockito.eq(this.azureUser));

        Mockito.verify(attachmentOrder, Mockito.times(TestUtils.RUN_ONCE)).getComputeId();
        Mockito.verify(attachmentOrder, Mockito.times(TestUtils.RUN_ONCE)).getVolumeId();
        Mockito.verify(this.azureUser, Mockito.times(TestUtils.RUN_ONCE)).getSubscriptionId();
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_TWICE)).buildResourceId(Mockito.anyString(),
                Mockito.anyString());

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doRequestInstance(Mockito.eq(azure),
                Mockito.anyString(), Mockito.anyString());
    }
    
    // test case: When calling the getInstance method, it must verify that is
    // call was successful.
    @Test
    public void testGetInstanceSuccessfully() throws Exception {
        // set up
        AttachmentOrder attachmentOrder = mockAttachmentOrder();

        Azure azure = PowerMockito.mock(Azure.class);
        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.doReturn(azure).when(AzureClientCacheManager.class, "getAzure", Mockito.eq(this.azureUser));

        AttachmentInstance attachmentInstance = Mockito.mock(AttachmentInstance.class);
        Mockito.doReturn(attachmentInstance).when(this.plugin).doGetInstance(Mockito.eq(azure), Mockito.anyString());

        // exercise
        this.plugin.getInstance(attachmentOrder, this.azureUser);

        // verify
        PowerMockito.verifyStatic(AzureClientCacheManager.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureClientCacheManager.getAzure(Mockito.eq(this.azureUser));

        Mockito.verify(attachmentOrder, Mockito.times(TestUtils.RUN_TWICE)).getInstanceId();
        Mockito.verify(this.azureUser, Mockito.times(TestUtils.RUN_ONCE)).getSubscriptionId();
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).buildResourceId(Mockito.anyString(),
                Mockito.anyString());

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetInstance(Mockito.eq(azure),
                Mockito.anyString());
    }
    
    // test case: When calling the deleteInstance method, it must verify that is
    // call was successful.
    @Test
    public void testDeleteInstanceSuccessfully() throws Exception {
        // set up
        AttachmentOrder attachmentOrder = mockAttachmentOrder();
        
        Azure azure = PowerMockito.mock(Azure.class);
        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.doReturn(azure).when(AzureClientCacheManager.class, "getAzure", Mockito.eq(this.azureUser));
        
        Mockito.doNothing().when(this.plugin).doDeleteInstance(Mockito.eq(azure), Mockito.anyString(), Mockito.anyString());
        
        // exercise
        this.plugin.deleteInstance(attachmentOrder, this.azureUser);
        
        // verify
        PowerMockito.verifyStatic(AzureClientCacheManager.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureClientCacheManager.getAzure(Mockito.eq(this.azureUser));

        Mockito.verify(attachmentOrder, Mockito.times(TestUtils.RUN_TWICE)).getInstanceId();
        Mockito.verify(attachmentOrder, Mockito.times(TestUtils.RUN_ONCE)).getComputeId();
        Mockito.verify(this.azureUser, Mockito.times(TestUtils.RUN_ONCE)).getSubscriptionId();
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_TWICE)).buildResourceId(Mockito.anyString(),
                Mockito.anyString());
        
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doDeleteInstance(Mockito.eq(azure),
                Mockito.anyString(), Mockito.anyString());
    }
    
    // test case: When calling the doDeleteInstance method, it must verify that is
    // call was successful.
    @Test
    public void testDoDeleteInstanceSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        
        String virtualMachineId = "virtual-machine-id";
        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
        Mockito.doReturn(virtualMachine).when(this.plugin).doGetVirtualMachineSDK(Mockito.eq(azure), Mockito.eq(virtualMachineId));
        
        String resourceId = "resource-id";
        VirtualMachineDataDisk virtualMachineDataDisk = Mockito.mock(VirtualMachineDataDisk.class);
        Mockito.doReturn(virtualMachineDataDisk).when(this.plugin).findVirtualMachineDataDisk(Mockito.eq(virtualMachine), Mockito.eq(resourceId));
        
        int lun = 0;
        Mockito.when(virtualMachineDataDisk.lun()).thenReturn(lun);
        
        Observable<VirtualMachine> observable = Mockito.mock(Observable.class);
        PowerMockito.mockStatic(AzureAttachmentSDK.class);
        PowerMockito.doReturn(observable).when(AzureAttachmentSDK.class, "detachDisk", Mockito.eq(virtualMachine), Mockito.eq(lun));
        Mockito.doNothing().when(this.operation).subscribeDetachDiskFrom(Mockito.eq(observable));
        
        // exercise
        this.plugin.doDeleteInstance(azure, virtualMachineId, resourceId);
        
        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetVirtualMachineSDK(Mockito.eq(azure), Mockito.eq(virtualMachineId));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).findVirtualMachineDataDisk(Mockito.eq(virtualMachine), Mockito.eq(resourceId));
        Mockito.verify(virtualMachineDataDisk, Mockito.times(TestUtils.RUN_ONCE)).lun();
        
        PowerMockito.verifyStatic(AzureAttachmentSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureAttachmentSDK.detachDisk(Mockito.eq(virtualMachine), Mockito.eq(lun));
        
        Mockito.verify(this.operation, Mockito.times(TestUtils.RUN_ONCE)).subscribeDetachDiskFrom(Mockito.eq(observable));
    }
    
    // test case: When calling the findVirtualMachineDataDisk method with a data
    // disk map containing a valid resource, it must verify that the expected data
    // disk was found.
    @Test
    public void testFindVirtualMachineDataDiskSuccessfully() throws Exception {
        // set up
        Integer lun = 1;
        String resourceId = "resource-id";
        VirtualMachineDataDisk dataDisk = Mockito.mock(VirtualMachineDataDisk.class);
        Mockito.when(dataDisk.id()).thenReturn(resourceId);
        Mockito.when(dataDisk.lun()).thenReturn(lun);

        Integer expectedLun = 0;
        String expectedId = "expected-resource-id";
        VirtualMachineDataDisk expectedDataDisk = Mockito.mock(VirtualMachineDataDisk.class);
        Mockito.when(expectedDataDisk.id()).thenReturn(expectedId);
        Mockito.when(expectedDataDisk.lun()).thenReturn(expectedLun);

        Map<Integer, VirtualMachineDataDisk> dataDiskMap = new HashMap();
        dataDiskMap.put(lun, dataDisk);
        dataDiskMap.put(expectedLun, expectedDataDisk);

        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
        Mockito.when(virtualMachine.dataDisks()).thenReturn(dataDiskMap);

        // exercise
        VirtualMachineDataDisk dataDiskFound = this.plugin.findVirtualMachineDataDisk(virtualMachine, expectedId);

        // verify
        Assert.assertEquals(expectedDataDisk, dataDiskFound);
    }
    
    // test case: When calling the findVirtualMachineDataDisk method with a resource
    // Id not compatible with the data disks present in the map, it must verify that
    // an InstanceNotFoundException has been thrown.
    @Test
    public void testFindVirtualMachineDataDiskFail() throws Exception {
        // set up
        String resourceId = "resource-id";
        VirtualMachineDataDisk expectedDataDisk = Mockito.mock(VirtualMachineDataDisk.class);

        String expectedId = "expected-resource-id";
        Mockito.when(expectedDataDisk.id()).thenReturn(expectedId);

        Integer expectedLun = 0;
        Mockito.when(expectedDataDisk.lun()).thenReturn(expectedLun);

        Map<Integer, VirtualMachineDataDisk> dataDiskMap = new HashMap();
        dataDiskMap.put(expectedLun, expectedDataDisk);

        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
        Mockito.when(virtualMachine.dataDisks()).thenReturn(dataDiskMap);

        // verify
        this.expectedException.expect(InstanceNotFoundException.class);

        // exercise
        this.plugin.findVirtualMachineDataDisk(virtualMachine, resourceId);
    }
    
    // test case: When calling the doGetInstance method, it must verify that is
    // call was successful.
    @Test
    public void testDoGetInstanceSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String resourceId = "resource-id";

        Disk disk = Mockito.mock(Disk.class);
        Mockito.doReturn(disk).when(this.plugin).doGetDiskSDK(Mockito.eq(azure), Mockito.eq(resourceId));

        AttachmentInstance attachmentInstance = Mockito.mock(AttachmentInstance.class);
        Mockito.doReturn(attachmentInstance).when(this.plugin).buildAttachmentInstance(Mockito.eq(disk));

        // exercise
        this.plugin.doGetInstance(azure, resourceId);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetDiskSDK(Mockito.eq(azure),
                Mockito.eq(resourceId));
        
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).buildAttachmentInstance(Mockito.eq(disk));
    }
    
    // test case: When calling the buildAttachmentInstance method with a disk state
    // attached, it must verify that the expected instance with this same state was
    // returned.
    @Test
    public void testBuildAttachmentInstanceAttached() {
        // set up
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        String virtualMachineId = "virtual-machine-id";
        String diskId = "disk-id";
        boolean attached = true;
        Disk disk = Mockito.mock(Disk.class);
        Mockito.when(disk.name()).thenReturn(resourceName);
        Mockito.when(disk.isAttachedToVirtualMachine()).thenReturn(attached);
        Mockito.when(disk.virtualMachineId()).thenReturn(virtualMachineId);
        Mockito.when(disk.id()).thenReturn(diskId);

        AttachmentInstance expected = createAttachmentInstance(attached);

        // exercise
        AttachmentInstance attachmentInstance = this.plugin.buildAttachmentInstance(disk);

        // verify
        Assert.assertEquals(expected, attachmentInstance);
    }

    // test case: When calling the buildAttachmentInstance method with a disk state
    // note attached, it must verify that the expected instance with the unattached
    // state was returned.
    @Test
    public void testBuildAttachmentInstanceNotAttached() {
        // set up
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        String virtualMachineId = "virtual-machine-id";
        String diskId = "disk-id";
        boolean attached = false;
        Disk disk = Mockito.mock(Disk.class);
        Mockito.when(disk.name()).thenReturn(resourceName);
        Mockito.when(disk.isAttachedToVirtualMachine()).thenReturn(attached);
        Mockito.when(disk.virtualMachineId()).thenReturn(virtualMachineId);
        Mockito.when(disk.id()).thenReturn(diskId);

        AttachmentInstance expected = createAttachmentInstance(attached);

        // exercise
        AttachmentInstance attachmentInstance = this.plugin.buildAttachmentInstance(disk);

        // verify
        Assert.assertEquals(expected, attachmentInstance);
    }
    
    private AttachmentInstance createAttachmentInstance(boolean attached) {
        return new AttachmentInstance(
                AzureTestUtils.RESOURCE_NAME, 
                attached ? AzureStateMapper.ATTACHED_STATE : AzureStateMapper.UNATTACHED_STATE, 
                "virtual-machine-id", 
                "disk-id", 
                null);
    }
    
    private AttachmentOrder mockAttachmentOrder() {
        String instanceId = "instance-id";
        String computeId = "compute-id";
        String volumeId = "volume-id";
        AttachmentOrder attachmentOrder = Mockito.mock(AttachmentOrder.class);
        Mockito.when(attachmentOrder.getInstanceId()).thenReturn(instanceId);
        Mockito.when(attachmentOrder.getComputeId()).thenReturn(computeId);
        Mockito.when(attachmentOrder.getVolumeId()).thenReturn(volumeId);
        return attachmentOrder;
    }
    
}
