package cloud.fogbow.ras.core.stubs;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.plugins.interoperability.PublicIpPlugin;

/**
 * This class is a stub for the PublicIpPlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubPublicIpPlugin implements PublicIpPlugin<CloudUser> {
    public StubPublicIpPlugin(String confFilePath) {
    }

    @Override
    public String requestInstance(PublicIpOrder publicIpOrder, CloudUser cloudUser) throws FogbowException {
        return null;
    }

    @Override
    public void deleteInstance(String publicIpInstanceId, CloudUser cloudUser) throws FogbowException {
    }

    @Override
    public PublicIpInstance getInstance(String publicIpInstanceId, CloudUser cloudUser) throws FogbowException {
        return null;
    }

    @Override
    public boolean isReady(String cloudState) {
        return true;
    }

    @Override
    public boolean hasFailed(String cloudState) {
        return false;
    }
}
