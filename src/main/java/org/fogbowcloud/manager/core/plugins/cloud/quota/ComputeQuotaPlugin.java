package org.fogbowcloud.manager.core.plugins.cloud.quota;

import org.fogbowcloud.manager.core.exceptions.QuotaException;
import org.fogbowcloud.manager.core.models.quotas.ComputeQuota;
import org.fogbowcloud.manager.core.models.token.Token;

public interface ComputeQuotaPlugin {
	
	public ComputeQuota getUserQuota(Token localToken) throws QuotaException;

}
