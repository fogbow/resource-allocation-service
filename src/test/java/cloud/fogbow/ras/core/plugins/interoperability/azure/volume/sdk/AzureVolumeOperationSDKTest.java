package cloud.fogbow.ras.core.plugins.interoperability.azure.volume.sdk;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.microsoft.azure.management.resources.fluentcore.model.Indexable;

import ch.qos.logback.classic.Level;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.LoggerAssert;
import rx.Observable;

public class AzureVolumeOperationSDKTest {

    private static final Logger GET_LOGGER_CLASS = Logger.getLogger(AzureVolumeOperationSDK.class);
    
    private LoggerAssert loggerAssert = new LoggerAssert(AzureVolumeOperationSDK.class);
    private AzureVolumeOperationSDK operation;
    
    @Before
    public void setup() {
        this.operation = Mockito.spy(new AzureVolumeOperationSDK());
    }
    
    // test case: When calling the subscribeCreateDisk method and the
    // observable executes without any error, it must verify than returns the right
    // logs.
    @Test
    public void testSubscribeCreateDiskSucessfully() {
        // set up
        Observable<Indexable> observable = createSimpleObservableSuccess();

        // exercise
        this.operation.subscribeCreateDisk(observable);

        // verify
        this.loggerAssert.assertEqualsInOrder(Level.INFO, Messages.Info.END_CREATE_DISK_ASYNC_BEHAVIOUR);
    }
    
    private Observable<Indexable> createSimpleObservableSuccess() {
        return Observable.defer(() -> {
            Indexable indexable = Mockito.mock(Indexable.class);
            return Observable.just(indexable);
        });
    }
}
