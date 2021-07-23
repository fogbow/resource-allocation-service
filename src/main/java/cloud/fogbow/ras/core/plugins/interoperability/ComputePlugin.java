package cloud.fogbow.ras.core.plugins.interoperability;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.Order;

public interface ComputePlugin<S extends CloudUser> extends OrderPlugin<ComputeInstance, ComputeOrder, S> {

    /**
     * This method requests the virtual machine creation on a provider.
     * If an instance is successfully allocated, then, the implementation
     * MUST set the order's actual resource allocation, since this can
     * be different from what was originally requested in the order.
     *
     * @param computeOrder        {@link Order} for creating a virtual machine.
     * @param cloudUser
     * @return Instance ID.
     * @throws FogbowException {@link FogbowException} When request fails.
     */
    public String requestInstance(ComputeOrder computeOrder, S cloudUser) throws FogbowException;

    public ComputeInstance getInstance(ComputeOrder computeOrder, S cloudUser) throws FogbowException;

    public void deleteInstance(ComputeOrder computeOrder, S cloudUser) throws FogbowException;

    public void takeSnapshot(ComputeOrder computeOrder, String name, S cloudUser) throws FogbowException;

    public void pauseInstance(ComputeOrder order, S cloudUser) throws FogbowException;

    public void hibernateInstance(ComputeOrder order, S cloudUser) throws FogbowException;

    public void stopInstance(ComputeOrder order, S cloudUser) throws FogbowException;
    
    public void resumeInstance(ComputeOrder order, S cloudUser) throws FogbowException;

    public boolean isPaused(String cloudState) throws FogbowException;

    public boolean isHibernated(String cloudState) throws FogbowException;

    public boolean isStopped(String cloudState) throws FogbowException;
}
