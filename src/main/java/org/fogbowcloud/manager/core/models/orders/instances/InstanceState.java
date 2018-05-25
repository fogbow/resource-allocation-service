package org.fogbowcloud.manager.core.models.orders.instances;

public enum InstanceState {
    READY,
    INACTIVE,
    SPAWNING,
    FAILED,
    INCONSISTENT; // This state signals a bug in the code.
}
