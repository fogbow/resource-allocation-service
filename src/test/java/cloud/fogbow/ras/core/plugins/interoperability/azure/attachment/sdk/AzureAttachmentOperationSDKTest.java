package cloud.fogbow.ras.core.plugins.interoperability.azure.attachment.sdk;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.microsoft.azure.management.compute.VirtualMachine;

import ch.qos.logback.classic.Level;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.LoggerAssert;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

public class AzureAttachmentOperationSDKTest {

    private AzureAttachmentOperationSDK operation;
    private LoggerAssert loggerAssert;
    
    @Before
    public void setUp() {
        // The scheduler trampoline makes the subscriptions execute 
        // in the current thread
        Scheduler scheduler = Schedulers.trampoline();
        this.operation = Mockito.spy(new AzureAttachmentOperationSDK());
        this.operation.setScheduler(scheduler);
        this.loggerAssert = new LoggerAssert(AzureAttachmentOperationSDK.class);
    }
    
    // test case: When calling the subscribeAttachDiskFrom method and the
    // observable executes without any error, it must verify than returns the right
    // logs.
    @Test
    public void testSubscribeAttachDiskFromObservableSuccessfully() {
        // set up
        Observable<VirtualMachine> observable = AzureTestUtils.createVirtualMachineObservableSuccess();

        // exercise
        this.operation.subscribeAttachDiskFrom(observable);

        // verify
        this.loggerAssert.assertEqualsInOrder(Level.INFO, Messages.Info.END_ATTACH_DISK_ASYNC_BEHAVIOUR);
    }
    
    // test case: When calling the subscribeAttachDiskFrom method and the
    // observable executes with an error, it must verify than returns the right
    // logs.
    @Test
    public void testSubscribeAttachDiskFromObservableFail() {
        // set up
        Observable observable = AzureTestUtils.createSimpleObservableFail();

        // exercise
        this.operation.subscribeAttachDiskFrom(observable);

        // verify
        this.loggerAssert.assertEqualsInOrder(Level.ERROR, Messages.Error.ERROR_ATTACH_DISK_ASYNC_BEHAVIOUR);
    }
    
    // test case: When calling the subscribeDetachDiskFrom method and the
    // observable executes without any error, it must verify than returns the right
    // logs.
    @Test
    public void testSubscribeDetachDiskFromObservableSuccessfully() {
        // set up
        Observable<VirtualMachine> observable = AzureTestUtils.createVirtualMachineObservableSuccess();

        // exercise
        this.operation.subscribeDetachDiskFrom(observable);

        // verify
        this.loggerAssert.assertEqualsInOrder(Level.INFO, Messages.Info.END_DETACH_DISK_ASYNC_BEHAVIOUR);
    }
    
    // test case: When calling the subscribeDetachDiskFrom method and the
    // observable executes with an error, it must verify than returns the right
    // logs.
    @Test
    public void testSubscribeDetachDiskFromObservableFail() {
        // set up
        Observable observable = AzureTestUtils.createSimpleObservableFail();

        // exercise
        this.operation.subscribeDetachDiskFrom(observable);

        // verify
        this.loggerAssert.assertEqualsInOrder(Level.ERROR, Messages.Error.ERROR_DETACH_DISK_ASYNC_BEHAVIOUR);
    }
    
}
