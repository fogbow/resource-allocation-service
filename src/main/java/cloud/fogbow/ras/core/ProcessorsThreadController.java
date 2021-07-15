package cloud.fogbow.ras.core;

import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.processors.*;
import org.apache.log4j.Logger;

public class ProcessorsThreadController {
    private static final Logger LOGGER = Logger.getLogger(ProcessorsThreadController.class);

    private final OpenProcessor openProcessor;
    private final SpawningProcessor spawningProcessor;
    private final FulfilledProcessor fulfilledProcessor;
    private final CheckingDeletionProcessor checkingDeletionProcessor;
    private final UnableToCheckStatusProcessor unableToCheckStatusProcessor;
    private final AssignedForDeletionProcessor assignedForDeletionProcessor;
    private final RemoteOrdersStateSynchronizationProcessor remoteOrdersStateSynchronizationProcessor;
    private final PausingProcessor pausingProcessor;
    private final HibernatingProcessor hibernatingProcessor;
    private final StoppingProcessor stoppingProcessor;
    private final ResumingProcessor resumingProcessor;
    
    private final static String OPEN_PROCESSOR_THREAD_NAME = "open-proc";
    private final static String SPAWNING_PROCESSOR_THREAD_NAME = "spawning-proc";
    private final static String FULFILLED_PROCESSOR_THREAD_NAME = "fulfilled-proc";
    private final static String CHECKING_DELETION_PROCESSOR_THREAD_NAME = "checking-deletion-proc";
    private final static String FAILED_PROCESSOR_THREAD_NAME = "failed-proc";
    private final static String ASSIGNED_FOR_DELETION_PROCESSOR_THREAD_NAME = "assigned-for-deletion-proc";
    private final static String REMOTE_ORDER_STATE_SYNCHRONIZATION_PROCESSOR_THREAD_NAME = "remote-sync-proc";
    private final static String PAUSING_PROCESSOR_THREAD_NAME = "pausing-proc";
    private final static String HIBERNATING_PROCESSOR_THREAD_NAME = "hibernating-proc";
    private final static String STOPPING_PROCESSOR_THREAD_NAME = "stopping-proc";
    private final static String RESUMING_PROCESSOR_THREAD_NAME = "resuming-proc";

    private Boolean threadsAreRunning;
    
