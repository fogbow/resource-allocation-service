package cloud.fogbow.ras.core.cloudconnector;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.util.connectivity.FogbowGenericResponse;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.api.http.response.Image;
import cloud.fogbow.ras.api.http.response.Instance;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.api.http.response.quotas.Quota;
import cloud.fogbow.ras.api.http.response.securityrules.SecurityRule;

import java.util.List;
import java.util.Map;

public interface CloudConnector {
    /**
     * Requests an instance in the cloud (either locally or remotely) using the requirements contained in order.
     *
     * @param order the order with the spec of the instance to be requested
     * @return the string that represents the instance Id
     * @throws FogbowException
     */
    String requestInstance(Order order) throws FogbowException;

    /**
     * Deletes in the cloud the instance associated to an order.
     *
     * @param order the order to be deleted
     * @throws FogbowException
     */
    void deleteInstance(Order order) throws FogbowException;

    /**
     * Gets from the cloud the instance currently associated to the order.
     *
     * @param order the order whose associated instance is requested
     * @return the Instance whose instance Id is stored in the order
     * @throws FogbowException
     */
    Instance getInstance(Order order) throws FogbowException;

    /**
     * Gets the quota of the federation user for resourceType.
     *
     * @param systemUserToken the attributes of the user
     * @param resourceType    the type of instance for which the quota was requested
     * @return the quota associated to the user
     * @throws FogbowException
     */
    Quota getUserQuota(SystemUser systemUserToken, ResourceType resourceType) throws FogbowException;

    /**
     * Gets the list of images that the federation user can see in the target cloud.
     *
     * @param systemUser the attributes of the user
     * @return a getCloudUser where each element is a pair (image name, image id)
     * @throws FogbowException
     */
    Map<String, String> getAllImages(SystemUser systemUser) throws FogbowException;

    /**
     * Gets the information about a given image.
     *
     * @param imageId    the Id of the image to be retrieved
     * @param systemUser the attributes of the user
     * @return the requested image
     * @throws FogbowException
     */

    Image getImage(String imageId, SystemUser systemUser) throws FogbowException;

    /**
     * Redirects a generic request to the cloud then answer the response.
     *
     * @param genericRequest
     * @param systemUser
     * @return the response received from the cloud
     * @throws FogbowException
     */
    FogbowGenericResponse genericRequest(String genericRequest, SystemUser systemUser) throws FogbowException;

    /**
     * Gets all security rules associated to an Order (must be either a publicIp or a network order)
     *
     * @param order             the order to which the security rules have been associated
     * @param systemUser        the attributes of the user
     * @return a list containing all security rules
     * @throws FogbowException
     */
    List<SecurityRule> getAllSecurityRules(Order order, SystemUser systemUser) throws FogbowException;

    /**
     * Requests a new security group rule in the cloud (either locally or remotely) using the requirements contained
     * security group rule.
     *
     * @param order             the order to which the security rule will be associated
     * @param securityRule      the rule to be added
     * @param systemUser        the attributes of the user
     * @return the string that represents the security rule Id
     * @throws FogbowException
     */
    String requestSecurityRule(Order order, SecurityRule securityRule, SystemUser systemUser) throws FogbowException;

    /**
     * Deletes in the cloud the indicated security rule that had been associated to either a publicIp or
     * a network.
     *
     * @param securityRuleId    the Id of the security rule to be deleted
     * @param systemUser        the attributes of the user
     * @throws FogbowException
     */
    void deleteSecurityRule(String securityRuleId, SystemUser systemUser) throws FogbowException;
}
