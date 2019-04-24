package cloud.fogbow.ras.core.models.orders;

public enum OrderState {
    OPEN("OPEN"),
    PENDING("PENDING"),
    SPAWNING("SPAWNING"),
    FULFILLED("FULFILLED"),
    FAILED_AFTER_SUCCESSFUL_REQUEST("FAILED_AFTER_SUCCESSFUL_REQUEST"),
    FAILED_ON_REQUEST("FAILED_ON_REQUEST"),
    UNABLE_TO_CHECK_STATUS("UNABLE_TO_CHECK_STATUS"),
    CLOSED("CLOSED"),
    DEACTIVATED("DEACTIVATED");
    // an order that has been closed is stored twice in stable storage:
    // one when the order is deleted (but instanceId != null),
    // and another when it is deactivated (when instanceId == null);
    // we need the deactivate state so that the add in the timestamp table won't break.

    private final String repr;

    OrderState(String repr) {
        this.repr = repr;
    }
}
