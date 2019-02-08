package cloud.fogbow.ras.core.plugins.interoperability;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.ras.core.models.instances.VolumeInstance;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;

public interface VolumePlugin<T extends CloudToken> {

    public String requestInstance(VolumeOrder volumeOrder, T localUserAttributes) throws FogbowException;

    public VolumeInstance getInstance(String volumeInstanceId, T localUserAttributes) throws FogbowException;

    public void deleteInstance(String volumeInstanceId, T localUserAttributes) throws FogbowException;
}
