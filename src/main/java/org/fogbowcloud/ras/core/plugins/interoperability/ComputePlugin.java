package org.fogbowcloud.ras.core.plugins.interoperability;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.instances.ComputeInstance;
import org.fogbowcloud.ras.core.models.orders.ComputeOrder;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.tokens.Token;

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
     * @throws FogbowRasException {@link FogbowRasException} When request fails.
     */
    public String requestInstance(ComputeOrder computeOrder, T localUserAttributes)
            throws FogbowRasException, UnexpectedException;

    public ComputeInstance getInstance(String computeInstanceId, T localUserAttributes)
            throws FogbowRasException, UnexpectedException;

    public void deleteInstance(String computeInstanceId, T localUserAttributes)
            throws FogbowRasException, UnexpectedException;

}
