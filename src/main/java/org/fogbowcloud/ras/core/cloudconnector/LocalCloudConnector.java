package org.fogbowcloud.ras.core.cloudconnector;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.InteroperabilityPluginsHolder;
import org.fogbowcloud.ras.core.SharedOrderHolders;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.images.Image;
import org.fogbowcloud.ras.core.models.instances.*;
import org.fogbowcloud.ras.core.models.orders.*;
import org.fogbowcloud.ras.core.models.quotas.Quota;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.aaa.mapper.FederationToLocalMapperPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class LocalCloudConnector implements CloudConnector {
    private static final Logger LOGGER = Logger.getLogger(LocalCloudConnector.class);

    private final FederationToLocalMapperPlugin mapperPlugin;
    private final PublicIpPlugin<Token> publicIpPlugin;
    private final AttachmentPlugin<Token> attachmentPlugin;
    private final ComputePlugin<Token> computePlugin;
    private final ComputeQuotaPlugin computeQuotaPlugin;
    private final NetworkPlugin<Token> networkPlugin;
    private final VolumePlugin<Token> volumePlugin;
    private final ImagePlugin<Token> imagePlugin;

    public LocalCloudConnector(FederationToLocalMapperPlugin mapperPlugin, InteroperabilityPluginsHolder interoperabilityPluginsHolder) {
        this.mapperPlugin = mapperPlugin;
        this.publicIpPlugin = interoperabilityPluginsHolder.getPublicIpPlugin();
        this.attachmentPlugin = interoperabilityPluginsHolder.getAttachmentPlugin();
        this.computePlugin = interoperabilityPluginsHolder.getComputePlugin();
        this.computeQuotaPlugin = interoperabilityPluginsHolder.getComputeQuotaPlugin();
        this.networkPlugin = interoperabilityPluginsHolder.getNetworkPlugin();
        this.volumePlugin = interoperabilityPluginsHolder.getVolumePlugin();
        this.imagePlugin = interoperabilityPluginsHolder.getImagePlugin();
    }

    @Override
    public String requestInstance(Order order) throws FogbowRasException, UnexpectedException {
        String requestInstance = null;
        Token token = this.mapperPlugin.map(order.getFederationUserToken());
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
                    requestInstance = this.computePlugin.requestInstance(computeOrder, token);
                } catch (Throwable e) {
                    throw e;
                } finally {
                    computeOrder.setNetworksId(savedNetworkOrderIds);
                }
                break;
            case NETWORK:
                NetworkOrder networkOrder = (NetworkOrder) order;
                requestInstance = this.networkPlugin.requestInstance(networkOrder, token);
                break;
            case VOLUME:
                VolumeOrder volumeOrder = (VolumeOrder) order;
                requestInstance = this.volumePlugin.requestInstance(volumeOrder, token);
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
                    requestInstance = this.attachmentPlugin.requestInstance(attachmentOrder, token);
                } catch (Throwable e) {
                    throw e;
                } finally {
                    attachmentOrder.setSource(savedSource);
                    attachmentOrder.setTarget(savedTarget);
                }
                break;
            case PUBLIC_IP:
                PublicIpOrder publicIpOrder = (PublicIpOrder) order;

                String computeOrderId = publicIpOrder.getComputeOrderId();

                Order retrievedComputeOrder = SharedOrderHolders.getInstance().getActiveOrdersMap()
                        .get(computeOrderId);

                String computeInstanceId = retrievedComputeOrder.getInstanceId();
                if (computeInstanceId != null) {
                    requestInstance = this.publicIpPlugin.requestInstance(publicIpOrder, computeInstanceId, token);
                }
                break;
            default:
                throw new UnexpectedException(String.format(Messages.Exception.PLUGIN_FOR_REQUEST_INSTANCE_NOT_IMPLEMENTED, order.getType()));
        }
        if (requestInstance == null) {
            throw new UnexpectedException(Messages.Exception.RETURNED_NULL_VALUE);
        }
        return requestInstance;
    }

    @Override
    public void deleteInstance(Order order) throws FogbowRasException, UnexpectedException {
        try {
            if (order.getInstanceId() != null) {
                Token token = this.mapperPlugin.map(order.getFederationUserToken());
                switch (order.getType()) {
                    case COMPUTE:
                        this.computePlugin.deleteInstance(order.getInstanceId(), token);
                        break;
                    case VOLUME:
                        this.volumePlugin.deleteInstance(order.getInstanceId(), token);
                        break;
                    case NETWORK:
                        this.networkPlugin.deleteInstance(order.getInstanceId(), token);
                        break;
                    case ATTACHMENT:
                        this.attachmentPlugin.deleteInstance(order.getInstanceId(), token);
                        break;
                    case PUBLIC_IP:
                        this.publicIpPlugin.deleteInstance(order.getInstanceId(), token);
                    default:
                        LOGGER.error(String.format(Messages.Error.DELETE_INSTANCE_PLUGIN_NOT_IMPLEMENTED, order.getType()));
                        break;
                }
            } else {
                // If instanceId is null, then there is nothing to do.
                return;
            }
        } catch (InstanceNotFoundException e) {
            // This may happen if the resource-allocation-service crashed after the instance is deleted
            // but before the new state is updated in stable storage.
            LOGGER.warn(Messages.Warn.INSTANCE_ALREADY_DELETED);
            return;
        }
    }

    @Override
    public Instance getInstance(Order order) throws FogbowRasException, UnexpectedException {
        Instance instance;
        Token token = this.mapperPlugin.map(order.getFederationUserToken());
        synchronized (order) {
            if (order.getOrderState() == OrderState.DEACTIVATED || order.getOrderState() == OrderState.CLOSED) {
                throw new InstanceNotFoundException();
            }
            String instanceId = order.getInstanceId();
            if (instanceId != null) {
                instance = getResourceInstance(order, order.getType(), token);
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
                    case PUBLIC_IP:
                        instance = new PublicIpInstance(order.getId());
                        break;
                    default:
                        throw new UnexpectedException(Messages.Exception.ORDER_TYPE_NOT_SUPPORTED);
                }
                InstanceState instanceState = getInstanceStateBasedOnOrderState(order);
                instance.setState(instanceState);
            }
        }
        return instance;
    }

    @Override
    public Quota getUserQuota(FederationUserToken federationUserToken, ResourceType resourceType) throws
            FogbowRasException, UnexpectedException {
        Token token = this.mapperPlugin.map(federationUserToken);
        switch (resourceType) {
            case COMPUTE:
                return this.computeQuotaPlugin.getUserQuota(token);
            default:
                throw new UnexpectedException(String.format(Messages.Exception.QUOTA_ENDPOINT_NOT_IMPLEMENTED, resourceType));
        }
    }

    @Override
    public Map<String, String> getAllImages(FederationUserToken federationUserToken)
            throws FogbowRasException, UnexpectedException {
        Token token = this.mapperPlugin.map(federationUserToken);
        return this.imagePlugin.getAllImages(token);
    }

    @Override
    public Image getImage(String imageId, FederationUserToken federationUserToken)
            throws FogbowRasException, UnexpectedException {
        Token token = this.mapperPlugin.map(federationUserToken);
        return this.imagePlugin.getImage(imageId, token);
    }

    /**
     * protected visibility for tests
     */
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

    private Instance getResourceInstance(Order order, ResourceType resourceType, Token token)
            throws FogbowRasException, UnexpectedException {
        Instance instance;
        String instanceId = order.getInstanceId();
        switch (resourceType) {
            case COMPUTE:
                instance = this.computePlugin.getInstance(instanceId, token);
                break;
            case NETWORK:
                instance = this.networkPlugin.getInstance(instanceId, token);
                break;
            case VOLUME:
                instance = this.volumePlugin.getInstance(instanceId, token);
                break;
            case ATTACHMENT:
                instance = this.attachmentPlugin.getInstance(instanceId, token);
                break;
            case PUBLIC_IP:
                instance = this.publicIpPlugin.getInstance(instanceId, token);
                break;
            default:
                throw new UnexpectedException(String.format(Messages.Exception.ORDER_TYPE_NOT_SUPPORTED, order.getType()));
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