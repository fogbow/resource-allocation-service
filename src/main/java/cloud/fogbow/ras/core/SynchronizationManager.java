package cloud.fogbow.ras.core;

import org.apache.log4j.Logger;

import cloud.fogbow.ras.constants.Messages;

public class SynchronizationManager {
    private static final Logger LOGGER = Logger.getLogger(SynchronizationManager.class);
    
    private static SynchronizationManager instance;
    private boolean reloading;
    private ProcessorsThreadController processorsThreadController;

    private SynchronizationManager() {
        this.reloading = false;
    }

    public static synchronized SynchronizationManager getInstance() {
        if (instance == null) {
            instance = new SynchronizationManager();
        }
        return instance;
    }

    public void setProcessorsThreadController(ProcessorsThreadController processorsThreadController) {
        this.processorsThreadController = processorsThreadController;
    }

    public void setAsReloading() {
        this.reloading = true;
    }

    public boolean isReloading() {
        return this.reloading;
    }

    public void reload() {
        this.processorsThreadController.stopRasThreads();
        this.doReload();
        this.processorsThreadController.startRasThreads();
        this.reloading = false;
    }
    
    public void setAsNotReloading() {
    	this.reloading = false;
    }

    private void doReload() {
        LOGGER.info(Messages.Log.RESETTING_PROCESSORS_CONFIGURATION);
        this.processorsThreadController.reset();
    }
}
