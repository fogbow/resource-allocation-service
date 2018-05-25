package org.fogbowcloud.manager.core.instanceprovider;

import org.fogbowcloud.manager.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.instances.Instance;

public interface InstanceProvider {

    /**
     * Requests an Instance for a Order.
     *
     * @return An Instance with at least an nonempty Id.
     * @throws Exception If the Instance creation fail
     */
    public String requestInstance(Order order) throws PropertyNotSpecifiedException,
        UnauthorizedException, TokenCreationException, RequestException;

    /**
     * Signals the cloud that the provided instance is no longer required.
     *
     * @throws Exception if a failure occurred when requesting the deletion of an instance
     */
    public void deleteInstance(Order order)
        throws RequestException, TokenCreationException, UnauthorizedException, PropertyNotSpecifiedException;

    /**
     * Gets the instance currently associated for the provided order.
     */
    public Instance getInstance(Order order) throws RequestException, TokenCreationException,
        UnauthorizedException, PropertyNotSpecifiedException, InstanceNotFoundException;
}