    public ProcessorsThreadController(String localProviderId, OrderController orderController) {
        String openOrdersProcSleepTimeStr = PropertiesHolder.getInstance().
                getProperty(ConfigurationPropertyKeys.OPEN_ORDERS_SLEEP_TIME_KEY,
                        ConfigurationPropertyDefaults.OPEN_ORDERS_SLEEP_TIME);

        this.openProcessor = new OpenProcessor(localProviderId, openOrdersProcSleepTimeStr);

        String spawningOrdersProcSleepTimeStr = PropertiesHolder.getInstance().
                getProperty(ConfigurationPropertyKeys.SPAWNING_ORDERS_SLEEP_TIME_KEY,
                        ConfigurationPropertyDefaults.SPAWNING_ORDERS_SLEEP_TIME);

        this.spawningProcessor = new SpawningProcessor(localProviderId, spawningOrdersProcSleepTimeStr);

        String fulfilledOrdersProcSleepTimeStr = PropertiesHolder.getInstance().
                getProperty(ConfigurationPropertyKeys.FULFILLED_ORDERS_SLEEP_TIME_KEY,
                        ConfigurationPropertyDefaults.FULFILLED_ORDERS_SLEEP_TIME);

        this.fulfilledProcessor = new FulfilledProcessor(localProviderId, fulfilledOrdersProcSleepTimeStr);

        String checkingDeletionOrdersProcSleepTimeStr = PropertiesHolder.getInstance().
                getProperty(ConfigurationPropertyKeys.CHECKING_DELETION_ORDERS_SLEEP_TIME_KEY,
                        ConfigurationPropertyDefaults.CHECKING_DELETION_ORDERS_SLEEP_TIME);

        this.checkingDeletionProcessor = new CheckingDeletionProcessor(orderController, localProviderId, checkingDeletionOrdersProcSleepTimeStr);
        
        String unableToCheckProcSleepTimeStr = PropertiesHolder.getInstance().
                getProperty(ConfigurationPropertyKeys.UNABLE_TO_CHECK_ORDERS_SLEEP_TIME_KEY,
                        ConfigurationPropertyDefaults.UNABLE_TO_CHECK_ORDERS_SLEEP_TIME);
        
        this.unableToCheckStatusProcessor = new UnableToCheckStatusProcessor(localProviderId, unableToCheckProcSleepTimeStr);

        String assignedForDeletionOrdersProcSleepTimeStr = PropertiesHolder.getInstance().
                getProperty(ConfigurationPropertyKeys.ASSIGNED_FOR_DELETION_ORDERS_SLEEP_TIME_KEY,
                        ConfigurationPropertyDefaults.ASSIGNED_FOR_DELETION_ORDERS_SLEEP_TIME);

        this.assignedForDeletionProcessor = new AssignedForDeletionProcessor(localProviderId, assignedForDeletionOrdersProcSleepTimeStr);

        String remoteOrdersStateSynchronizationProcSleepTimeStr = PropertiesHolder.getInstance().
                getProperty(ConfigurationPropertyKeys.REMOTE_ORDER_STATE_SYNCHRONIZATION_SLEEP_TIME_KEY,
                        ConfigurationPropertyDefaults.REMOTE_ORDER_STATE_SYNCHRONIZATION_SLEEP_TIME);
        
        this.remoteOrdersStateSynchronizationProcessor = new RemoteOrdersStateSynchronizationProcessor(localProviderId, remoteOrdersStateSynchronizationProcSleepTimeStr);

        String pausingOrdersProcSleepTimeStr = PropertiesHolder.getInstance().
                getProperty(ConfigurationPropertyKeys.PAUSING_ORDERS_SLEEP_TIME_KEY,
                        ConfigurationPropertyDefaults.PAUSING_ORDERS_SLEEP_TIME);

        this.pausingProcessor = new PausingProcessor(localProviderId, pausingOrdersProcSleepTimeStr);

        String hibernatingOrdersProcSleepTimeStr = PropertiesHolder.getInstance().
                getProperty(ConfigurationPropertyKeys.HIBERNATING_ORDERS_SLEEP_TIME_KEY,
                        ConfigurationPropertyDefaults.HIBERNATING_ORDERS_SLEEP_TIME);

        this.hibernatingProcessor = new HibernatingProcessor(localProviderId, hibernatingOrdersProcSleepTimeStr);

        String stoppingOrdersProcSleepTimeStr = PropertiesHolder.getInstance().
                getProperty(ConfigurationPropertyKeys.STOPPING_ORDERS_SLEEP_TIME_KEY,
                        ConfigurationPropertyDefaults.STOPPING_ORDERS_SLEEP_TIME);

        this.stoppingProcessor = new StoppingProcessor(localProviderId, stoppingOrdersProcSleepTimeStr);
        
        String resumingOrdersProcSleepTimeStr = PropertiesHolder.getInstance().
                getProperty(ConfigurationPropertyKeys.RESUMING_ORDERS_SLEEP_TIME_KEY,
                        ConfigurationPropertyDefaults.RESUMING_ORDERS_SLEEP_TIME);

        this.resumingProcessor = new ResumingProcessor(localProviderId, resumingOrdersProcSleepTimeStr);
        
        this.threadsAreRunning = false;
    }

