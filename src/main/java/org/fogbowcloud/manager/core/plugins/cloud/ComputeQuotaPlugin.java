package org.fogbowcloud.manager.core.plugins.cloud;

import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.quotas.ComputeQuota;
import org.fogbowcloud.manager.core.models.tokens.LocalUserAttributes;

public interface ComputeQuotaPlugin {
	
	public ComputeQuota getUserQuota(LocalUserAttributes localUserAttributes) throws FogbowManagerException, UnexpectedException;

}
