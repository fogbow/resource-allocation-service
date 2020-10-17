package cloud.fogbow.ras.core;

public class SynchronizationManager {
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

    public boolean isReloading() {
        return this.reloading;
    }

    public void reload() {
        this.reloading = true;
        while (ApplicationFacade.getInstance().getOnGoingRequests() != 0)
            ;
        this.processorsThreadController.stopRasThreads();
        this.doReload();
        this.processorsThreadController.startRasThreads();
        this.reloading = false;
    }

    private void doReload() {
        // ToDo: Add reload functionality
    }
}