    /**
     * This method starts all RAS processors, if you defined a new RAS operation and this
     * operation require a new thread to run, you should start this thread at this method.
     */
    public void startRasThreads() {

        if (!this.threadsAreRunning) {
            LOGGER.info(Messages.Log.STARTING_THREADS);
            
            Thread openProcessorThread = new Thread(this.openProcessor, OPEN_PROCESSOR_THREAD_NAME);
            Thread spawningProcessorThread = new Thread(this.spawningProcessor, SPAWNING_PROCESSOR_THREAD_NAME);
            Thread fulfilledProcessorThread = new Thread(this.fulfilledProcessor, FULFILLED_PROCESSOR_THREAD_NAME);
            Thread checkingDeletionProcessorThread = new Thread(checkingDeletionProcessor, CHECKING_DELETION_PROCESSOR_THREAD_NAME);
            Thread failedProcessorThread = new Thread(unableToCheckStatusProcessor, FAILED_PROCESSOR_THREAD_NAME);
            Thread assignedForDeletionProcessorThread = new Thread(assignedForDeletionProcessor, ASSIGNED_FOR_DELETION_PROCESSOR_THREAD_NAME);
            Thread remoteOrdersStateSynchronizationProcessorThread = new Thread(remoteOrdersStateSynchronizationProcessor, REMOTE_ORDER_STATE_SYNCHRONIZATION_PROCESSOR_THREAD_NAME);
            Thread pausingProcessorThread = new Thread(pausingProcessor, PAUSING_PROCESSOR_THREAD_NAME);
            Thread hibernatingProcessorThread = new Thread(hibernatingProcessor, HIBERNATING_PROCESSOR_THREAD_NAME);
            Thread stoppingProcessorThread = new Thread(stoppingProcessor, STOPPING_PROCESSOR_THREAD_NAME);
            Thread resumingProcessorThread = new Thread(resumingProcessor, RESUMING_PROCESSOR_THREAD_NAME);

            openProcessorThread.start();
            spawningProcessorThread.start();
            fulfilledProcessorThread.start();
            checkingDeletionProcessorThread.start();
            failedProcessorThread.start();
            assignedForDeletionProcessorThread.start();
            remoteOrdersStateSynchronizationProcessorThread.start();
            pausingProcessorThread.start();
            hibernatingProcessorThread.start();
            stoppingProcessorThread.start();
            resumingProcessorThread.start();
            
            /*
             * This line of code treats the unlikely situation where 
             * a stopRasThreads call is performed right after a startRasThreads call, 
             * before the processors could change isActive attribute to true.
             * In this situation, the stop operation would do nothing and the 
             * processors would continue running.
             */
            assureAllProcessorsAreActive();
            
            this.threadsAreRunning = true;
        } else {
            LOGGER.info(Messages.Log.THREADS_ARE_ALREADY_RUNNING);
        }
    }

    private void assureAllProcessorsAreActive() {
        while (!this.openProcessor.isActive());
        while (!this.spawningProcessor.isActive());
        while (!this.fulfilledProcessor.isActive());
        while (!this.checkingDeletionProcessor.isActive());
        while (!this.unableToCheckStatusProcessor.isActive());
        while (!this.assignedForDeletionProcessor.isActive());
        while (!this.remoteOrdersStateSynchronizationProcessor.isActive());
        while (!this.pausingProcessor.isActive());
        while (!this.hibernatingProcessor.isActive());
        while (!this.stoppingProcessor.isActive());
        while (!this.resumingProcessor.isActive());
    }

    public void stopRasThreads() {
        if (this.threadsAreRunning) {
            LOGGER.info(Messages.Log.STOPPING_THREADS);
            
            this.openProcessor.stop();
            this.spawningProcessor.stop();
            this.fulfilledProcessor.stop();
            this.checkingDeletionProcessor.stop();
            this.unableToCheckStatusProcessor.stop();
            this.assignedForDeletionProcessor.stop();
            this.remoteOrdersStateSynchronizationProcessor.stop();
            this.pausingProcessor.stop();
            this.hibernatingProcessor.stop();
            this.stoppingProcessor.stop();
            this.resumingProcessor.stop();
            
            this.threadsAreRunning = false;
        } else {
            LOGGER.info(Messages.Log.THREADS_ARE_NOT_RUNNING);
        }
    }
    
    private Long getSleepTimeFromProperties(String key, String defaultValue) {
        String timeStr = PropertiesHolder.getInstance().getProperty(key, defaultValue);
        return Long.valueOf(timeStr);
    }
    
