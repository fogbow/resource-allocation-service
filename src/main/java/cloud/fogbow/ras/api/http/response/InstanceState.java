package cloud.fogbow.ras.api.http.response;

public enum InstanceState {
    DISPATCHED("dispatched"),
    READY("ready"),
    CREATING("creating"),
    BUSY("busy"),
    FAILED("failed"),
    ERROR("error"),
    UNKNOWN("unknown"),
    INCONSISTENT("inconsistent");

    private String value;

    private InstanceState(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
