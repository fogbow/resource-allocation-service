package org.fogbowcloud.manager.core.cloudconnector;

import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.models.images.Image;
import org.fogbowcloud.manager.core.models.instances.Instance;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.quotas.Quota;
import org.fogbowcloud.manager.core.models.token.FederationUser;

import java.util.Map;

public interface CloudConnector {

    /**
     * Requests an Instance for a Order.
     *
     * @return An Instance with at least an nonempty Id.
     * @throws FogbowManagerException
     */
    String requestInstance(Order order) throws FogbowManagerException;
    /**
     * Signals the cloud that the provided instance is no longer required.
     *
     * @throws FogbowManagerException
     */
     void deleteInstance(Order order) throws FogbowManagerException;

    /**
     * Gets the instance currently associated for the provided order.
     * @throws FogbowManagerException;
     */
     Instance getInstance(Order order) throws FogbowManagerException;

    /**
     * Gets the quota of the federation user.
     * @throws FogbowManagerException
     */
     Quota getUserQuota(FederationUser federationUser, InstanceType instanceType) throws FogbowManagerException;

    /**
     * Gets the list of images that the federation user can see in the target cloud.
     * @param federationUser
     * @return a map where each element is a pair (image name, image id).
     * @throws FogbowManagerException
     */
     Map<String, String> getAllImages(FederationUser federationUser) throws FogbowManagerException;

    /**
     *
     * Gets the information about a given image.
     * @param imageId
     * @param federationUser
     * @return
     * @throws FogbowManagerException
     */
     Image getImage(String imageId, FederationUser federationUser) throws FogbowManagerException;
}
