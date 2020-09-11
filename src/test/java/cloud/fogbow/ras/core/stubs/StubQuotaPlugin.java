package cloud.fogbow.ras.core.stubs;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.ras.api.http.response.quotas.ResourceQuota;
import cloud.fogbow.ras.core.plugins.interoperability.QuotaPlugin;

/**
 * This class is a stub for the QuotaPlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubQuotaPlugin implements QuotaPlugin<CloudUser> {

    public StubQuotaPlugin(String confFilePath) {
    }

    @Override
    public ResourceQuota getUserQuota(CloudUser cloudUser) throws FogbowException {
        return null;
    }
}
