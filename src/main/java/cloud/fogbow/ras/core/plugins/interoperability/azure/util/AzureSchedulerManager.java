package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AzureSchedulerManager {

    private static final int VIRTUAL_MACHINE_POOL_SIZE = 2;

    public static ExecutorService getVirtualMachineExecutor() {
        return Executors.newFixedThreadPool(VIRTUAL_MACHINE_POOL_SIZE);
    }

    // TODO implement
    public static ExecutorService getVirtualNetworkExecutor() {
        return null;
    }
}
