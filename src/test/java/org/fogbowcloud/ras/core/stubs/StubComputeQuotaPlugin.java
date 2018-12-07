package org.fogbowcloud.ras.core.stubs;

import org.fogbowcloud.ras.core.models.quotas.ComputeQuota;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.ComputeQuotaPlugin;

/**
 * This class is a stub for the ComputeQuotaPlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubComputeQuotaPlugin implements ComputeQuotaPlugin {

    public StubComputeQuotaPlugin(String confFilePath) {
    }

    @Override
    public ComputeQuota getUserQuota(Token token) {
        return null;
    }
}
