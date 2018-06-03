package org.fogbowcloud.manager.core.cloudconnector;

import org.fogbowcloud.manager.api.intercomponent.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.plugins.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.plugins.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.instances.Instance;
import org.fogbowcloud.manager.core.models.quotas.ComputeQuota;
import org.fogbowcloud.manager.core.models.token.FederationUser;

public interface CloudConnector {

    /**
     * Requests an Instance for a Order.
     *
     * @return An Instance with at least an nonempty Id.
     * @throws PropertyNotSpecifiedException,
     *             UnauthorizedException, TokenCreationException, RequestException, RemoteConnectionException,
     *             RemoteRequestException, OrderManagementException
     */
    public String requestInstance(Order order) throws PropertyNotSpecifiedException,
            UnauthorizedException, TokenCreationException, RequestException, RemoteConnectionException,
            RemoteRequestException, OrderManagementException;

    /**
     * Signals the cloud that the provided instance is no longer required.
     *
     * @throws RequestException, TokenCreationException, UnauthorizedException, PropertyNotSpecifiedException,
     * RemoteRequestException, OrderManagementException
     */
    public void deleteInstance(Order order)
        throws RequestException, TokenCreationException, UnauthorizedException, PropertyNotSpecifiedException,
            RemoteRequestException, OrderManagementException;

    /**
     * Gets the instance currently associated for the provided order.
     * @throws RequestException, TokenCreationException,
     *         UnauthorizedException, PropertyNotSpecifiedException, InstanceNotFoundException, RemoteRequestException
     */
    public Instance getInstance(Order order) throws RequestException, TokenCreationException,
        UnauthorizedException, PropertyNotSpecifiedException, InstanceNotFoundException, RemoteRequestException;

    /**
     * Gets the quota of the federationidentity user.
     * @throws TokenCreationException, UnauthorizedException, PropertyNotSpecifiedException, QuotaException
     */
    public ComputeQuota getComputeQuota(FederationUser federationUser) throws TokenCreationException,
            UnauthorizedException, PropertyNotSpecifiedException, QuotaException;
}
