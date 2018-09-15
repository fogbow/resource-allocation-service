package org.fogbowcloud.ras.core.models.orders;

public enum OrderState {
    OPEN,
    PENDING,
    SPAWNING,
    FULFILLED,
    FAILED,
    CLOSED,
    DEACTIVATED
    // an order that has been closed is stored twice in stable storage:
    // one when the order is deleted (but instanceId != null),
    // and another when it is deactivated (when instanceId == null);
    // we need the deactivate state so that the add in the timestamp table won't break.
}
