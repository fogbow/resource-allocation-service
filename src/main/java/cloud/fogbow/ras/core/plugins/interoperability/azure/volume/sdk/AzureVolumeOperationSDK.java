package cloud.fogbow.ras.core.plugins.interoperability.azure.volume.sdk;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import rx.Scheduler;
import rx.schedulers.Schedulers;

public class AzureVolumeOperationSDK {

    private static final Logger LOGGER = Logger.getLogger(AzureVolumeOperationSDK.class);

    private static final int VOLUME_THREAD_POOL = 2;
    
    private String regionName;
    private Scheduler scheduler;
    
    public AzureVolumeOperationSDK(String regionName) {
        ExecutorService volumeExecutor = Executors.newFixedThreadPool(VOLUME_THREAD_POOL);
        this.scheduler = Schedulers.from(volumeExecutor);
        this.regionName = regionName;
    }
    
}
