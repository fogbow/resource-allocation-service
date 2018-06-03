package org.fogbowcloud.manager.core.models.quotas;

import org.fogbowcloud.manager.core.models.quotas.allocation.Allocation;

public interface Quota {
    public Allocation getTotalQuota();

    public Allocation getUsedQuota();

    public Allocation getAvailableQuota();
}
