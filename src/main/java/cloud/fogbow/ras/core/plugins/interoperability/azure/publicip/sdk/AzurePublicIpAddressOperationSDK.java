package cloud.fogbow.ras.core.plugins.interoperability.azure.publicip.sdk;

import java.util.concurrent.ExecutorService;

import org.apache.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;

import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureSchedulerManager;
import rx.Completable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

public class AzurePublicIpAddressOperationSDK {
    
    private static final Logger LOGGER = Logger.getLogger(AzurePublicIpAddressOperationSDK.class);

    private Scheduler scheduler;

    public AzurePublicIpAddressOperationSDK() {
        ExecutorService executor = AzureSchedulerManager.getPublicIpExecutor();
        this.scheduler = Schedulers.from(executor);
    }

    public void subscribeDeletePublicIPAddress(Completable completable) {
        setDeleteDiskBehaviour(completable)
        .subscribeOn(this.scheduler)
        .subscribe();
    }

    @VisibleForTesting
    Completable setDeleteDiskBehaviour(Completable completable) {
        return completable.doOnError((error -> {
            LOGGER.error(Messages.Error.ERROR_DELETE_PUBLIC_IP_ASYNC_BEHAVIOUR);
        })).doOnCompleted(() -> {
            LOGGER.info(Messages.Info.END_DELETE_PUBLIC_IP_ASYNC_BEHAVIOUR);
        });
    }

}
