package cloud.fogbow.ras.core.stubs;

import cloud.fogbow.ras.core.models.instances.ComputeInstance;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import org.fogbowcloud.ras.core.models.tokens.Token;
import cloud.fogbow.ras.core.plugins.interoperability.ComputePlugin;

/**
 * This class is a stub for the ComputePlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubComputePlugin implements ComputePlugin<Token> {

    public StubComputePlugin(String confFilePath) {
    }

    @Override
    public String requestInstance(ComputeOrder computeOrder, Token token) {
        return null;
    }

    @Override
    public ComputeInstance getInstance(String computeInstanceId, Token token) {
        return null;
    }

    @Override
    public void deleteInstance(String computeInstanceId, Token token) {
    }
}
