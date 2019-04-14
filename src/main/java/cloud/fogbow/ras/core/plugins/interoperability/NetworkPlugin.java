package cloud.fogbow.ras.core.plugins.interoperability;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;

public interface NetworkPlugin<S extends CloudUser> extends OrderPlugin<NetworkInstance, NetworkOrder, S> {

    public static final String SECURITY_GROUP_PREFIX = "ras-sg-pn-";

    public String requestInstance(NetworkOrder networkOrder, S cloudUser) throws FogbowException;

    public NetworkInstance getInstance(String networkInstanceId, S cloudUser) throws FogbowException;

    public void deleteInstance(String networkInstanceId, S cloudUser) throws FogbowException;
}
