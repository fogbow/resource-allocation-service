package cloud.fogbow.ras.api.http.response;

public enum InstanceState {
    DISPATCHED("dispatched"),
    READY("ready"),
    CREATING("creating"),
    UNAVAILABLE("unavailable"),
    FAILED("failed"),
    INCONSISTENT("inconsistent");

    private String value;

    private InstanceState(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
