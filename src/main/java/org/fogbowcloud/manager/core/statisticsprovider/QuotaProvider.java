package org.fogbowcloud.manager.core.statisticsprovider;

import org.fogbowcloud.manager.core.exceptions.QuotaException;
import org.fogbowcloud.manager.core.models.quotas.ComputeQuota;
import org.fogbowcloud.manager.core.models.token.FederationUser;

public interface QuotaProvider {
	
	public ComputeQuota getComputeQuota(FederationUser federationUser) throws QuotaException;
}
