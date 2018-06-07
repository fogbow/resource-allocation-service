package org.fogbowcloud.manager.core;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.processors.ClosedProcessor;
import org.fogbowcloud.manager.core.processors.FulfilledProcessor;
import org.fogbowcloud.manager.core.processors.OpenProcessor;
import org.fogbowcloud.manager.core.processors.SpawningProcessor;
import org.fogbowcloud.manager.utils.PropertiesUtil;
import org.fogbowcloud.manager.utils.SshConnectivityUtil;
import org.fogbowcloud.manager.utils.TunnelingServiceUtil;

public class ProcessorsThreadController {

    private Thread openProcessorThread;
    private Thread spawningProcessorThread;
    private Thread fulfilledProcessorThread;
    private Thread closedProcessorThread;

    private static final Logger LOGGER = Logger.getLogger(ProcessorsThreadController.class);

    public ProcessorsThreadController(String localMemberId) {


        String openOrdersProcSleepTimeStr = PropertiesUtil.getInstance().
                getProperty(ConfigurationConstants.OPEN_ORDERS_SLEEP_TIME_KEY,
                        DefaultConfigurationConstants.OPEN_ORDERS_SLEEP_TIME);

        OpenProcessor openProcessor = new OpenProcessor(localMemberId, openOrdersProcSleepTimeStr);

        String spawningOrdersProcSleepTimeStr = PropertiesHolder.getInstance().
                getProperty(ConfigurationConstants.SPAWNING_ORDERS_SLEEP_TIME_KEY,
                        DefaultConfigurationConstants.SPAWNING_ORDERS_SLEEP_TIME);

        TunnelingServiceUtil tunnelingServiceUtil = TunnelingServiceUtil.getInstance();
        SshConnectivityUtil sshConnectivityUtil = SshConnectivityUtil.getInstance();

        SpawningProcessor spawningProcessor =
                new SpawningProcessor(localMemberId, tunnelingServiceUtil,
                        sshConnectivityUtil, spawningOrdersProcSleepTimeStr);

        String fulfilledOrdersProcSleepTimeStr = PropertiesHolder.getInstance().
                getProperty(ConfigurationConstants.FULFILLED_ORDERS_SLEEP_TIME_KEY,
                        DefaultConfigurationConstants.FULFILLED_ORDERS_SLEEP_TIME);

        FulfilledProcessor fulfilledProcessor =
                new FulfilledProcessor(localMemberId, tunnelingServiceUtil,
                        sshConnectivityUtil, fulfilledOrdersProcSleepTimeStr);

        String closedOrdersProcSleepTimeStr = PropertiesHolder.getInstance().
                getProperty(ConfigurationConstants.CLOSED_ORDERS_SLEEP_TIME_KEY,
                        DefaultConfigurationConstants.CLOSED_ORDERS_SLEEP_TIME);

        ClosedProcessor closedProcessor = new ClosedProcessor(closedOrdersProcSleepTimeStr);

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
