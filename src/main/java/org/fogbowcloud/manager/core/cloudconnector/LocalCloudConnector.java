package org.fogbowcloud.manager.core.cloudconnector;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.CloudPluginsHolder;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.images.Image;
import org.fogbowcloud.manager.core.models.instances.*;
import org.fogbowcloud.manager.core.models.orders.*;
import org.fogbowcloud.manager.core.models.quotas.Quota;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.core.plugins.behavior.mapper.FederationToLocalMapperPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.AttachmentPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.ComputePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.ImagePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.NetworkPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.ComputeQuotaPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.VolumePlugin;

import java.util.Map;

public class LocalCloudConnector implements CloudConnector {

    private final FederationToLocalMapperPlugin mapperPlugin;
    private final AttachmentPlugin attachmentPlugin;
    private final ComputePlugin computePlugin;
    private final ComputeQuotaPlugin computeQuotaPlugin;
    private final NetworkPlugin networkPlugin;
    private final VolumePlugin volumePlugin;
    private final ImagePlugin imagePlugin;

    private static final Logger LOGGER = Logger.getLogger(LocalCloudConnector.class);

    public LocalCloudConnector(FederationToLocalMapperPlugin mapperPlugin, CloudPluginsHolder cloudPluginsHolder) {
        this.mapperPlugin = mapperPlugin;
        this.attachmentPlugin = cloudPluginsHolder.getAttachmentPlugin();
        this.computePlugin = cloudPluginsHolder.getComputePlugin();
        this.computeQuotaPlugin = cloudPluginsHolder.getComputeQuotaPlugin();
        this.networkPlugin = cloudPluginsHolder.getNetworkPlugin();
        this.volumePlugin = cloudPluginsHolder.getVolumePlugin();
        this.imagePlugin = cloudPluginsHolder.getImagePlugin();
    }

    @Override
    public String requestInstance(Order order) throws FogbowManagerException, UnexpectedException {
        String requestInstance = null;
        Token localToken = this.mapperPlugin.getToken(order.getFederationUser());
        switch (order.getType()) {
            case COMPUTE:
                ComputeOrder computeOrder = (ComputeOrder) order;
                requestInstance = this.computePlugin.requestInstance(computeOrder, localToken);
                break;
            case NETWORK:
                NetworkOrder networkOrder = (NetworkOrder) order;
                requestInstance = this.networkPlugin.requestInstance(networkOrder, localToken);
                break;
            case VOLUME:
                VolumeOrder volumeOrder = (VolumeOrder) order;
                requestInstance = this.volumePlugin.requestInstance(volumeOrder, localToken);
                break;
            case ATTACHMENT:
                AttachmentOrder attachmentOrder = (AttachmentOrder) order;
                requestInstance = this.attachmentPlugin.requestInstance(attachmentOrder, localToken);
                break;
            default:
                String message = "No requestInstance plugin implemented for order " + order.getType();
                throw new UnexpectedException(message);
        }
        if (requestInstance == null) {
            String message = "Plugin returned a null value for the instanceId.";
            throw new UnexpectedException(message);
        }
        return requestInstance;
    }

    @Override
    public void deleteInstance(Order order) throws FogbowManagerException, UnexpectedException {
        try {
            if (order.getInstanceId() != null) {
                Token localToken = this.mapperPlugin.getToken(order.getFederationUser());
                switch (order.getType()) {
                    case COMPUTE:
                        this.computePlugin.deleteInstance(order.getInstanceId(), localToken);
                        break;
                    case VOLUME:
                        this.volumePlugin.deleteInstance(order.getInstanceId(), localToken);
                        break;
                    case NETWORK:
                        this.networkPlugin.deleteInstance(order.getInstanceId(), localToken);
                        break;
                    case ATTACHMENT:
                        this.attachmentPlugin.deleteInstance(order.getInstanceId(), localToken);
                        break;
                    default:
                        LOGGER.error("No deleteInstance plugin implemented for order " + order.getType());
                        break;
                }
            } else {
                // If instanceId is null, then there is nothing to do.
                return;
            }
        } catch (InstanceNotFoundException e) {
            // This may happen if the resource-allocation-service crashed after the instance is deleted
            // but before the new state is updated in stable storage.
            LOGGER.warn("Instance has already been deleted");
            return;
        }
    }

