package org.fogbowcloud.manager.core.manager.plugins.quota;

import org.fogbowcloud.manager.core.models.quotas.ComputeQuota;

public interface ComputeQuotaPlugin {
	
	ComputeQuota getQuota();
	
}
