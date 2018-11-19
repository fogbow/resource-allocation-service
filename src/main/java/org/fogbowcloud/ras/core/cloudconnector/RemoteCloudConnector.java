package org.fogbowcloud.ras.core.cloudconnector;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.intercomponent.xmpp.requesters.*;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.images.Image;
import org.fogbowcloud.ras.core.models.instances.Instance;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.quotas.Quota;
import org.fogbowcloud.ras.core.models.securitygroups.SecurityGroupRule;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;

import java.util.HashMap;
import java.util.List;

public class RemoteCloudConnector implements CloudConnector {
    private static final Logger LOGGER = Logger.getLogger(RemoteCloudConnector.class);

    private String destinationMember;

    public RemoteCloudConnector(String memberId) {
        this.destinationMember = memberId;
    }

    @Override
    public String requestInstance(Order order) throws Exception {
        RemoteCreateOrderRequest remoteCreateOrderRequest = new RemoteCreateOrderRequest(order);
        remoteCreateOrderRequest.send();
        // At the requesting member, the instance Id should be null, since the instance
        // was not created at the requesting member's cloud.
        return null;
    }

    @Override
    public void deleteInstance(Order order) throws Exception {
        try {
            LOGGER.debug("Going to delete order <" + order.getId() + ">");
            RemoteDeleteOrderRequest remoteDeleteOrderRequest = new RemoteDeleteOrderRequest(order);
            LOGGER.debug("Sending request");
            remoteDeleteOrderRequest.send();
        } catch (Exception e) {
            LOGGER.error(e.toString());
            throw e;
        }
    }

    @Override
    public Instance getInstance(Order order) throws Exception {
        RemoteGetOrderRequest remoteGetOrderRequest = new RemoteGetOrderRequest(order);
        Instance instance = remoteGetOrderRequest.send();
        return instance;
    }

    @Override
    public Quota getUserQuota(FederationUserToken federationUserToken, ResourceType resourceType) throws Exception {

        RemoteGetUserQuotaRequest remoteGetUserQuotaRequest = new RemoteGetUserQuotaRequest(this.destinationMember,
                federationUserToken, resourceType);
        Quota quota = remoteGetUserQuotaRequest.send();
        return quota;
    }

    @Override
    public HashMap<String, String> getAllImages(FederationUserToken federationUserToken) throws Exception {

        RemoteGetAllImagesRequest remoteGetAllImagesRequest = new RemoteGetAllImagesRequest(this.destinationMember,
                federationUserToken);
        HashMap<String, String> imagesMap = remoteGetAllImagesRequest.send();
        return imagesMap;
    }

    @Override
    public Image getImage(String imageId, FederationUserToken federationUserToken) throws Exception {

        RemoteGetImageRequest remoteGetImageRequest = new RemoteGetImageRequest(this.destinationMember, imageId,
                federationUserToken);
        Image image = remoteGetImageRequest.send();
        return image;
    }

    @Override
    public List<SecurityGroupRule> getAllSecurityGroupRules(Order majorOrder, FederationUserToken federationUserToken)
            throws Exception {
        RemoteGetAllSecurityRuleRequest remoteGetAllSecurityRuleRequest =
                new RemoteGetAllSecurityRuleRequest(this.destinationMember, majorOrder.getId(), federationUserToken);
        return remoteGetAllSecurityRuleRequest.send();
    }

    @Override
    public String requestSecurityGroupRule(Order majorOrder, SecurityGroupRule securityGroupRule,
                                           FederationUserToken federationUserToken) throws Exception {
        RemoteCreateSecurityRuleRequest remoteCreateSecurityRuleRequest =
                new RemoteCreateSecurityRuleRequest(securityGroupRule, federationUserToken, this.destinationMember, majorOrder);
        remoteCreateSecurityRuleRequest.send();
        return null;
    }

    @Override
    public void deleteSecurityGroupRule(String securityGroupRuleId, FederationUserToken federationUserToken) throws Exception {
        RemoteDeleteSecurityRuleRequest remoteDeleteSecurityRuleRequest =
                new RemoteDeleteSecurityRuleRequest(securityGroupRuleId, this.destinationMember, federationUserToken);
        remoteDeleteSecurityRuleRequest.send();
    }
}
