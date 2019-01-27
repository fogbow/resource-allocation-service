package cloud.fogbow.ras.core.models.quotas;

import cloud.fogbow.ras.core.models.quotas.allocation.Allocation;

public interface Quota {
    public Allocation getTotalQuota();

    public Allocation getUsedQuota();

    public Allocation getAvailableQuota();
}
