package cloud.fogbow.ras.core.plugins.interoperability;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.ras.api.http.response.VolumeInstance;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;

public interface VolumePlugin<S extends CloudUser> extends OrderPlugin<VolumeInstance, VolumeOrder, S> {

    public String requestInstance(VolumeOrder volumeOrder, S cloudUser) throws FogbowException;

    public VolumeInstance getInstance(String volumeInstanceId, S cloudUser) throws FogbowException;

    public void deleteInstance(String volumeInstanceId, S cloudUser) throws FogbowException;
}
