package org.fogbowcloud.ras.core.models.quotas;

import org.fogbowcloud.ras.core.models.quotas.allocation.Allocation;

public interface Quota {
    public Allocation getTotalQuota();

    public Allocation getUsedQuota();

    public Allocation getAvailableQuota();
}
