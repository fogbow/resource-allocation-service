package cloud.fogbow.ras.core.stubs;

import cloud.fogbow.ras.core.models.instances.PublicIpInstance;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import org.fogbowcloud.ras.core.models.tokens.Token;
import cloud.fogbow.ras.core.plugins.interoperability.PublicIpPlugin;

/**
 * This class is a stub for the PublicIpPlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubPublicIpPlugin implements PublicIpPlugin {
    public StubPublicIpPlugin(String confFilePath) {
    }

    @Override
    public String requestInstance(PublicIpOrder publicIpOrder, String computeInstanceId, Token token) throws FogbowRasException, UnexpectedException {
        return null;
    }

    @Override
    public void deleteInstance(String publicIpInstanceId, String computeInstanceId, Token token) throws FogbowRasException, UnexpectedException {

    }

    @Override
    public PublicIpInstance getInstance(String publicIpInstanceId, Token token) throws FogbowRasException, UnexpectedException {
        return null;
    }
}
