package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import cloud.fogbow.ras.constants.Messages;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/*
This class helps to cope Plugins that they have asynchronous instances creation.

Problem: It might generate resource trash in the cloud due to the fact that
If the RAS shutdown this class will lost its data in memory.

Note: This context helps to fix this issue:
- https://github.com/fogbow/resource-allocation-service/issues/435
- https://github.com/fogbow/resource-allocation-service/issues/473
- https://github.com/fogbow/resource-allocation-service/issues/536
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
        LOGGER.debug(String.format(Messages.Log.START_ASYNC_INSTANCE_CREATION_S, instanceId));
        defineAsCreating(instanceId);
        return new Callbacks().builder()
                .doOnComplete(() -> {
                    defineAsCreated(instanceId);
                    LOGGER.debug(String.format(Messages.Log.END_ASYNC_INSTANCE_CREATION_S, instanceId));
                })
                .doOnError((faultMessage) -> {
                    defineAsFailed(instanceId, faultMessage);
                    LOGGER.debug(String.format(Messages.Log.ERROR_ASYNC_INSTANCE_CREATION_S, instanceId));
                }).build();
    }

    /*
    It must remove the instance of the map.
    */
    public void endCreation(String instanceId) {
        if (this.creating.get(instanceId) != null) {
            this.creating.remove(instanceId);
        }
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
        Status create = Status.create();
        this.creating.put(instanceId, create);
    }

    /*
    It must set as FAILED in the map when the resource has a problem at the creation in the cloud.
     */
    private void defineAsFailed(String instanceId, String faultMessage) {
        Status fail = Status.fail(faultMessage);
        this.creating.put(instanceId, fail);
    }

    /*
    It must remove the instance of the map whether the status is CREATING.
    */
    private void defineAsCreated(String instanceId) {
        if (this.creating.get(instanceId) != null &&
                this.creating.get(instanceId).getType() == StatusType.CREATING) {
            this.creating.remove(instanceId);
        }
    }

    public static class Status {
        private StatusType type;
        private String faultMessage;

        private Status(StatusType type, String faultMessage) {
            this.type = type;
            this.faultMessage = faultMessage;
        }

        public static Status create() {
            return new Status(StatusType.CREATING, null);
        }

        public static Status fail(String faultMessage) {
            return new Status(StatusType.FAILED, faultMessage);
        }

        public StatusType getType() {
            return type;
        }

        public String getFaultMessage() {
            return faultMessage;
        }
    }

    public enum StatusType {
        CREATING, FAILED
    }

    public class Callbacks {

        private Runnable doOnComplete;
        private Consumer<String> doOnError;

        public Builder builder() {
            return new Builder();
        }

        public void runOnComplete() {
            this.doOnComplete.run();
        }

        public void runOnError(String faultMessage) {
            this.doOnError.accept(faultMessage);
        }

        private void setDoOnComplete(Runnable doOnComplete) {
            this.doOnComplete = doOnComplete;
        }

        private void setDoOnError(Consumer<String> doOnError) {
            this.doOnError = doOnError;
        }

        private class Builder {
            private Callbacks asyncInstanceCreationCallbacks;

            private Builder() {
                this.asyncInstanceCreationCallbacks = new Callbacks();
            }

            public Builder doOnComplete(Runnable doOnComplete) {
                this.asyncInstanceCreationCallbacks.setDoOnComplete(doOnComplete);
                return this;
            }

            public Builder doOnError(Consumer<String> doOnError) {
                this.asyncInstanceCreationCallbacks.setDoOnError(doOnError);
                return this;
            }

            public Callbacks build() {
                return asyncInstanceCreationCallbacks;
            }
        }

    }

}
