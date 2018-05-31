package org.fogbowcloud.manager.core.statisticsprovider;

import org.fogbowcloud.manager.core.models.quotas.ComputeQuota;
import org.fogbowcloud.manager.core.models.token.FederationUser;

public class RemoteStatisticsProvider implements StatisticsProvider {

	@Override
	public ComputeQuota getSharedQuota() {
		return null;
	}

	@Override
	public ComputeQuota getUsedQuota() {
		return null;
	}

	@Override
	public ComputeQuota getInUseQuota(FederationUser federationUser) {
		return null;
	}

}
