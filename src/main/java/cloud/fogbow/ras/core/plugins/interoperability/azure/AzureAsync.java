package cloud.fogbow.ras.core.plugins.interoperability.azure;

import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.OrderInstance;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AsyncInstanceCreationManager;

import javax.annotation.Nullable;

public abstract class AzureAsync<T> {

    private AsyncInstanceCreationManager asyncInstanceCreation = new AsyncInstanceCreationManager();

    public Runnable startIntanceCreation(String instanceId) {
        return this.asyncInstanceCreation.startCreation(instanceId);
    }

    /*
    It checks if the instance is still in creating by cloud
     */
    @Nullable
    public T getCreatingInstance(String instanceId) {
        if (this.asyncInstanceCreation.isCreating(instanceId)) {
            return (T) new OrderInstance(instanceId, InstanceState.CREATING.getValue());
        }
        return null;
    }

}
