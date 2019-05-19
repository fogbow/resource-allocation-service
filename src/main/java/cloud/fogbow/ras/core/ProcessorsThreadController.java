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
    private final static String OPEN_PROCESSOR_THREAD_NAME = "open-proc";
    private final static String SPAWNING_PROCESSOR_THREAD_NAME = "spawning-proc";
    private final static String FULFILLED_PROCESSOR_THREAD_NAME = "fulfilled-proc";
    private final static String CLOSED_PROCESSOR_THREAD_NAME = "closed-proc";
    private final static String FAILED_PROCESSOR_THREAD_NAME = "failed-proc";

    public ProcessorsThreadController(String localProviderId, OrderController orderController) {
        String openOrdersProcSleepTimeStr = PropertiesHolder.getInstance().
                getProperty(ConfigurationPropertyKeys.OPEN_ORDERS_SLEEP_TIME_KEY,
                        ConfigurationPropertyDefaults.OPEN_ORDERS_SLEEP_TIME);

        OpenProcessor openProcessor = new OpenProcessor(localProviderId, openOrdersProcSleepTimeStr);

        String spawningOrdersProcSleepTimeStr = PropertiesHolder.getInstance().
                getProperty(ConfigurationPropertyKeys.SPAWNING_ORDERS_SLEEP_TIME_KEY,
                        ConfigurationPropertyDefaults.SPAWNING_ORDERS_SLEEP_TIME);

        SpawningProcessor spawningProcessor = new SpawningProcessor(localProviderId, spawningOrdersProcSleepTimeStr);

        String fulfilledOrdersProcSleepTimeStr = PropertiesHolder.getInstance().
                getProperty(ConfigurationPropertyKeys.FULFILLED_ORDERS_SLEEP_TIME_KEY,
                        ConfigurationPropertyDefaults.FULFILLED_ORDERS_SLEEP_TIME);

        FulfilledProcessor fulfilledProcessor = new FulfilledProcessor(localProviderId, fulfilledOrdersProcSleepTimeStr);

        String closedOrdersProcSleepTimeStr = PropertiesHolder.getInstance().
                getProperty(ConfigurationPropertyKeys.CLOSED_ORDERS_SLEEP_TIME_KEY,
                        ConfigurationPropertyDefaults.CLOSED_ORDERS_SLEEP_TIME);

        ClosedProcessor closedProcessor = new ClosedProcessor(orderController, closedOrdersProcSleepTimeStr);
        
        String failedOrdersProcSleepTimeStr = PropertiesHolder.getInstance().
                getProperty(ConfigurationPropertyKeys.FAILED_ORDERS_SLEEP_TIME_KEY,
                        ConfigurationPropertyDefaults.FAILED_ORDERS_SLEEP_TIME);
        
        UnableToCheckStatusProcessor unableToCheckStatusProcessor = new UnableToCheckStatusProcessor(localProviderId, failedOrdersProcSleepTimeStr);

        this.openProcessorThread = new Thread(openProcessor, OPEN_PROCESSOR_THREAD_NAME);
        this.spawningProcessorThread = new Thread(spawningProcessor, SPAWNING_PROCESSOR_THREAD_NAME);
        this.fulfilledProcessorThread = new Thread(fulfilledProcessor, FULFILLED_PROCESSOR_THREAD_NAME);
        this.closedProcessorThread = new Thread(closedProcessor, CLOSED_PROCESSOR_THREAD_NAME);
        this.failedProcessorThread = new Thread(unableToCheckStatusProcessor, FAILED_PROCESSOR_THREAD_NAME);
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
