package org.fogbowcloud.manager.core.statisticsprovider;

import org.fogbowcloud.manager.core.models.quotas.ComputeQuota;

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
	public ComputeQuota getInUseQuota() {
		return null;
	}

}
