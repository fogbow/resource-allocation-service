package org.fogbowcloud.ras.core.cloudconnector;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.images.Image;
import org.fogbowcloud.ras.core.models.instances.Instance;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.quotas.Quota;
import org.fogbowcloud.ras.core.models.securitygroups.SecurityGroupRule;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;

import java.util.List;
import java.util.Map;

public interface CloudConnector {
    /**
     * Requests an instance in the cloud (either locally or remotely) using the requirements contained in order.
     *
     * @param order the order with the spec of the instance to be requested
     * @return the string that represents the instance Id
     * @throws Exception
     */
    String requestInstance(Order order) throws Exception;

    /**
     * Deletes in the cloud the instance associated to an order.
     *
     * @param order the order to be deleted
     * @throws Exception
     */
    void deleteInstance(Order order) throws Exception;

    /**
     * Gets from the cloud the instance currently associated to order.
     *
     * @param order the order whose associated instance is requested
     * @return the Instance whose instance Id is stored in the order
     * @throws Exception
     */
    Instance getInstance(Order order) throws Exception;

    /**
     * Gets the quota of the federation user for resourceType.
     *
     * @param federationUserToken the attributes of the federation user
     * @param resourceType        the type of instance for which the quota was requested
     * @return the quota associated to the user
     * @throws Exception
     */
    Quota getUserQuota(FederationUserToken federationUserToken, ResourceType resourceType) throws Exception;

    /**
     * Gets the list of images that the federation user can see in the target cloud.
     *
     * @param federationUserToken the attributes of the federation user
     * @return a map where each element is a pair (image name, image id)
     * @throws Exception
     */
    Map<String, String> getAllImages(FederationUserToken federationUserToken) throws Exception;

    /**
     * Gets the information about a given image.
     *
     * @param imageId             the Id of the image to be retrieved
     * @param federationUserToken the attributes of the federation user
     * @return the requested image
     * @throws Exception
     */
    Image getImage(String imageId, FederationUserToken federationUserToken) throws Exception;

    /**
     * Gets all security group rules from a specific orderId (must be a publicIp or a network)
     *
     * @param securityGroupName   the security group name to retrieve all rules
     * @param federationUserToken the attributes of the federation user
     * @return a list containing all security group rules
     */
    List<SecurityGroupRule> getAllSecurityGroupRules(String securityGroupName, FederationUserToken federationUserToken) throws UnexpectedException, FogbowRasException;

    /**
     * Requests a new security group rule in the cloud (either locally or remotely) using the requirements contained
     * security group rule.
     *
     * @param securityGroupName   the security group name to be added a new rule
     * @param securityGroupRule   the rule to be added
     * @param federationUserToken the attributes of the federation user
     * @return the string that represents the security group rule Id
     */
    String requestSecurityGroupRule(String securityGroupName, SecurityGroupRule securityGroupRule,
            FederationUserToken federationUserToken) throws UnexpectedException, FogbowRasException;

    /**
     * Deletes in the cloud the security group rule associated to a security group.
     *
     * @param securityGroupRuleId the Id of the security group rule to be retrieved
     * @param federationUserToken the attributes of the federation user
     */
    void deleteSecurityGroupRule(String securityGroupRuleId, FederationUserToken federationUserToken) throws UnexpectedException, FogbowRasException;
}
