package org.fogbowcloud.manager.core.plugins.cloud;

import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.models.quotas.ComputeQuota;
import org.fogbowcloud.manager.core.models.token.Token;

public interface ComputeQuotaPlugin {
	
	public ComputeQuota getUserQuota(Token localToken) throws FogbowManagerException;

}
