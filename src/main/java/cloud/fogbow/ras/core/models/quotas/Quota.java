package cloud.fogbow.ras.core.models.quotas;

import cloud.fogbow.ras.core.models.quotas.allocation.Allocation;

public abstract class Quota {
    public abstract Allocation getTotalQuota();

    public abstract Allocation getUsedQuota();

    public abstract Allocation getAvailableQuota();

    public abstract String toString();
}
