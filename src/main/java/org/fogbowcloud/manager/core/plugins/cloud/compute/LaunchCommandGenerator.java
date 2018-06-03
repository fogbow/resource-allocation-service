package org.fogbowcloud.manager.core.plugins.cloud.compute;

import org.fogbowcloud.manager.core.models.orders.ComputeOrder;

public interface LaunchCommandGenerator {
    public String createLaunchCommand(ComputeOrder order);
}
