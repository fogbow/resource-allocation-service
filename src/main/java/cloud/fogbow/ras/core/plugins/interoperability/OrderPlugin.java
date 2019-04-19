package cloud.fogbow.ras.core.plugins.interoperability;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.ras.api.http.response.Instance;
import cloud.fogbow.ras.core.models.orders.Order;

public interface OrderPlugin<R extends Instance, T extends Order, S extends CloudUser> {
    public String requestInstance(T Order, S cloudUser) throws FogbowException;

    public void deleteInstance(T Order, S cloudUser) throws FogbowException;

    public R getInstance(T Order, S cloudUser) throws FogbowException;

    public boolean isReady(String instanceState);

    public boolean hasFailed(String instanceState);
}
