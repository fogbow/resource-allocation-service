package org.fogbowcloud.manager.core.manager.plugins.quota;

import org.fogbowcloud.manager.core.exceptions.QuotaException;
import org.fogbowcloud.manager.core.models.quotas.ComputeQuota;
import org.fogbowcloud.manager.core.models.token.Token;

public interface ComputeQuotaPlugin {
	
	ComputeQuota getQuotaUsed(Token localToken) throws QuotaException;

	ComputeQuota getMaxQuota(Token localToken) throws QuotaException;
	
}
