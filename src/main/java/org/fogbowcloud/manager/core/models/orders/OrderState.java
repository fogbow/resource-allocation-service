package org.fogbowcloud.manager.core.models.orders;

public enum OrderState {
    OPEN,
    PENDING,
    SPAWNING,
    FULFILLED,
    FAILED,
    CLOSED;
}
