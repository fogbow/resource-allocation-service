package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.volume;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.ras.api.http.response.VolumeInstance;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.VolumePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudUtils;

public class EmulatedCloudVolumePlugin implements VolumePlugin<CloudUser> {
    @Override
    public String requestInstance(VolumeOrder volumeOrder, CloudUser cloudUser) throws FogbowException {
        return null;
    }

    @Override
    public VolumeInstance getInstance(VolumeOrder volumeOrder, CloudUser cloudUser) throws FogbowException {
        return null;
    }

    @Override
    public boolean isReady(String instanceState) {
        return true;
    }

    @Override
    public boolean hasFailed(String instanceState) {
        return false;
    }

    @Override
    public void deleteInstance(VolumeOrder volumeOrder, CloudUser cloudUser) throws FogbowException {
        String volumeId = volumeOrder.getId();
        String volumePath = EmulatedCloudUtils.getResourcePath(this.properties, volumeId);

        EmulatedCloudUtils.deleteFile(volumePath);
    }
}
