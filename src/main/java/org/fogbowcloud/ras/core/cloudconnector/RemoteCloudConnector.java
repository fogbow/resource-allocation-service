package org.fogbowcloud.ras.core.cloudconnector;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.intercomponent.xmpp.requesters.*;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.images.Image;
import org.fogbowcloud.ras.core.models.instances.Instance;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.quotas.Quota;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;

import java.util.HashMap;
import java.util.List;

public class RemoteCloudConnector implements CloudConnector {
    private static final Logger LOGGER = Logger.getLogger(RemoteCloudConnector.class);

    private String destinationMember;
    private String cloudName;

    public RemoteCloudConnector(String memberId, String cloudName) {
        this.destinationMember = memberId;
        this.cloudName = cloudName;
    }

    public RemoteCloudConnector(String memberId) {
        this(memberId, "");
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
                this.cloudName, federationUserToken, resourceType);
        Quota quota = remoteGetUserQuotaRequest.send();
        return quota;
    }

    @Override
    public HashMap<String, String> getAllImages(FederationUserToken federationUserToken) throws Exception {

        RemoteGetAllImagesRequest remoteGetAllImagesRequest = new RemoteGetAllImagesRequest(this.destinationMember,
                this.cloudName, federationUserToken);
        HashMap<String, String> imagesMap = remoteGetAllImagesRequest.send();
        return imagesMap;
    }

    @Override
    public Image getImage(String imageId, FederationUserToken federationUserToken) throws Exception {

        RemoteGetImageRequest remoteGetImageRequest = new RemoteGetImageRequest(this.destinationMember, this.cloudName,
                imageId, federationUserToken);
        Image image = remoteGetImageRequest.send();
        return image;
    }

    public List<String> getCloudNames(FederationUserToken federationUserToken) throws Exception {
        RemoteGetCloudNamesRequest remoteGetCloudNames = new RemoteGetCloudNamesRequest(this.destinationMember, federationUserToken);
        List<String> cloudNames = remoteGetCloudNames.send();
        return cloudNames;
    }
}
