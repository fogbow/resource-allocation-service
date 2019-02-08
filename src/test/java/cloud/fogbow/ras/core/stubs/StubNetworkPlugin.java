package cloud.fogbow.ras.core.stubs;

import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.ras.core.models.instances.NetworkInstance;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.plugins.interoperability.NetworkPlugin;

/**
 * This class is a stub for the NetworkPlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubNetworkPlugin implements NetworkPlugin<CloudToken> {

    public StubNetworkPlugin(String confFilePath) {
    }

    @Override
    public String requestInstance(NetworkOrder networkOrder, CloudToken token) {
        return null;
    }

    @Override
    public NetworkInstance getInstance(String networkInstanceId, CloudToken token) {
        return null;
    }

    @Override
    public void deleteInstance(String networkInstanceId, CloudToken token) {
    }
}
