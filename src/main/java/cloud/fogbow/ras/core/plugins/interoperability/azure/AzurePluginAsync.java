package cloud.fogbow.ras.core.plugins.interoperability.azure;

import cloud.fogbow.ras.core.plugins.interoperability.azure.util.CreatingInstanceManager;

// TODO(chico) - Create the issue in the GitHub
/*
This class helps to cope Azure instance creation asynchronous.
Note: This context helps to fix this issue:

Problem: It might generate resource trash in the Azure cloud due to the fact that
If the RAS shutdown this class will lost its data in memory.
 */
public abstract class AzurePluginAsync {

    private final CreatingInstanceManager creatingInstanceManager;

    protected AzurePluginAsync() {
        String parentClassKey = this.getClass().getSuperclass().getSimpleName();
        this.creatingInstanceManager = new CreatingInstanceManager(parentClassKey);
    }

    /*
    It must be used in the parent class soon after it makes the Azure(SDK) asynchronous operation.

    @return It is a callback that it must be used when the Azure(SDK) finish asynchronous operation;
    Suggestion: Use "finishAsyncCreationCallback" as attribute name.
     */
    protected Runnable startAsyncCreation(String instanceId) {
        this.creatingInstanceManager.defineAsCreating(instanceId);
        return () -> creatingInstanceManager.defineAsCreated(instanceId);
    }

    /*
    It must check if is Asynchronous operation has or has not finished.
     */
    protected boolean isCreatingAsync(String instanceId) {
        return this.creatingInstanceManager.isCreating(instanceId);
    }

}
