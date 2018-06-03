package org.fogbowcloud.manager.core.cloudconnector;

import org.fogbowcloud.manager.api.intercomponent.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.plugins.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.plugins.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.instances.Instance;
import org.fogbowcloud.manager.core.models.quotas.Quota;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.fogbowcloud.manager.core.models.quotas.allocation.Allocation;

import java.util.Collection;

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
     * Gets the quota of the federation user.
     * @throws TokenCreationException, UnauthorizedException, PropertyNotSpecifiedException, QuotaException
     */
    public Quota getUserQuota(FederationUser federationUser, InstanceType instanceType) throws
            TokenCreationException, UnauthorizedException, PropertyNotSpecifiedException, QuotaException,
            RemoteRequestException;

    /**
     * Gets the allocation of the federation user.
     * @throws TokenCreationException, UnauthorizedException, PropertyNotSpecifiedException, QuotaException
     */
    public Allocation getUserAllocation(Collection<Order> orders, InstanceType instanceType) throws
            RemoteRequestException, InstanceNotFoundException, RequestException, QuotaException,
            TokenCreationException, PropertyNotSpecifiedException, UnauthorizedException;
}
