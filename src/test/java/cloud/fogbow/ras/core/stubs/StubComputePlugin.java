package cloud.fogbow.ras.core.stubs;

import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.ComputePlugin;

/**
 * This class is a stub for the ComputePlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubComputePlugin implements ComputePlugin<CloudUser> {

    public StubComputePlugin(String confFilePath) {
    }

    @Override
    public String requestInstance(ComputeOrder computeOrder, CloudUser cloudUser) {
        return null;
    }

    @Override
    public ComputeInstance getInstance(ComputeOrder computeOrder, CloudUser cloudUser) {
        return null;
    }

    @Override
    public void deleteInstance(ComputeOrder computeOrder, CloudUser cloudUser) {
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
