package cloud.fogbow.ras.core.plugins.interoperability.azure;

import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AsyncInstanceCreationManager;

import javax.annotation.Nullable;

public interface AzureAsync<T> {

    AsyncInstanceCreationManager asyncInstanceCreation = new AsyncInstanceCreationManager();

    /*
    It must be used in the requestInstance method context
     */
    default Runnable startInstanceCreation(String instanceId) {
        return this.asyncInstanceCreation.startCreation(instanceId);
    }

    /*
    It must be used in the getInstance method context;
     */
    @Nullable
    default T getCreatingInstance(String instanceId) {
        if (this.asyncInstanceCreation.isCreating(instanceId)) {
            return buildCreatingInstance(instanceId);
        }
        return null;
    }

    /*
    It must return the OrderInstance with Creating state
     */
    T buildCreatingInstance(String instanceId);

}
