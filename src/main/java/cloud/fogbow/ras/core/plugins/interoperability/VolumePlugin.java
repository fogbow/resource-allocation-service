package cloud.fogbow.ras.core.plugins.interoperability;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.ras.core.models.instances.VolumeInstance;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;

public interface VolumePlugin {

    public String requestInstance(VolumeOrder volumeOrder, CloudToken localUserAttributes) throws FogbowException;

    public VolumeInstance getInstance(String volumeInstanceId, CloudToken localUserAttributes) throws FogbowException;

    public void deleteInstance(String volumeInstanceId, CloudToken localUserAttributes) throws FogbowException;
}
