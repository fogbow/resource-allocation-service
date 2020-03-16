package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AzureSchedulerManager {

    private static final int ATTACHMENT_POOL_SIZE = 2;
    private static final int VIRTUAL_MACHINE_POOL_SIZE = 2;
    private static final int VOLUME_POOL_SIZE = 2;

    public static ExecutorService getAttachmentExecutor() {
        return Executors.newFixedThreadPool(ATTACHMENT_POOL_SIZE);
    }
    
    public static ExecutorService getVirtualMachineExecutor() {
        return Executors.newFixedThreadPool(VIRTUAL_MACHINE_POOL_SIZE);
    }

    public static ExecutorService getVolumeExecutor() {
        return Executors.newFixedThreadPool(VOLUME_POOL_SIZE);
    }

}
