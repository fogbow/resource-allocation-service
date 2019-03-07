package cloud.fogbow.ras.core.plugins.interoperability;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.ras.api.http.response.VolumeInstance;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;

public interface VolumePlugin<T extends CloudUser> {

    public String requestInstance(VolumeOrder volumeOrder, T cloudUser) throws FogbowException;

    public VolumeInstance getInstance(String volumeInstanceId, T cloudUser) throws FogbowException;

    public void deleteInstance(String volumeInstanceId, T cloudUser) throws FogbowException;
}
