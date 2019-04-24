package cloud.fogbow.ras.core.stubs;

import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.ras.api.http.response.VolumeInstance;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.VolumePlugin;

/**
 * This class is a stub for the VolumePlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubVolumePlugin implements VolumePlugin<CloudUser> {

    public StubVolumePlugin(String confFilePath) {
    }

    @Override
    public String requestInstance(VolumeOrder volumeOrder, CloudUser cloudUser) {
        return null;
    }

    @Override
    public VolumeInstance getInstance(VolumeOrder volumeOrder, CloudUser cloudUser) {
        return null;
    }

    @Override
    public void deleteInstance(VolumeOrder volumeOrder, CloudUser cloudUser) {
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
