package cloud.fogbow.ras.core.plugins.interoperability.azure;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.OrderInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AsyncInstanceCreationManager;
import org.apache.log4j.Logger;

import javax.annotation.Nullable;

import static cloud.fogbow.ras.core.plugins.interoperability.azure.util.AsyncInstanceCreationManager.Status;

public interface AzureAsync<T extends OrderInstance> {

    Logger LOGGER = Logger.getLogger(AzureAsync.class);

    AsyncInstanceCreationManager asyncInstanceCreation = new AsyncInstanceCreationManager();
    int SLEEP_TIME = 1000; // 1 second

    /*
    It must be used in the requestInstance method context before asynchronous Azure SDK request;
    Also callbacks(return) must be used at the response of asynchronous Azure SDK request.
     */
    default AsyncInstanceCreationManager.Callbacks startInstanceCreation(String instanceId) {
        return this.asyncInstanceCreation.startCreation(instanceId);
    }

    /*
    It must be used in the getInstance method context; It returns the current Creating Instance Status.
     */
    @Nullable
    default T getCreatingInstance(String instanceId) throws UnexpectedException {
        Status currentStatus = this.asyncInstanceCreation.getStatus(instanceId);
        boolean isInstanceCreationFinished = currentStatus == null;
        if (isInstanceCreationFinished) {
            return null;
        }

        T creatingInstance = buildCreatingInstance(instanceId);
        switch (currentStatus) {
            case CREATING:
                creatingInstance.setState(InstanceState.CREATING);
                break;
            case FAILED:
                creatingInstance.setState(InstanceState.FAILED);
                break;
            default:
                throw new UnexpectedException(Messages.Exception.UNEXPECTED_ERROR);
        }

        return creatingInstance;
    }

    /*
    It must be used in the requestInstance method context after asynchronous Azure SDK request;
    It waits and checks whether asynchronous Azure SDK request had an error in the request.
     */
    default void waitAndCheckForInstanceCreationFailed(String instanceId) throws FogbowException {
        try {
            Thread.sleep(SLEEP_TIME);
        } catch (InterruptedException e) {
            LOGGER.warn(Messages.Warn.SLEEP_THREAD_INTERRUPTED);
        } finally {
            Status status = this.asyncInstanceCreation.getStatus(instanceId);
            if (status == Status.FAILED) {
                throw new FogbowException();
            }
        }
    }

    /*
    It must return a specific OrderInstance(T).
     */
    T buildCreatingInstance(String instanceId);

}
