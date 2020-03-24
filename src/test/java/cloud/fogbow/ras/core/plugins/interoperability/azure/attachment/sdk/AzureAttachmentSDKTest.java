package cloud.fogbow.ras.core.plugins.interoperability.azure.attachment.sdk;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.microsoft.azure.management.compute.Disk;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachine.Update;

import cloud.fogbow.ras.core.TestUtils;
import rx.Observable;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ AzureAttachmentSDK.class })
public class AzureAttachmentSDKTest {

    @Before
    public void setup() {
        PowerMockito.mockStatic(AzureAttachmentSDK.class);
    }
    
    // test case: When calling the attachDisk method, it must verify
    // that is call was successful.
    @Test
    public void testAttachDiskSuccessfully() throws Exception {
        // set up
        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
        Disk disk = Mockito.mock(Disk.class);
        PowerMockito.doCallRealMethod().when(AzureAttachmentSDK.class, "attachDisk", 
                Mockito.eq(virtualMachine), Mockito.eq(disk));

        Update update = Mockito.mock(Update.class);
        Mockito.when(virtualMachine.update()).thenReturn(update);

        Observable<VirtualMachine> observable = Mockito.mock(Observable.class);
        Mockito.when(update.withExistingDataDisk(Mockito.eq(disk))).thenReturn(update);
        Mockito.when(update.applyAsync()).thenReturn(observable);

        // exercise
        AzureAttachmentSDK.attachDisk(virtualMachine, disk);

        // verify
        Mockito.verify(virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).update();
        Mockito.verify(update, Mockito.times(TestUtils.RUN_ONCE)).withExistingDataDisk(Mockito.eq(disk));
        Mockito.verify(update, Mockito.times(TestUtils.RUN_ONCE)).applyAsync();
    }
    
    // test case: When calling the detachDisk method, it must verify
    // that is call was successful.
    @Test
    public void testDetachDiskSuccessfully() throws Exception {
        // set up
        int lun = 0;
        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
        PowerMockito.doCallRealMethod().when(AzureAttachmentSDK.class, "detachDisk", 
                Mockito.eq(virtualMachine), Mockito.eq(lun));

        Update update = Mockito.mock(Update.class);
        Mockito.when(virtualMachine.update()).thenReturn(update);

        Observable<VirtualMachine> observable = Mockito.mock(Observable.class);
        Mockito.when(update.withoutDataDisk(Mockito.eq(lun))).thenReturn(update);
        Mockito.when(update.applyAsync()).thenReturn(observable);

        // exercise
        AzureAttachmentSDK.detachDisk(virtualMachine, lun);

        // verify
        Mockito.verify(virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).update();
        Mockito.verify(update, Mockito.times(TestUtils.RUN_ONCE)).withoutDataDisk(Mockito.eq(lun));
        Mockito.verify(update, Mockito.times(TestUtils.RUN_ONCE)).applyAsync();
    }
    
}
