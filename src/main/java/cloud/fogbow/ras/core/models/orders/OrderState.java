package cloud.fogbow.ras.core.models.orders;

public enum OrderState {
    OPEN("OPEN"),
    SELECTED("SELECTED"),
    REMOTE("REMOTE"),
    SPAWNING("SPAWNING"),
    FULFILLED("FULFILLED"),
    FAILED_AFTER_SUCCESSFUL_REQUEST("FAILED_AFTER_SUCCESSFUL_REQUEST"),
    FAILED_ON_REQUEST("FAILED_ON_REQUEST"),
    UNABLE_TO_CHECK_STATUS("UNABLE_TO_CHECK_STATUS"),
    ASSIGNED_FOR_DELETION("ASSIGNED_FOR_DELETION"),
    CHECKING_DELETION("CHECKING_DELETION"),
    CLOSED("CLOSED");

    private final String repr;

    OrderState(String repr) {
        this.repr = repr;
    }
}
