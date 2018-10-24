package org.fogbowcloud.ras.core.plugins.interoperability.util;

import org.fogbowcloud.ras.core.models.orders.ComputeOrder;

public interface LaunchCommandGenerator {

    public String createLaunchCommand(ComputeOrder order);
}