    public void reset() {
        Long openOrdersProcSleepTime = getSleepTimeFromProperties(ConfigurationPropertyKeys.OPEN_ORDERS_SLEEP_TIME_KEY, 
                                                    ConfigurationPropertyDefaults.OPEN_ORDERS_SLEEP_TIME);
        this.openProcessor.setSleepTime(openOrdersProcSleepTime);

        Long spawningOrdersProcSleepTime = getSleepTimeFromProperties(ConfigurationPropertyKeys.SPAWNING_ORDERS_SLEEP_TIME_KEY,
                                                        ConfigurationPropertyDefaults.SPAWNING_ORDERS_SLEEP_TIME);
        this.spawningProcessor.setSleepTime(spawningOrdersProcSleepTime);

        Long fulfilledOrdersProcSleepTime = getSleepTimeFromProperties(ConfigurationPropertyKeys.FULFILLED_ORDERS_SLEEP_TIME_KEY,
                                                         ConfigurationPropertyDefaults.FULFILLED_ORDERS_SLEEP_TIME);
        this.fulfilledProcessor.setSleepTime(fulfilledOrdersProcSleepTime);

        Long checkingDeletionOrdersProcSleepTime = getSleepTimeFromProperties(ConfigurationPropertyKeys.CHECKING_DELETION_ORDERS_SLEEP_TIME_KEY,
                                                                ConfigurationPropertyDefaults.CHECKING_DELETION_ORDERS_SLEEP_TIME);
        this.checkingDeletionProcessor.setSleepTime(checkingDeletionOrdersProcSleepTime);
        
        Long unableToCheckProcSleepTime = getSleepTimeFromProperties(ConfigurationPropertyKeys.UNABLE_TO_CHECK_ORDERS_SLEEP_TIME_KEY,
                                                       ConfigurationPropertyDefaults.UNABLE_TO_CHECK_ORDERS_SLEEP_TIME);
        this.unableToCheckStatusProcessor.setSleepTime(unableToCheckProcSleepTime);

        Long assignedForDeletionOrdersProcSleepTime = getSleepTimeFromProperties(ConfigurationPropertyKeys.ASSIGNED_FOR_DELETION_ORDERS_SLEEP_TIME_KEY,
                                                                   ConfigurationPropertyDefaults.ASSIGNED_FOR_DELETION_ORDERS_SLEEP_TIME);
        this.assignedForDeletionProcessor.setSleepTime(assignedForDeletionOrdersProcSleepTime);

        Long remoteOrdersStateSynchronizationProcSleepTimeStr = getSleepTimeFromProperties(ConfigurationPropertyKeys.REMOTE_ORDER_STATE_SYNCHRONIZATION_SLEEP_TIME_KEY,
                                                                             ConfigurationPropertyDefaults.REMOTE_ORDER_STATE_SYNCHRONIZATION_SLEEP_TIME);
        this.remoteOrdersStateSynchronizationProcessor.setSleepTime(remoteOrdersStateSynchronizationProcSleepTimeStr);
      
        Long pausingProcSleepTimeStr = getSleepTimeFromProperties(ConfigurationPropertyKeys.PAUSING_ORDERS_SLEEP_TIME_KEY,
                                                                             ConfigurationPropertyDefaults.PAUSING_ORDERS_SLEEP_TIME);
        this.pausingProcessor.setSleepTime(pausingProcSleepTimeStr);
      
        Long hibernatingProcSleepTimeStr = getSleepTimeFromProperties(ConfigurationPropertyKeys.HIBERNATING_ORDERS_SLEEP_TIME_KEY,
                                                                             ConfigurationPropertyDefaults.HIBERNATING_ORDERS_SLEEP_TIME);
        this.hibernatingProcessor.setSleepTime(hibernatingProcSleepTimeStr);
        
        Long stoppingProcSleepTimeStr = getSleepTimeFromProperties(ConfigurationPropertyKeys.STOPPING_ORDERS_SLEEP_TIME_KEY,
                ConfigurationPropertyDefaults.STOPPING_ORDERS_SLEEP_TIME);
        this.stoppingProcessor.setSleepTime(stoppingProcSleepTimeStr);
      
        Long resumingProcSleepTimeStr = getSleepTimeFromProperties(ConfigurationPropertyKeys.RESUMING_ORDERS_SLEEP_TIME_KEY,
                                                                             ConfigurationPropertyDefaults.RESUMING_ORDERS_SLEEP_TIME);
        this.resumingProcessor.setSleepTime(resumingProcSleepTimeStr);
    }
}
