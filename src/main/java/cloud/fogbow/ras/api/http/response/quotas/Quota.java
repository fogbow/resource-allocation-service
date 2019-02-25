package cloud.fogbow.ras.api.http.response.quotas;

import cloud.fogbow.ras.api.http.response.quotas.allocation.Allocation;

public abstract class Quota {
    public abstract Allocation getTotalQuota();

    public abstract Allocation getUsedQuota();

    public abstract Allocation getAvailableQuota();

    public abstract String toString();
}
