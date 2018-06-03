package org.fogbowcloud.manager.core;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.processors.ClosedProcessor;
import org.fogbowcloud.manager.core.processors.FulfilledProcessor;
import org.fogbowcloud.manager.core.processors.OpenProcessor;
import org.fogbowcloud.manager.core.processors.SpawningProcessor;

public class ProcessorsThreadController {

    private Thread openProcessorThread;
    private Thread spawningProcessorThread;
    private Thread fulfilledProcessorThread;
    private Thread closedProcessorThread;

    private static final Logger LOGGER = Logger.getLogger(ProcessorsThreadController.class);

    public ProcessorsThreadController(OpenProcessor openProcessor, SpawningProcessor spawningProcessor,
                                      FulfilledProcessor fulfilledProcessor, ClosedProcessor closedProcessor) {

        this.openProcessorThread = new Thread(openProcessor);
        this.spawningProcessorThread = new Thread(spawningProcessor);
        this.fulfilledProcessorThread = new Thread(fulfilledProcessor);
        this.closedProcessorThread = new Thread(closedProcessor);

        this.startManagerThreads();
    }

    /**
     * This method starts all manager processors, if you defined a new manager operation and this
     * operation require a new thread to run, you should start this thread at this method.
     */
    private void startManagerThreads() {
        LOGGER.info("Starting all processor threads");
        this.openProcessorThread.start();
        this.spawningProcessorThread.start();
        this.fulfilledProcessorThread.start();
        this.closedProcessorThread.start();
    }
}
