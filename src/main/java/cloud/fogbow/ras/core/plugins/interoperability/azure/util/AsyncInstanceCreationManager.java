package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import cloud.fogbow.ras.constants.Messages;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
This class helps to cope Plugins that they have asynchronous instances creation.

Problem: It might generate resource trash in the cloud due to the fact that
If the RAS shutdown this class will lost its data in memory.

Note: This context helps to fix this issue:
- https://github.com/fogbow/resource-allocation-service/issues/435
- https://github.com/fogbow/resource-allocation-service/issues/473
 */
public class AsyncInstanceCreationManager {

    private static final Logger LOGGER = Logger.getLogger(AsyncInstanceCreationManager.class);

    private final static Map<String, Status> creating = new ConcurrentHashMap<>();

    /*
    It must be used soon before the plugin makes asynchronous creation operation in the cloud.

    @return They are callbacks that they must be used when the cloud finish asynchronous creation operation
    in the cloud;
    Suggestion: Use "finishCreationCallbacks" as attribute name.
    */
    public Callbacks startCreation(String instanceId) {
        LOGGER.debug(String.format(Messages.Info.START_ASYNC_INSTANCE_CREATION_S, instanceId));
        defineAsCreating(instanceId);
        return new Callbacks().builder()
                .doOnComplete(() -> {
                    defineAsCreated(instanceId);
                    LOGGER.debug(String.format(Messages.Info.END_ASYNC_INSTANCE_CREATION_S, instanceId));
                })
                .doOnError(() -> {
                    defineAsFailed(instanceId);
                    LOGGER.debug(String.format(Messages.Info.ERROR_ASYNC_INSTANCE_CREATION_S, instanceId));
                }).build();
    }

    /*
    It must return the current instance creation status.
    */
    public Status getStatus(String instanceId) {
        return this.creating.get(instanceId);
    }

    /*
    It must set as CREATING in the map when the resource be not created in the cloud yet.
     */
    private void defineAsCreating(String instanceId) {
        this.creating.put(instanceId, Status.CREATING);
    }

    /*
    It must set as FAILED in the map when the resource has a problem at the creation in the cloud.
     */
    private void defineAsFailed(String instanceId) {
        this.creating.put(instanceId, Status.FAILED);
    }

    /*
    It must remove the instance of the map when the resource be created in the cloud.
    */
    private void defineAsCreated(String instanceId) {
        this.creating.remove(instanceId);
    }

    public enum Status {
        CREATING, FAILED
    }

    public class Callbacks {

        private Runnable doOnComplete;
        private Runnable doOnError;

        public Builder builder() {
            return new Builder();
        }

        public void runOnComplete() {
            this.doOnComplete.run();
        }

        public void runOnError() {
            this.doOnError.run();
        }

        private void setDoOnComplete(Runnable doOnComplete) {
            this.doOnComplete = doOnComplete;
        }

        private void setDoOnError(Runnable doOnError) {
            this.doOnError = doOnError;
        }

        private class Builder {
            private Callbacks callbacks;

            private Builder() {
                this.callbacks = new Callbacks();
            }

            public Builder doOnComplete(Runnable doOnComplete) {
                this.callbacks.setDoOnComplete(doOnComplete);
                return this;
            }

            public Builder doOnError(Runnable doOnError) {
                this.callbacks.setDoOnError(doOnError);
                return this;
            }

            public Callbacks build() {
                return callbacks;
            }
        }

    }

}
