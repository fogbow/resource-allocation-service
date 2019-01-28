package cloud.fogbow.ras.core.plugins.interoperability;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.ras.core.models.instances.ComputeInstance;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.Order;

public interface ComputePlugin<T extends CloudToken> {

    /**
     * This method requests the virtual machine creation on a provider.
     * If an instance is successfully allocated, then, the implementation
     * MUST set the order's actual resource allocation, since this can
     * be different from what was originally requested in the order.
     *
     * @param computeOrder        {@link Order} for creating a virtual machine.
     * @param localUserAttributes
     * @return Instance ID.
     * @throws FogbowException {@link FogbowException} When request fails.
     */
    public String requestInstance(ComputeOrder computeOrder, T localUserAttributes) throws FogbowException;

    public ComputeInstance getInstance(String computeInstanceId, T localUserAttributes) throws FogbowException;

    public void deleteInstance(String computeInstanceId, T localUserAttributes) throws FogbowException;

}
