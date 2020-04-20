package cloud.fogbow.ras.core.plugins.interoperability.azure.volume.sdk;

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
import com.microsoft.azure.management.compute.Disks;
import com.microsoft.azure.management.resources.fluentcore.model.Creatable;
import com.microsoft.azure.management.resources.fluentcore.model.Indexable;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureResourceIdBuilder;
import rx.Completable;
import rx.Observable;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Azure.class, AzureVolumeSDK.class })
public class AzureVolumeSDKTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    
    @Before
    public void setup() {
        PowerMockito.mockStatic(AzureVolumeSDK.class);
    }
    
    // test case: When calling the buildCreateDiskObservable method, it must verify
    // that is call was successful.
    @Test
    public void testBuildCreateDiskObservableSuccessfully() throws Exception {
        // set up
        Creatable<Disk> diskCreatable = Mockito.mock(Creatable.class);
        PowerMockito.doCallRealMethod().when(AzureVolumeSDK.class, "buildCreateDiskObservable",
                Mockito.eq(diskCreatable));

        Observable<Indexable> observable = Mockito.mock(Observable.class);
        Mockito.when(diskCreatable.createAsync()).thenReturn(observable);

        // exercise
        AzureVolumeSDK.buildCreateDiskObservable(diskCreatable);

        // verify
        Mockito.verify(diskCreatable, Mockito.times(TestUtils.RUN_ONCE)).createAsync();
    }
    
    // test case: When calling the buildDeleteDiskCompletable method with a valid
    // resource ID, it must verify that is call was successful.
    @Test
    public void testBuildDeleteDiskCompletableSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String resourceId = AzureResourceIdBuilder.diskId().build();
        PowerMockito.doCallRealMethod().when(AzureVolumeSDK.class, "buildDeleteDiskCompletable", 
                Mockito.eq(azure), Mockito.eq(resourceId));

        Disks disks = Mockito.mock(Disks.class);
        PowerMockito.doReturn(disks).when(AzureVolumeSDK.class, "getDisksSDK", Mockito.eq(azure));

        Completable completable = Mockito.mock(Completable.class);
        Mockito.when(disks.deleteByIdAsync(Mockito.eq(resourceId))).thenReturn(completable);

        // exercise
        AzureVolumeSDK.buildDeleteDiskCompletable(azure, resourceId);

        // verify
        PowerMockito.verifyStatic(AzureVolumeSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureVolumeSDK.getDisksSDK(Mockito.eq(azure));

        Mockito.verify(disks, Mockito.times(TestUtils.RUN_ONCE)).deleteByIdAsync(Mockito.eq(resourceId));
    }
    
    // test case: When calling the buildDeleteDiskCompletable method and an
    // unexpected error occurs, it must verify than an UnexpectedException has been
    // thrown.
    @Test
    public void testBuildDeleteDiskCompletableFail() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String resourceId = AzureResourceIdBuilder.diskId().build();
        PowerMockito.doCallRealMethod().when(AzureVolumeSDK.class, "buildDeleteDiskCompletable", Mockito.eq(azure),
                Mockito.eq(resourceId));

        Disks disks = Mockito.mock(Disks.class);
        PowerMockito.doReturn(disks).when(AzureVolumeSDK.class, "getDisksSDK", Mockito.eq(azure));
        Mockito.when(disks.deleteByIdAsync(resourceId)).thenThrow(new RuntimeException());

        // verify
        this.expectedException.expect(UnexpectedException.class);

        // exercise
        AzureVolumeSDK.buildDeleteDiskCompletable(azure, resourceId);
    }
    
    // test case: When calling the getDisk method with a valid resource ID, it must
    // verify that is call was successful.
    @Test
    public void testGetDiskSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String resourceId = AzureResourceIdBuilder.diskId().build();
        PowerMockito.doCallRealMethod().when(AzureVolumeSDK.class, "getDisk", Mockito.eq(azure),
                Mockito.eq(resourceId));

        Disks disks = Mockito.mock(Disks.class);
        PowerMockito.doReturn(disks).when(AzureVolumeSDK.class, "getDisksSDK", Mockito.eq(azure));

        Disk disk = Mockito.mock(Disk.class);
        Mockito.when(disks.getById(resourceId)).thenReturn(disk);

        // exercise
        AzureVolumeSDK.getDisk(azure, resourceId);

        // verify
        PowerMockito.verifyStatic(AzureVolumeSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureVolumeSDK.getDisksSDK(Mockito.eq(azure));

        Mockito.verify(disks, Mockito.times(TestUtils.RUN_ONCE)).getById(Mockito.eq(resourceId));
    }
    
    // test case: When calling the getDisk method and an unexpected error occurs, it
    // must verify than an UnexpectedException has been thrown.
    @Test
    public void testGetDiskFail() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String resourceId = AzureResourceIdBuilder.diskId().build();
        PowerMockito.doCallRealMethod().when(AzureVolumeSDK.class, "getDisk", Mockito.eq(azure),
                Mockito.eq(resourceId));

        Disks disks = Mockito.mock(Disks.class);
        PowerMockito.doReturn(disks).when(AzureVolumeSDK.class, "getDisksSDK", Mockito.eq(azure));
        Mockito.when(disks.getById(resourceId)).thenThrow(new RuntimeException());

        // verify
        this.expectedException.expect(UnexpectedException.class);

        // exercise
        AzureVolumeSDK.getDisk(azure, resourceId);
    }
    
}
