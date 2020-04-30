package cloud.fogbow.ras.core.plugins.interoperability.azure.network;

import cloud.fogbow.ras.core.plugins.interoperability.azure.util.CreatingInstanceManager;

public class AzureAsync {

    protected final CreatingInstanceManager creatingInstanceManager;

    protected AzureAsync(Class classObj) {
        this.creatingInstanceManager = new CreatingInstanceManager(classObj.getSimpleName());
    }

    protected void defineAsCreating(String instanceId) {
        this.creatingInstanceManager.defineAsCreating(instanceId);
    }

}
