package org.fogbowcloud.manager.core.cloudconnector;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.intercomponent.xmpp.requesters.*;
import org.fogbowcloud.manager.core.models.images.Image;
import org.fogbowcloud.manager.core.models.instances.Instance;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.quotas.Quota;
import org.fogbowcloud.manager.core.models.tokens.FederationUserAttributes;

import java.util.HashMap;

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
        RemoteDeleteOrderRequest remoteDeleteOrderRequest = new RemoteDeleteOrderRequest(order);
		remoteDeleteOrderRequest.send();
    }

    @Override
    public Instance getInstance(Order order) throws Exception {
        RemoteGetOrderRequest remoteGetOrderRequest = new RemoteGetOrderRequest(order);
        Instance instance = remoteGetOrderRequest.send();
        return instance;
    }

    @Override
    public Quota getUserQuota(FederationUserAttributes federationUserAttributes, ResourceType resourceType) throws Exception {

        RemoteGetUserQuotaRequest remoteGetUserQuotaRequest = new RemoteGetUserQuotaRequest(this.destinationMember,
                federationUserAttributes, resourceType);
        Quota quota = remoteGetUserQuotaRequest.send();
        return quota;
    }

    @Override
    public HashMap<String, String> getAllImages(FederationUserAttributes federationUserAttributes) throws Exception {

        RemoteGetAllImagesRequest remoteGetAllImagesRequest = new RemoteGetAllImagesRequest(this.destinationMember,
                federationUserAttributes);
        HashMap<String, String> imagesMap = remoteGetAllImagesRequest.send();
        return imagesMap;
    }

    @Override
    public Image getImage(String imageId, FederationUserAttributes federationUserAttributes) throws Exception {

        RemoteGetImageRequest remoteGetImageRequest = new RemoteGetImageRequest(this.destinationMember, imageId,
                federationUserAttributes);
        Image image = remoteGetImageRequest.send();
        return image;    }
}
