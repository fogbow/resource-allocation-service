package org.fogbowcloud.manager.core;

import org.fogbowcloud.manager.core.statisticsprovider.LocalQuotaProvider;
import org.fogbowcloud.manager.core.statisticsprovider.QuotaProvider;
import org.fogbowcloud.manager.core.statisticsprovider.RemoteQuotaProvider;

public class QuotaProviderController {
	
	private QuotaProvider localQuotaProvider;
	private String localMemberId;
	
	public QuotaProviderController (
			LocalQuotaProvider localQuotaProvider,
			String localMemberId
			){
		this.localQuotaProvider = localQuotaProvider;
		this.localMemberId = localMemberId;
	}
	
	public QuotaProvider getQuotaProvider(String memberId) {
		if (this.localMemberId.equals(memberId)) {
			return this.localQuotaProvider;
		} else {
			return new RemoteQuotaProvider(memberId);
		}
	}
	
}
