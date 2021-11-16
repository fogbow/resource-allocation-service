package cloud.fogbow.ras.core.models.orders;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.ras.constants.Messages;

public enum OrderState {
    OPEN("OPEN"),
    SELECTED("SELECTED"),
    PENDING("PENDING"),
    SPAWNING("SPAWNING"),
    FULFILLED("FULFILLED"),
    FAILED_AFTER_SUCCESSFUL_REQUEST("FAILED_AFTER_SUCCESSFUL_REQUEST"),
    FAILED_ON_REQUEST("FAILED_ON_REQUEST"),
    UNABLE_TO_CHECK_STATUS("UNABLE_TO_CHECK_STATUS"),
    ASSIGNED_FOR_DELETION("ASSIGNED_FOR_DELETION"),
    CHECKING_DELETION("CHECKING_DELETION"),
    CLOSED("CLOSED"),
    PAUSED("PAUSED"),
    HIBERNATED("HIBERNATED"),
    PAUSING("PAUSING"),
    HIBERNATING("HIBERNATING"),
    RESUMING("RESUMING"),
    STOPPED("STOPPED"),
    STOPPING("STOPPING");

    private final String repr;

    OrderState(String repr) {
        this.repr = repr;
    }
    
	public static OrderState fromValue(String value) throws InvalidParameterException {
		for (OrderState state: OrderState.values()) {
			if (state.repr.equals(value)) {
				return state;
			}
		}
		
		throw new InvalidParameterException(String.format(Messages.Exception.UNKNOWN_ORDER_STATE, value));
	}
	
	public String getValue() {
	    return repr;
	}
}
