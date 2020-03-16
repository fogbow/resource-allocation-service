package cloud.fogbow.ras.core.plugins.interoperability.azure.volume.sdk;

import java.util.concurrent.ExecutorService;

import org.apache.log4j.Logger;

import com.microsoft.azure.management.resources.fluentcore.model.Indexable;

import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureSchedulerManager;
import rx.Completable;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

public class AzureVolumeOperationSDK {

    private static final Logger LOGGER = Logger.getLogger(AzureVolumeOperationSDK.class);

    private Scheduler scheduler;
    
    public AzureVolumeOperationSDK() {
        ExecutorService executor = AzureSchedulerManager.getVolumeExecutor();
        this.scheduler = Schedulers.from(executor);
    }
    
    public void subscribeCreateDisk(Observable<Indexable> observable) {
        observable.doOnError((error -> {
            LOGGER.error(Messages.Error.ERROR_CREATE_DISK_ASYNC_BEHAVIOUR, error);
        }))
        .doOnCompleted(() -> {
            LOGGER.info(Messages.Info.END_CREATE_DISK_ASYNC_BEHAVIOUR);
        })
        .subscribeOn(this.scheduler)
        .subscribe();
    }
    
    public void subscribeDeleteDisk(Completable completable) {
        completable.doOnError((error -> {
            LOGGER.error(Messages.Error.ERROR_DELETE_DISK_ASYNC_BEHAVIOUR);
        }))
        .doOnCompleted(() -> {
            LOGGER.info(Messages.Info.END_DELETE_DISK_ASYNC_BEHAVIOUR);
        })
        .subscribeOn(this.scheduler)
        .subscribe();
    }
    
}
