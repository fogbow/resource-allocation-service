package org.fogbowcloud.manager.core.statisticsprovider;

import org.fogbowcloud.manager.core.exceptions.QuotaException;
import org.fogbowcloud.manager.core.models.quotas.ComputeQuota;

public interface StatisticsProvider {
	
	public ComputeQuota getSharedQuota();
	
	public ComputeQuota getUsedQuota() throws QuotaException;
	
	public ComputeQuota getInUseQuota();
}
