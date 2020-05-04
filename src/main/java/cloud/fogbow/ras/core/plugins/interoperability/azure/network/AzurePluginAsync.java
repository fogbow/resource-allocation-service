package cloud.fogbow.ras.core.plugins.interoperability.azure.network;

import cloud.fogbow.ras.core.plugins.interoperability.azure.util.CreatingInstanceManager;

// TODO(chico) - Finish commentaries
/*

 */
public abstract class AzurePluginAsync {

    private final CreatingInstanceManager creatingInstanceManager;

    protected AzurePluginAsync() {
        String parentClassKey = this.getClass().getSuperclass().getSimpleName();
        this.creatingInstanceManager = new CreatingInstanceManager(parentClassKey);
    }

    /*
    It must be used in the parent class soon after it makes the asynchronous operation.

    @return It is a callback that it must be used the the; Suggestion;
    Use "finishAsyncCreationCallback" as attribute name.
     */
    protected Runnable startAsyncCreation(String instanceId) {
        this.creatingInstanceManager.defineAsCreating(instanceId);
        return () -> creatingInstanceManager.defineAsCreated(instanceId);
    }

    /*
    It must check is Asynchronous operation has not finished.
     */
    public boolean isCreatingAsync(String instanceId) {
        return this.creatingInstanceManager.isCreating(instanceId);
    }

}
