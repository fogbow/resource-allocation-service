package org.fogbowcloud.manager.core.cloudconnector;

import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.models.images.Image;
import org.fogbowcloud.manager.core.models.instances.Instance;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.quotas.Quota;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;

import java.util.Map;

public interface CloudConnector {

    /**
     * Requests an instance in the cloud (either locally or remotely) using the requirements contained in order.
     *
     * @return the string that represents the instance Id
     * @param order the order
     * @throws FogbowManagerException
     */
    String requestInstance(Order order) throws Exception;

    /**
     * Deletes in the cloud the instance associated to an order.
     *
     * @param order the order
     * @throws FogbowManagerException
     */
     void deleteInstance(Order order) throws Exception;

    /**
     * Gets from the cloud the instance currently associated to order.
     *
     * @return the Instance whose instance Id is stored in the order
     * @param order the order
     * @throws FogbowManagerException
     */
     Instance getInstance(Order order) throws Exception;

    /**
     * Gets the quota of the federation user for instanceType.
     *
     * @return the quota associated to the user
     * @param federationUser the attributes of the federation user
     * @param instanceType the type of instance for which the quota was requested
     * @throws FogbowManagerException
     */
     Quota getUserQuota(FederationUser federationUser, InstanceType instanceType) throws Exception;

    /**
     * Gets the list of images that the federation user can see in the target cloud.
     *
     * @param federationUser
     * @return a map where each element is a pair (image name, image id)
     * @throws FogbowManagerException
     */
     Map<String, String> getAllImages(FederationUser federationUser) throws Exception;

    /**
     * Gets the information about a given image.
     *
     * @return the requested image
     * @param imageId the Id of the image to be retrieved
     * @param federationUser the attributes of the federation user
     * @throws FogbowManagerException
     */
     Image getImage(String imageId, FederationUser federationUser) throws Exception;
}
