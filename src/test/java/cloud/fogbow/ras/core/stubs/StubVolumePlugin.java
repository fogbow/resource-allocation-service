package cloud.fogbow.ras.core.stubs;

import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.ras.core.models.instances.VolumeInstance;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.VolumePlugin;

/**
 * This class is a stub for the VolumePlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubVolumePlugin implements VolumePlugin<CloudToken> {

    public StubVolumePlugin(String confFilePath) {
    }

    @Override
    public String requestInstance(VolumeOrder volumeOrder, CloudToken token) {
        return null;
    }

    @Override
    public VolumeInstance getInstance(String volumeInstanceId, CloudToken token) {
        return null;
    }

    @Override
    public void deleteInstance(String volumeInstanceId, CloudToken token) {
    }
}
