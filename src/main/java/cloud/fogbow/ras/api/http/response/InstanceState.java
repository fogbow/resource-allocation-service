package cloud.fogbow.ras.api.http.response;

public enum InstanceState {
    DISPATCHED("dispatched"),
    READY("ready"),
    CREATING("creating"),
    BUSY("busy"),
    FAILED("failed"),
    ERROR("error"),
    DELETING("deleting"),
    DELETED("deleted"),
    UNKNOWN("unknown"),
    PAUSING("pausing"),
    PAUSED("paused"),
    HIBERNATING("hibernating"),
    HIBERNATED("hibernated"),
    STOPPING("stopping"),
    STOPPED("stopped"),
    RESUMING("resuming"),
    INCONSISTENT("inconsistent");

    private String value;

    private InstanceState(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
