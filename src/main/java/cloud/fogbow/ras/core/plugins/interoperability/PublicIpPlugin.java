package cloud.fogbow.ras.core.plugins.interoperability;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;

public interface PublicIpPlugin<S extends CloudUser> extends OrderPlugin<PublicIpInstance, PublicIpOrder, S> {

    String requestInstance(PublicIpOrder publicIpOrder, S cloudUser) throws FogbowException;

    void deleteInstance(PublicIpOrder publicIpOrder, S cloudUser) throws FogbowException;

    PublicIpInstance getInstance(PublicIpOrder publicIpOrder, S cloudUser) throws FogbowException;
}