    @Override
    public Instance getInstance(Order order) throws FogbowManagerException, UnexpectedException {
        Instance instance;
        Token localToken = this.mapperPlugin.getToken(order.getFederationUser());
        synchronized (order) {
        	if (order.getOrderState() == OrderState.DEACTIVATED || order.getOrderState() == OrderState.CLOSED) {
        		throw new InstanceNotFoundException();
        	}
            String instanceId = order.getInstanceId();
            if (instanceId != null) {
                instance = getResourceInstance(order, order.getType(), localToken);
            } else {
                // When there is no instance, an empty one is created with the appropriate state
            	InstanceState instanceState = getInstanceStateBasedOnOrderState(order);
            	
                switch (order.getType()) {
                    case COMPUTE:
                        instance = new ComputeInstance(order.getId());
                        break;
                    case VOLUME:
                        instance = new VolumeInstance(order.getId());
                        break;
                    case NETWORK:
                        instance = new NetworkInstance(order.getId());
                        break;
                    case ATTACHMENT:
                        instance = new AttachmentInstance(order.getId());
                        break;
                    default:
                        String message = "Not supported order type " + order.getType();
                        throw new UnexpectedException(message);
                }
                instance.setState(instanceState);
            }
        }
        return instance;
    }

    @Override
    public Quota getUserQuota(FederationUser federationUser, ResourceType resourceType) throws
            FogbowManagerException, UnexpectedException {
        Token localToken = this.mapperPlugin.getToken(federationUser);
        switch (resourceType) {
            case COMPUTE:
                return this.computeQuotaPlugin.getUserQuota(localToken);
            default:
                throw new UnexpectedException("Not yet implemented quota endpoint for " + resourceType);
        }
    }

    @Override
    public Map<String, String> getAllImages(FederationUser federationUser)
            throws FogbowManagerException, UnexpectedException {
        Token localToken = this.mapperPlugin.getToken(federationUser);
        return this.imagePlugin.getAllImages(localToken);
    }

    @Override
    public Image getImage(String imageId, FederationUser federationUser)
            throws FogbowManagerException, UnexpectedException {
        Token localToken = this.mapperPlugin.getToken(federationUser);
        return this.imagePlugin.getImage(imageId, localToken);
    }

    private Instance getResourceInstance(Order order, ResourceType resourceType, Token localToken)
            throws FogbowManagerException, UnexpectedException {
        Instance instance;
        String instanceId = order.getInstanceId();
        switch (resourceType) {
            case COMPUTE:
                instance = this.computePlugin.getInstance(instanceId, localToken);
                break;
            case NETWORK:
                instance = this.networkPlugin.getInstance(instanceId, localToken);
                break;
            case VOLUME:
                instance = this.volumePlugin.getInstance(instanceId, localToken);
                break;
            case ATTACHMENT:
                instance = this.attachmentPlugin.getInstance(instanceId, localToken);
                break;
            default:
                String message = "Not supported order type " + order.getType();
                throw new UnexpectedException(message);
        }
        order.setCachedInstanceState(instance.getState());
        instance.setProvider(order.getProvidingMember());
        return instance;
    }
    
    private InstanceState getInstanceStateBasedOnOrderState(Order order) {
    	InstanceState instanceState = null;
    	// If order state is DEACTIVATED or CLOSED, an exception is throw before method call.
    	// If order state is FULFILLED or SPAWNING, the order has an instance id, so this method is never called.
    	if (order.getOrderState() == OrderState.OPEN || order.getOrderState() == OrderState.PENDING) {
    		instanceState = InstanceState.DISPATCHED;
    	} else if (order.getOrderState() == OrderState.FAILED) {
    		instanceState = InstanceState.FAILED;
    	}
    	return instanceState;
    }
}