package cloud.fogbow.ras.core.plugins.interoperability.azure.volume.sdk;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.microsoft.azure.management.resources.fluentcore.model.Indexable;

import ch.qos.logback.classic.Level;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.LoggerAssert;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;
import rx.Completable;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

public class AzureVolumeOperationSDKTest {

    private AzureVolumeOperationSDK operation;
    private LoggerAssert loggerAssert;

    @Before
    public void setup() {
        // The scheduler trampoline makes the subscriptions execute 
        // in the current thread
        Scheduler scheduler = Schedulers.trampoline();
        this.operation = Mockito.spy(new AzureVolumeOperationSDK());
        this.operation.setScheduler(scheduler);
        this.loggerAssert = new LoggerAssert(AzureVolumeOperationSDK.class);
    }

    // test case: When calling the subscribeCreateDisk method and the
    // observable executes without any error, it must verify than returns the right
    // logs.
    @Test
    public void testSubscribeCreateDiskSucessfully() {
        // set up
        Observable<Indexable> observable = AzureTestUtils.createSimpleObservableSuccess();

        // exercise
        this.operation.subscribeCreateDisk(observable);

        // verify
        this.loggerAssert.assertEqualsInOrder(Level.INFO, Messages.Info.END_CREATE_DISK_ASYNC_BEHAVIOUR);
    }

    // test case: When calling the subscribeCreateDisk method and the
    // observable executes with an error, it must verify than returns the right
    // logs.
    @Test
    public void testSubscribeCreateDiskFail() {
        // set up
        Observable observable = AzureTestUtils.createSimpleObservableFail();

        // exercise
        this.operation.subscribeCreateDisk(observable);

        // verify
        this.loggerAssert.assertEqualsInOrder(Level.ERROR, Messages.Error.ERROR_CREATE_DISK_ASYNC_BEHAVIOUR);
    }

    // test case: When calling the subscribeDeleteDisk method and the completable
    // executes without any error, it must verify than returns the right logs.
    @Test
    public void testSubscribeDeleteDiskSuccessfully() {
        // set up
        Completable completable = Completable.complete();

        // exercise
        this.operation.subscribeDeleteDisk(completable);

        // verify
        this.loggerAssert.assertEqualsInOrder(Level.INFO, Messages.Info.END_DELETE_DISK_ASYNC_BEHAVIOUR);
    }

    // test case: When calling the subscribeDeleteDisk method and the completable
    // executes with an error, it must verify if It returns the right logs.
    @Test
    public void testSubscribeDeleteDiskFail() {
        // set up
        Completable completable = Completable.error(new RuntimeException());

        // exercise
        this.operation.subscribeDeleteDisk(completable);

        // verify
        this.loggerAssert.assertEqualsInOrder(Level.ERROR, Messages.Error.ERROR_DELETE_DISK_ASYNC_BEHAVIOUR);
    }

}
