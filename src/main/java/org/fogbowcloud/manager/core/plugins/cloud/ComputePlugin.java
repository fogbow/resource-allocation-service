package org.fogbowcloud.manager.core.plugins.cloud;

import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.tokens.Token;

public interface ComputePlugin<T extends Token> {

    /**
     * This method requests the virtual machine creation on a provider.
     * If an instance is successfully allocated, then, the implementation
     * MUST set the order's actual resource allocation, since this can
     * be different from what was originally requested in the order.
     *
     * @param computeOrder        {@link Order} for creating a virtual machine.
     * @param localUserAttributes
     * @return Instance ID.
     * @throws FogbowManagerException {@link FogbowManagerException} When request fails.
     */
    public String requestInstance(ComputeOrder computeOrder, T localUserAttributes)
            throws FogbowManagerException, UnexpectedException;

    public ComputeInstance getInstance(String computeInstanceId, T localUserAttributes)
            throws FogbowManagerException, UnexpectedException;

    public void deleteInstance(String computeInstanceId, T localUserAttributes)
            throws FogbowManagerException, UnexpectedException;

}
