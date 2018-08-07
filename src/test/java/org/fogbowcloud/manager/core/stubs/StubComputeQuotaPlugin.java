package org.fogbowcloud.manager.core.stubs;

import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.quotas.ComputeQuota;
import org.fogbowcloud.manager.core.models.tokens.LocalUserAttributes;
import org.fogbowcloud.manager.core.plugins.cloud.ComputeQuotaPlugin;

/**
 * This class is a stub for the ComputeQuotaPlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubComputeQuotaPlugin implements ComputeQuotaPlugin {

    public StubComputeQuotaPlugin() {}
    
    @Override
    public ComputeQuota getUserQuota(LocalUserAttributes localUserAttributes)
            throws FogbowManagerException, UnexpectedException {
        return null;
    }

}
