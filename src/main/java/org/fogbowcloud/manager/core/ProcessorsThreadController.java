package org.fogbowcloud.manager.core;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.processors.ClosedProcessor;
import org.fogbowcloud.manager.core.processors.FulfilledProcessor;
import org.fogbowcloud.manager.core.processors.OpenProcessor;
import org.fogbowcloud.manager.core.processors.SpawningProcessor;

public class ProcessorsThreadController {
    private static final Logger LOGGER = Logger.getLogger(ProcessorsThreadController.class);

    private final Thread openProcessorThread;
    private final Thread spawningProcessorThread;
    private final Thread fulfilledProcessorThread;
    private final Thread closedProcessorThread;

    public ProcessorsThreadController(String localMemberId) {
        String openOrdersProcSleepTimeStr = PropertiesHolder.getInstance().
                getProperty(ConfigurationConstants.OPEN_ORDERS_SLEEP_TIME_KEY,
                        DefaultConfigurationConstants.OPEN_ORDERS_SLEEP_TIME);

        OpenProcessor openProcessor = new OpenProcessor(localMemberId, openOrdersProcSleepTimeStr);

        String spawningOrdersProcSleepTimeStr = PropertiesHolder.getInstance().
                getProperty(ConfigurationConstants.SPAWNING_ORDERS_SLEEP_TIME_KEY,
                        DefaultConfigurationConstants.SPAWNING_ORDERS_SLEEP_TIME);

        SpawningProcessor spawningProcessor = new SpawningProcessor(localMemberId, spawningOrdersProcSleepTimeStr);

        String fulfilledOrdersProcSleepTimeStr = PropertiesHolder.getInstance().
                getProperty(ConfigurationConstants.FULFILLED_ORDERS_SLEEP_TIME_KEY,
                        DefaultConfigurationConstants.FULFILLED_ORDERS_SLEEP_TIME);

        FulfilledProcessor fulfilledProcessor = new FulfilledProcessor(localMemberId, fulfilledOrdersProcSleepTimeStr);

        String closedOrdersProcSleepTimeStr = PropertiesHolder.getInstance().
                getProperty(ConfigurationConstants.CLOSED_ORDERS_SLEEP_TIME_KEY,
                        DefaultConfigurationConstants.CLOSED_ORDERS_SLEEP_TIME);

        ClosedProcessor closedProcessor = new ClosedProcessor(closedOrdersProcSleepTimeStr);

        this.openProcessorThread = new Thread(openProcessor, "open-proc");
        this.spawningProcessorThread = new Thread(spawningProcessor, "spawning-proc");
        this.fulfilledProcessorThread = new Thread(fulfilledProcessor, "fulfilled-proc");
        this.closedProcessorThread = new Thread(closedProcessor, "closed-proc");
    }

    /**
     * This method starts all manager processors, if you defined a new manager operation and this
     * operation require a new thread to run, you should start this thread at this method.
     */
    public void startManagerThreads() {
        LOGGER.info("Starting processor threads");
        this.openProcessorThread.start();
        this.spawningProcessorThread.start();
        this.fulfilledProcessorThread.start();
        this.closedProcessorThread.start();
    }
}
