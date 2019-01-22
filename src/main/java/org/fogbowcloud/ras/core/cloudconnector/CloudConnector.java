package org.fogbowcloud.ras.core.cloudconnector;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.images.Image;
import org.fogbowcloud.ras.core.models.instances.Instance;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.quotas.Quota;
import org.fogbowcloud.ras.core.models.securityrules.SecurityRule;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.plugins.interoperability.genericrequest.GenericRequest;
import org.fogbowcloud.ras.core.plugins.interoperability.genericrequest.GenericRequestResponse;

import java.util.List;
import java.util.Map;

public interface CloudConnector {
    /**
     * Requests an instance in the cloud (either locally or remotely) using the requirements contained in order.
     *
     * @param order the order with the spec of the instance to be requested
     * @return the string that represents the instance Id
     * @throws FogbowRasException
     * @throws UnexpectedException
     */
    String requestInstance(Order order) throws FogbowRasException, UnexpectedException;

    /**
     * Deletes in the cloud the instance associated to an order.
     *
     * @param order the order to be deleted
     * @throws FogbowRasException
     * @throws UnexpectedException
     */
    void deleteInstance(Order order) throws FogbowRasException, UnexpectedException;

    /**
     * Gets from the cloud the instance currently associated to the order.
     *
     * @param order the order whose associated instance is requested
     * @return the Instance whose instance Id is stored in the order
     * @throws FogbowRasException
     * @throws UnexpectedException
     */
    Instance getInstance(Order order) throws FogbowRasException, UnexpectedException;

    /**
     * Gets the quota of the federation user for resourceType.
     *
     * @param federationUserToken the attributes of the federation user
     * @param resourceType        the type of instance for which the quota was requested
     * @return the quota associated to the user
     * @throws FogbowRasException
     * @throws UnexpectedException
     */
    Quota getUserQuota(FederationUserToken federationUserToken, ResourceType resourceType) throws FogbowRasException,
            UnexpectedException;

    /**
     * Gets the list of images that the federation user can see in the target cloud.
     *
     * @param federationUserToken the attributes of the federation user
     * @return a map where each element is a pair (image name, image id)
     * @throws FogbowRasException
     * @throws UnexpectedException
     */
    Map<String, String> getAllImages(FederationUserToken federationUserToken) throws FogbowRasException,
            UnexpectedException;

    /**
     * Gets the information about a given image.
     *
     * @param imageId             the Id of the image to be retrieved
     * @param federationUserToken the attributes of the federation user
     * @return the requested image
     * @throws FogbowRasException
     * @throws UnexpectedException
     */
    Image getImage(String imageId, FederationUserToken federationUserToken) throws FogbowRasException,
            UnexpectedException;

    /**
     * Redirects a generic request to the cloud then answer the response.
     *
     * @param genericRequest
     * @param federationUserToken
     * @return the response received from the cloud
     * @throws FogbowRasException
     * @throws UnexpectedException
     */
    GenericRequestResponse genericRequest(GenericRequest genericRequest, FederationUserToken federationUserToken)
            throws FogbowRasException, UnexpectedException;

    /**
     * Gets all security rules associated to an Order (must be either a publicIp or a network order)
     *
     * @param order                 the order to which the security rules have been associated
     * @param federationUserToken   the attributes of the federation user
     * @return a list containing all security rules
     * @throws FogbowRasException
     * @throws UnexpectedException
     */
    List<SecurityRule> getAllSecurityRules(Order order, FederationUserToken federationUserToken)
            throws FogbowRasException, UnexpectedException;

    /**
     * Requests a new security group rule in the cloud (either locally or remotely) using the requirements contained
     * security group rule.
     *
     * @param order                 the order to which the security rule will be associated
     * @param securityRule          the rule to be added
     * @param federationUserToken   the attributes of the federation user
     * @return the string that represents the security rule Id
     * @throws FogbowRasException
     * @throws UnexpectedException
     */
    String requestSecurityRule(Order order, SecurityRule securityRule, FederationUserToken federationUserToken)
            throws FogbowRasException, UnexpectedException;

    /**
     * Deletes in the cloud the indicated security rule that had been associated to either a publicIp or
     * a network.
     *
     * @param securityRuleId        the Id of the security rule to be deleted
     * @param federationUserToken   the attributes of the federation user
     * @throws FogbowRasException
     * @throws UnexpectedException
     */
    void deleteSecurityRule(String securityRuleId, FederationUserToken federationUserToken)
            throws FogbowRasException, UnexpectedException;
}
