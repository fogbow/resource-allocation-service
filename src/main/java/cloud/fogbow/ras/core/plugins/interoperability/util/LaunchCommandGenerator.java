package cloud.fogbow.ras.core.plugins.interoperability.util;

import cloud.fogbow.ras.core.models.orders.ComputeOrder;

public interface LaunchCommandGenerator {

    public String createLaunchCommand(ComputeOrder order);
}
