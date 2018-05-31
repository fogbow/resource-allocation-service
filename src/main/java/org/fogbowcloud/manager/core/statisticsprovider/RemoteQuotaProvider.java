package org.fogbowcloud.manager.core.statisticsprovider;

import org.fogbowcloud.manager.core.models.quotas.ComputeQuota;
import org.fogbowcloud.manager.core.models.token.FederationUser;

public class RemoteQuotaProvider implements QuotaProvider {
	
	private String memberId; 
	
	public RemoteQuotaProvider(String memberId) {
		this.memberId = memberId;
	}
	
	@Override
	public ComputeQuota getComputeQuota(FederationUser federationUser) {
		return null;
	}

}
