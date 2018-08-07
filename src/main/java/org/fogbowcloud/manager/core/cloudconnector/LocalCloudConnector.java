package org.fogbowcloud.manager.core.cloudconnector;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.CloudPluginsHolder;
import org.fogbowcloud.manager.core.SharedOrderHolders;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.images.Image;
import org.fogbowcloud.manager.core.models.instances.*;
import org.fogbowcloud.manager.core.models.orders.*;
import org.fogbowcloud.manager.core.models.quotas.Quota;
import org.fogbowcloud.manager.core.models.tokens.FederationUserAttributes;
import org.fogbowcloud.manager.core.models.tokens.LocalUserAttributes;
import org.fogbowcloud.manager.core.plugins.behavior.mapper.FederationToLocalMapperPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.AttachmentPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.ComputePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.ImagePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.NetworkPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.ComputeQuotaPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.VolumePlugin;

import java.util.LinkedList;
import java.util.List;
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
        LocalUserAttributes localUserAttributes = this.mapperPlugin.map(order.getFederationUserAttributes());
        switch (order.getType()) {
            case COMPUTE:
                // As the order parameter came from the rest API, the NetworkInstanceIds in the order are actually
                // NetworkOrderIds, since these are the Ids that are known to users/applications using the API.
                // Thus, before requesting the plugin to create the Compute, we need to replace NetworkOrderIds by
                // NetworkInstanceIds, which are contained in the respective NetworkOrder.
                // We save the list of NetworkOrderIds in the original order, to restore these values, after
                // the Compute instance is requested in the cloud.
                ComputeOrder computeOrder = (ComputeOrder) order;
                List<String> savedNetworkOrderIds = computeOrder.getNetworksId();
                List<String> networkInstanceIds = getNetworkInstanceIdsFromNetworkOrderIds(computeOrder);
                computeOrder.setNetworksId(networkInstanceIds);
                try {
                    requestInstance = this.computePlugin.requestInstance(computeOrder, localUserAttributes);
                } catch (Throwable e) {
                    throw e;
                } finally {
                    computeOrder.setNetworksId(savedNetworkOrderIds);
                }
                break;
            case NETWORK:
                NetworkOrder networkOrder = (NetworkOrder) order;
                requestInstance = this.networkPlugin.requestInstance(networkOrder, localUserAttributes);
                break;
            case VOLUME:
                VolumeOrder volumeOrder = (VolumeOrder) order;
                requestInstance = this.volumePlugin.requestInstance(volumeOrder, localUserAttributes);
                break;
            case ATTACHMENT:
                // As the order parameter came from the rest API, the Source and Target fields are actually
                // ComputeOrder and VolumeOrder Ids, since these are the Ids that are known to users/applications
                // using the API. Thus, before requesting the plugin to create the Attachment, we need to replace
                // The ComputeOrderId of the source by its corresponding ComputeInstanceId, and the VolumeOrderId
                // of the target by its corresponding VolumeInstanceId.
                // We save the Order Ids in the original order, to restore these values, after the Attachment is
                // requested in the cloud.
                AttachmentOrder attachmentOrder = (AttachmentOrder) order;
                String savedSource = attachmentOrder.getSource();
                String savedTarget = attachmentOrder.getTarget();
                Order sourceOrder = SharedOrderHolders.getInstance().getActiveOrdersMap().get(savedSource);
                Order targetOrder = SharedOrderHolders.getInstance().getActiveOrdersMap().get(savedTarget);
                attachmentOrder.setSource(sourceOrder.getInstanceId());
                attachmentOrder.setTarget(targetOrder.getInstanceId());
                try {
                    requestInstance = this.attachmentPlugin.requestInstance(attachmentOrder, localUserAttributes);
                } catch (Throwable e) {
                    throw e;
                } finally {
                    attachmentOrder.setSource(savedSource);
                    attachmentOrder.setTarget(savedTarget);
                }
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
                LocalUserAttributes localUserAttributes = this.mapperPlugin.map(order.getFederationUserAttributes());
                switch (order.getType()) {
                    case COMPUTE:
                        this.computePlugin.deleteInstance(order.getInstanceId(), localUserAttributes);
                        break;
                    case VOLUME:
                        this.volumePlugin.deleteInstance(order.getInstanceId(), localUserAttributes);
                        break;
                    case NETWORK:
                        this.networkPlugin.deleteInstance(order.getInstanceId(), localUserAttributes);
                        break;
                    case ATTACHMENT:
                        this.attachmentPlugin.deleteInstance(order.getInstanceId(), localUserAttributes);
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
        LocalUserAttributes localUserAttributes = this.mapperPlugin.map(order.getFederationUserAttributes());
        synchronized (order) {
        	if (order.getOrderState() == OrderState.DEACTIVATED || order.getOrderState() == OrderState.CLOSED) {
        		throw new InstanceNotFoundException();
        	}
            String instanceId = order.getInstanceId();
            if (instanceId != null) {
                instance = getResourceInstance(order, order.getType(), localUserAttributes);
                // The user believes that the order id is actually the instance id.
                // So we need to set the instance id accordingly before returning the instance.
                instance.setId(order.getId());
            } else {
                // When there is no instance, an empty one is created with the appropriate state
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
                InstanceState instanceState = getInstanceStateBasedOnOrderState(order);
                instance.setState(instanceState);
            }
        }
        return instance;
    }

    @Override
    public Quota getUserQuota(FederationUserAttributes federationUserAttributes, ResourceType resourceType) throws
            FogbowManagerException, UnexpectedException {
        LocalUserAttributes localUserAttributes = this.mapperPlugin.map(federationUserAttributes);
        switch (resourceType) {
            case COMPUTE:
                return this.computeQuotaPlugin.getUserQuota(localUserAttributes);
            default:
                throw new UnexpectedException("Not yet implemented quota endpoint for " + resourceType);
        }
    }

    @Override
    public Map<String, String> getAllImages(FederationUserAttributes federationUserAttributes)
            throws FogbowManagerException, UnexpectedException {
        LocalUserAttributes localUserAttributes = this.mapperPlugin.map(federationUserAttributes);
        return this.imagePlugin.getAllImages(localUserAttributes);
    }

    @Override
    public Image getImage(String imageId, FederationUserAttributes federationUserAttributes)
            throws FogbowManagerException, UnexpectedException {
        LocalUserAttributes localUserAttributes = this.mapperPlugin.map(federationUserAttributes);
        return this.imagePlugin.getImage(imageId, localUserAttributes);
    }

    /** protected visibility for tests */
    protected List<String> getNetworkInstanceIdsFromNetworkOrderIds(ComputeOrder order) {
        List<String> networkOrdersId = order.getNetworksId();
        List<String> networkInstanceIDs = new LinkedList<String>();

        for (String orderId : networkOrdersId) {
            Order networkOrder = SharedOrderHolders.getInstance().getActiveOrdersMap().get(orderId);
            String instanceId = networkOrder.getInstanceId();
            networkInstanceIDs.add(instanceId);
        }
        return networkInstanceIDs;
    }

    private Instance getResourceInstance(Order order, ResourceType resourceType, LocalUserAttributes localUserAttributes)
            throws FogbowManagerException, UnexpectedException {
        Instance instance;
        String instanceId = order.getInstanceId();
        switch (resourceType) {
            case COMPUTE:
                instance = this.computePlugin.getInstance(instanceId, localUserAttributes);
                break;
            case NETWORK:
                instance = this.networkPlugin.getInstance(instanceId, localUserAttributes);
                break;
            case VOLUME:
                instance = this.volumePlugin.getInstance(instanceId, localUserAttributes);
                break;
            case ATTACHMENT:
                instance = this.attachmentPlugin.getInstance(instanceId, localUserAttributes);
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