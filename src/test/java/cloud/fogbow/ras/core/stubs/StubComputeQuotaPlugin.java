package cloud.fogbow.ras.core.stubs;

import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.ras.api.http.response.quotas.ComputeQuota;
import cloud.fogbow.ras.core.plugins.interoperability.ComputeQuotaPlugin;

/**
 * This class is a stub for the ComputeQuotaPlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubComputeQuotaPlugin implements ComputeQuotaPlugin<CloudUser> {

    public StubComputeQuotaPlugin(String confFilePath) {
    }

    @Override
    public ComputeQuota getUserQuota(CloudUser cloudUser) {
        return null;
    }
}
