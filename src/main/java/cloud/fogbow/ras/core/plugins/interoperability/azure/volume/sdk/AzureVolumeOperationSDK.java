package cloud.fogbow.ras.core.plugins.interoperability.azure.volume.sdk;

import java.util.concurrent.ExecutorService;

import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AsyncInstanceCreationManager;
import org.apache.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
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
    
    public void subscribeCreateDisk(Observable<Indexable> observable,
                                    AsyncInstanceCreationManager.Callbacks finishCreationCallback) {

        setCreateDiskBehaviour(observable, finishCreationCallback)
        .subscribeOn(this.scheduler)
        .subscribe();
    }

    @VisibleForTesting
    Observable<Indexable> setCreateDiskBehaviour(Observable<Indexable> observable,
                                                 AsyncInstanceCreationManager.Callbacks finishCreationCallback) {

        return observable.onErrorReturn((error -> {
            finishCreationCallback.runOnError();
            LOGGER.error(Messages.Error.ERROR_CREATE_DISK_ASYNC_BEHAVIOUR, error);
            return null;
        })).doOnCompleted(() -> {
            finishCreationCallback.runOnComplete();
            LOGGER.info(Messages.Info.END_CREATE_DISK_ASYNC_BEHAVIOUR);
        });
    }
    
    public void subscribeDeleteDisk(Completable completable) {
        setDeleteDiskBehaviour(completable)
        .subscribeOn(this.scheduler)
        .subscribe();
    }

    @VisibleForTesting
    Completable setDeleteDiskBehaviour(Completable completable) {
        return completable.doOnError((error -> {
            LOGGER.error(Messages.Error.ERROR_DELETE_DISK_ASYNC_BEHAVIOUR);
        })).doOnCompleted(() -> {
            LOGGER.info(Messages.Info.END_DELETE_DISK_ASYNC_BEHAVIOUR);
        });
    }
	
    @VisibleForTesting
    void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }
    
}
