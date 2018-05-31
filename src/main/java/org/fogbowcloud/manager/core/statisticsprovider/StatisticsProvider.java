package org.fogbowcloud.manager.core.statisticsprovider;

import org.fogbowcloud.manager.core.exceptions.QuotaException;
import org.fogbowcloud.manager.core.models.quotas.ComputeQuota;
import org.fogbowcloud.manager.core.models.token.FederationUser;

public interface StatisticsProvider {
	
	public ComputeQuota getSharedQuota() throws QuotaException;
	
	public ComputeQuota getUsedQuota() throws QuotaException;
	
	public ComputeQuota getInUseQuota(FederationUser federationUser) throws QuotaException;
}
