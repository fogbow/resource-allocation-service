package cloud.fogbow.ras.core.plugins.interoperability;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;

public interface NetworkPlugin<S extends CloudUser> extends OrderPlugin<NetworkInstance, NetworkOrder, S> {

    public String requestInstance(NetworkOrder networkOrder, S cloudUser) throws FogbowException;

    public NetworkInstance getInstance(NetworkOrder networkOrder, S cloudUser) throws FogbowException;

    public void deleteInstance(NetworkOrder networkOrder, S cloudUser) throws FogbowException;
}
