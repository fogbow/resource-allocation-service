package cloud.fogbow.ras.core.cloudconnector;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.api.http.response.ImageSummary;
import cloud.fogbow.ras.api.http.response.OrderInstance;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.api.http.response.quotas.Quota;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;

import java.util.List;

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
     * @return the OrderInstance whose instance Id is stored in the order
     * @throws FogbowException
     */
    OrderInstance getInstance(Order order) throws FogbowException;

    /**
     * Gets the quota of the system user for resourceType.
     *
     * @param systemUser the attributes that identify the user
     * @return the quota associated to the user
     * @throws FogbowException
     */
    Quota getUserQuota(SystemUser systemUser) throws FogbowException;

    /**
     * Gets the list of images that the system user can see in the target cloud.
     *
     * @param systemUser the attributes that identify the user
     * @return a list where each element is an ImageSummary containing the image id and the image name
     * @throws FogbowException
     */
    List<ImageSummary> getAllImages(SystemUser systemUser) throws FogbowException;

    /**
     * Gets the information about a given image.
     *
     * @param imageId    the Id of the image to be retrieved
     * @param systemUser the attributes that identify the user
     * @return the requested image
     * @throws FogbowException
     */

    ImageInstance getImage(String imageId, SystemUser systemUser) throws FogbowException;

    /**
     * Gets all security rules associated to an Order (must be either a publicIp or a network order)
     *
     * @param order             the order to which the security rules have been associated
     * @param systemUser        the attributes that identify the user
     * @return a list containing all security rules
     * @throws FogbowException
     */
    List<SecurityRuleInstance> getAllSecurityRules(Order order, SystemUser systemUser) throws FogbowException;

    /**
     * Requests a new security group rule in the cloud (either locally or remotely) using the requirements contained
     * in the security group rule.
     *
     * @param order             the order to which the security rule will be associated
     * @param securityRule      the rule to be added
     * @param systemUser        the attributes that identify the user
     * @return the string that represents the security rule Id
     * @throws FogbowException
     */
    String requestSecurityRule(Order order, SecurityRule securityRule, SystemUser systemUser) throws FogbowException;

    /**
     * Deletes in the cloud the indicated security rule that had been previously associated to either a publicIp or
     * a network order.
     *
     * @param securityRuleId    the Id of the security rule to be deleted
     * @param systemUser        the attributes that identify the user
     * @throws FogbowException
     */
    void deleteSecurityRule(String securityRuleId, SystemUser systemUser) throws FogbowException;
}
