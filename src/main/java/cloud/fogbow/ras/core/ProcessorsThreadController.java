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
    private final Thread checkingDeletionProcessorThread;
    private final Thread failedProcessorThread;
    private final Thread assignedForDeletionProcessorThread;
    private final Thread remoteOrdersStateSynchronizationProcessorThread;

    private final static String OPEN_PROCESSOR_THREAD_NAME = "open-proc";
    private final static String SPAWNING_PROCESSOR_THREAD_NAME = "spawning-proc";
    private final static String FULFILLED_PROCESSOR_THREAD_NAME = "fulfilled-proc";
    private final static String CHECKING_DELETION_PROCESSOR_THREAD_NAME = "checking-deletion-proc";
    private final static String FAILED_PROCESSOR_THREAD_NAME = "failed-proc";
    private final static String ASSIGNED_FOR_DELETION_PROCESSOR_THREAD_NAME = "assigned-for-deletion-proc";
    private final static String REMOTE_ORDER_STATE_SYNCHRONIZATION_PROCESSOR_THREAD_NAME = "remote-sync-proc";

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

        String checkingDeletionOrdersProcSleepTimeStr = PropertiesHolder.getInstance().
                getProperty(ConfigurationPropertyKeys.CHECKING_DELETION_ORDERS_SLEEP_TIME_KEY,
                        ConfigurationPropertyDefaults.CHECKING_DELETION_ORDERS_SLEEP_TIME);

        CheckingDeletionProcessor checkingDeletionProcessor = new CheckingDeletionProcessor(orderController, localProviderId, checkingDeletionOrdersProcSleepTimeStr);
        
        String unableToCheckProcSleepTimeStr = PropertiesHolder.getInstance().
                getProperty(ConfigurationPropertyKeys.UNABLE_TO_CHECK_ORDERS_SLEEP_TIME_KEY,
                        ConfigurationPropertyDefaults.UNABLE_TO_CHECK_ORDERS_SLEEP_TIME);
        
        UnableToCheckStatusProcessor unableToCheckStatusProcessor = new UnableToCheckStatusProcessor(localProviderId, unableToCheckProcSleepTimeStr);

        String assignedForDeletionOrdersProcSleepTimeStr = PropertiesHolder.getInstance().
                getProperty(ConfigurationPropertyKeys.ASSIGNED_FOR_DELETION_ORDERS_SLEEP_TIME_KEY,
                        ConfigurationPropertyDefaults.ASSIGNED_FOR_DELETION_ORDERS_SLEEP_TIME);

        AssignedForDeletionProcessor assignedForDeletionProcessor = new AssignedForDeletionProcessor(localProviderId, assignedForDeletionOrdersProcSleepTimeStr);

        String remoteOrdersStateSynchronizationProcSleepTimeStr = PropertiesHolder.getInstance().
                getProperty(ConfigurationPropertyKeys.REMOTE_ORDER_STATE_SYNCHRONIZATION_SLEEP_TIME_KEY,
                        ConfigurationPropertyDefaults.REMOTE_ORDER_STATE_SYNCHRONIZATION_SLEEP_TIME);

        RemoteOrdersStateSynchronizationProcessor remoteOrdersStateSynchronizationProcessor = new RemoteOrdersStateSynchronizationProcessor(localProviderId, remoteOrdersStateSynchronizationProcSleepTimeStr);

        this.openProcessorThread = new Thread(openProcessor, OPEN_PROCESSOR_THREAD_NAME);
        this.spawningProcessorThread = new Thread(spawningProcessor, SPAWNING_PROCESSOR_THREAD_NAME);
        this.fulfilledProcessorThread = new Thread(fulfilledProcessor, FULFILLED_PROCESSOR_THREAD_NAME);
        this.checkingDeletionProcessorThread = new Thread(checkingDeletionProcessor, CHECKING_DELETION_PROCESSOR_THREAD_NAME);
        this.failedProcessorThread = new Thread(unableToCheckStatusProcessor, FAILED_PROCESSOR_THREAD_NAME);
        this.assignedForDeletionProcessorThread = new Thread(assignedForDeletionProcessor, ASSIGNED_FOR_DELETION_PROCESSOR_THREAD_NAME);
        this.remoteOrdersStateSynchronizationProcessorThread = new Thread(remoteOrdersStateSynchronizationProcessor, REMOTE_ORDER_STATE_SYNCHRONIZATION_PROCESSOR_THREAD_NAME);
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
        this.checkingDeletionProcessorThread.start();
        this.failedProcessorThread.start();
        this.assignedForDeletionProcessorThread.start();
        this.remoteOrdersStateSynchronizationProcessorThread.start();
    }
}
