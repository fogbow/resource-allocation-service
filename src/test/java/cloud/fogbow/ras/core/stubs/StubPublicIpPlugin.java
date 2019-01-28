package cloud.fogbow.ras.core.stubs;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.ras.core.models.instances.PublicIpInstance;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.plugins.interoperability.PublicIpPlugin;

/**
 * This class is a stub for the PublicIpPlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubPublicIpPlugin implements PublicIpPlugin {
    public StubPublicIpPlugin(String confFilePath) {
    }

    @Override
    public String requestInstance(PublicIpOrder publicIpOrder, String computeInstanceId, CloudToken token) throws FogbowException {
        return null;
    }

    @Override
    public void deleteInstance(String publicIpInstanceId, String computeInstanceId, CloudToken token) throws FogbowException {

    }

    @Override
    public PublicIpInstance getInstance(String publicIpInstanceId, CloudToken token) throws FogbowException {
        return null;
    }
}
