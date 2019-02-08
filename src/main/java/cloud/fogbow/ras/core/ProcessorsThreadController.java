package cloud.fogbow.ras.core;

import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.processors.*;
import org.apache.log4j.Logger;

public class ProcessorsThreadController {
    private static final Logger LOGGER = Logger.getLogger(ProcessorsThreadController.class);

    private final Thread openProcessorThread;
    private final Thread spawningProcessorThread;
    private final Thread fulfilledProcessorThread;
    private final Thread closedProcessorThread;
    private final Thread failedProcessorThread;

    public ProcessorsThreadController(String localMemberId) {
        String openOrdersProcSleepTimeStr = PropertiesHolder.getInstance().
                getProperty(ConfigurationPropertyKeys.OPEN_ORDERS_SLEEP_TIME_KEY,
                        ConfigurationPropertyDefaults.OPEN_ORDERS_SLEEP_TIME);

        OpenProcessor openProcessor = new OpenProcessor(localMemberId, openOrdersProcSleepTimeStr);

        String spawningOrdersProcSleepTimeStr = PropertiesHolder.getInstance().
                getProperty(ConfigurationPropertyKeys.SPAWNING_ORDERS_SLEEP_TIME_KEY,
                        ConfigurationPropertyDefaults.SPAWNING_ORDERS_SLEEP_TIME);

        SpawningProcessor spawningProcessor = new SpawningProcessor(localMemberId, spawningOrdersProcSleepTimeStr);

        String fulfilledOrdersProcSleepTimeStr = PropertiesHolder.getInstance().
                getProperty(ConfigurationPropertyKeys.FULFILLED_ORDERS_SLEEP_TIME_KEY,
                        ConfigurationPropertyDefaults.FULFILLED_ORDERS_SLEEP_TIME);

        FulfilledProcessor fulfilledProcessor = new FulfilledProcessor(localMemberId, fulfilledOrdersProcSleepTimeStr);

        String closedOrdersProcSleepTimeStr = PropertiesHolder.getInstance().
                getProperty(ConfigurationPropertyKeys.CLOSED_ORDERS_SLEEP_TIME_KEY,
                        ConfigurationPropertyDefaults.CLOSED_ORDERS_SLEEP_TIME);

        ClosedProcessor closedProcessor = new ClosedProcessor(closedOrdersProcSleepTimeStr);
        
        String failedOrdersProcSleepTimeStr = PropertiesHolder.getInstance().
                getProperty(ConfigurationPropertyKeys.FAILED_ORDERS_SLEEP_TIME_KEY,
                        ConfigurationPropertyDefaults.FAILED_ORDERS_SLEEP_TIME);
        
        FailedProcessor failedProcessor = new FailedProcessor(localMemberId, failedOrdersProcSleepTimeStr);

        this.openProcessorThread = new Thread(openProcessor, "open-proc");
        this.spawningProcessorThread = new Thread(spawningProcessor, "spawning-proc");
        this.fulfilledProcessorThread = new Thread(fulfilledProcessor, "fulfilled-proc");
        this.closedProcessorThread = new Thread(closedProcessor, "closed-proc");
        this.failedProcessorThread = new Thread(failedProcessor, "failed-proc");
    }

    /**
     * This method starts all RAS processors, if you defined a new RAS operation and this
     * operation require a new thread to run, you should start this thread at this method.
     */
    public void startRasThreads() {
        LOGGER.info(Messages.Info.STARTING_THREADS);
        this.openProcessorThread.start();
        this.spawningProcessorThread.start();
        this.fulfilledProcessorThread.start();
        this.closedProcessorThread.start();
        this.failedProcessorThread.start();
    }
}
