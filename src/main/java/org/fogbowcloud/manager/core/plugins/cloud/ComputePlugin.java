package org.fogbowcloud.manager.core.plugins.cloud;

import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.token.Token;

public interface ComputePlugin {

    /**
     * This method requests the virtual machine creation on a provider.
     * If an instance is successfully allocated, then, the implementation
     * MUST set the order's actual resource allocation, since this can
     * be different from what was originally requested in the order.
     *
     * @param computeOrder {@link Order} for creating a virtual machine.
     * @param localToken
     * @return Instance ID.
     * @throws RequestException {@link RequestException} When request fails.
     */
    public String requestInstance(ComputeOrder computeOrder, Token localToken)
            throws RequestException;

    public ComputeInstance getInstance(String computeInstanceId, Token localToken)
            throws RequestException;

    public void deleteInstance(String computeInstanceId, Token localToken) 
    		throws RequestException;

}
