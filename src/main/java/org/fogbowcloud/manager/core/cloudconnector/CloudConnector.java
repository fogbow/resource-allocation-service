package org.fogbowcloud.manager.core.cloudconnector;

import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.intercomponent.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.core.models.images.Image;
import org.fogbowcloud.manager.core.models.instances.Instance;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.quotas.Quota;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.fogbowcloud.manager.core.plugins.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.plugins.exceptions.UnauthorizedException;

import java.util.Map;

public interface CloudConnector {

    /**
     * Requests an Instance for a Order.
     *
     * @return An Instance with at least an nonempty Id.
     * @throws PropertyNotSpecifiedException,
     *             UnauthorizedException, TokenCreationException, RequestException, RemoteConnectionException,
     *             RemoteRequestException, OrderManagementException
     */
    String requestInstance(Order order) throws PropertyNotSpecifiedException,
            UnauthorizedException, TokenCreationException, RequestException, RemoteConnectionException,
            RemoteRequestException, OrderManagementException;

    /**
     * Signals the cloud that the provided instance is no longer required.
     *
     * @throws RequestException, TokenCreationException, UnauthorizedException, PropertyNotSpecifiedException,
     * RemoteRequestException, OrderManagementException
     */
     void deleteInstance(Order order)
        throws RequestException, TokenCreationException, UnauthorizedException, PropertyNotSpecifiedException,
            RemoteRequestException, OrderManagementException;

    /**
     * Gets the instance currently associated for the provided order.
     * @throws RequestException, TokenCreationException,
     *         UnauthorizedException, PropertyNotSpecifiedException, InstanceNotFoundException, RemoteRequestException
     */
     Instance getInstance(Order order) throws RequestException, TokenCreationException,
        UnauthorizedException, PropertyNotSpecifiedException, InstanceNotFoundException, RemoteRequestException;

    /**
     * Gets the quota of the federation user.
     * @throws TokenCreationException, UnauthorizedException, PropertyNotSpecifiedException, QuotaException
     */
     Quota getUserQuota(FederationUser federationUser, InstanceType instanceType) throws
            TokenCreationException, UnauthorizedException, PropertyNotSpecifiedException, QuotaException,
            RemoteRequestException;

    /**
     * Gets the list of images that the federation user can see in the target cloud.
     * @param federationUser
     * @return a map where each element is a pair (image name, image id).
     * @throws ImageException 
     */
     Map<String, String> getAllImages(FederationUser federationUser) throws TokenCreationException,
            UnauthorizedException, PropertyNotSpecifiedException, RemoteRequestException, ImageException;

    /**
     *
     * Gets the information about a given image.
     * @param imageId
     * @param federationUser
     * @return
     * @throws ImageException 
     */
     Image getImage(String imageId, FederationUser federationUser) throws TokenCreationException,
            UnauthorizedException, PropertyNotSpecifiedException, RemoteRequestException, ImageException;
}
