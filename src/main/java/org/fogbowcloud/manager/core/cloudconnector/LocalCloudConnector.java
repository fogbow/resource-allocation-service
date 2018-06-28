package org.fogbowcloud.manager.core.cloudconnector;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.AaController;
import org.fogbowcloud.manager.core.CloudPluginsHolder;
import org.fogbowcloud.manager.core.OrderController;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.models.images.Image;
import org.fogbowcloud.manager.core.models.instances.Instance;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.orders.*;
import org.fogbowcloud.manager.core.models.quotas.Quota;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.plugins.cloud.AttachmentPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.ComputePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.ImagePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.NetworkPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.ComputeQuotaPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.VolumePlugin;
import org.fogbowcloud.manager.core.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.exceptions.UnauthorizedRequestException;

import java.util.Map;

public class LocalCloudConnector implements CloudConnector {

    private final String memberId;
    private final AaController aaController;
    private final OrderController orderController;
    private final AttachmentPlugin attachmentPlugin;
    private final ComputePlugin computePlugin;
    private final ComputeQuotaPlugin computeQuotaPlugin;
    private final NetworkPlugin networkPlugin;
    private final VolumePlugin volumePlugin;
    private final ImagePlugin imagePlugin;

    private static final Logger LOGGER = Logger.getLogger(LocalCloudConnector.class);

    public LocalCloudConnector(String memberId, AaController aaController, OrderController orderController,
                               CloudPluginsHolder cloudPluginsHolder) {
        //FIXME: is this memberId variable still necessary?
        this.memberId = memberId;
        this.aaController = aaController;
        this.orderController = orderController;
        this.attachmentPlugin = cloudPluginsHolder.getAttachmentPlugin();
        this.computePlugin = cloudPluginsHolder.getComputePlugin();
        this.computeQuotaPlugin = cloudPluginsHolder.getComputeQuotaPlugin();
        this.networkPlugin = cloudPluginsHolder.getNetworkPlugin();
        this.volumePlugin = cloudPluginsHolder.getVolumePlugin();
        this.imagePlugin = cloudPluginsHolder.getImagePlugin();
    }

    @Override
    public String requestInstance(Order order) throws FogbowManagerException {
        String requestInstance = null;
        Token localToken = this.aaController.getLocalToken(order.getFederationUser());
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
        }
        if (requestInstance == null) {
            throw new UnsupportedOperationException("Not implemented yet.");
        }
        return requestInstance;
    }

    @Override
    public void deleteInstance(Order order) throws FogbowManagerException {

        //FIXME: This code does nothing (do not throw an exception nor a flag, e.g false, in case the order
        //does not have an instanceId yet. Is it ok?
        if (order.getInstanceId() != null) {
            Token localToken = this.aaController.getLocalToken(order.getFederationUser());
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
                    LOGGER.error("Undefined type " + order.getType());
                    break;
            }
        }
    }

    @Override
    public Instance getInstance(Order order) throws FogbowManagerException {
        Instance instance;
        Token localToken = this.aaController.getLocalToken(order.getFederationUser());

        synchronized (order) {
            String instanceId = order.getInstanceId();

            if (instanceId != null) {
                instance = getResourceInstance(order, order.getType(), localToken);
            } else {
                // When there is no instance, an empty one is created with the appropriate state
                instance = new Instance(null);
                switch (order.getOrderState()) {
                    case OPEN:
                        instance.setState(InstanceState.INACTIVE);
                        break;
                    case FAILED:
                        instance.setState(InstanceState.FAILED);
                        break;
                    case CLOSED:
                        throw new InstanceNotFoundException();
                    default:
                        LOGGER.error("Inconsistent state.");
                        instance.setState(InstanceState.INCONSISTENT);
                }
            }
        }

        return instance;
    }

    @Override
    public Quota getUserQuota(FederationUser federationUser, InstanceType instanceType) throws
            FogbowManagerException {

        Token localToken = this.aaController.getLocalToken(federationUser);

        switch (instanceType) {
            case COMPUTE:
                return this.computeQuotaPlugin.getUserQuota(localToken);
            default:
                throw new UnsupportedOperationException("Not yet implemented.");
        }
    }

    @Override
    public Map<String, String> getAllImages(FederationUser federationUser) throws FogbowManagerException {
        Token localToken = this.aaController.getLocalToken(federationUser);
        return this.imagePlugin.getAllImages(localToken);
    }

    @Override
    public Image getImage(String imageId, FederationUser federationUser) throws FogbowManagerException {
        Token localToken = this.aaController.getLocalToken(federationUser);
        return this.imagePlugin.getImage(imageId, localToken);
    }

    private Instance getResourceInstance(Order order, InstanceType instanceType, Token localToken)
            throws FogbowManagerException {
        Instance instance;
        String instanceId = order.getInstanceId();

        switch (instanceType) {
            case COMPUTE:
                instance = this.computePlugin.getInstance(instanceId, order.getId(), localToken);
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
                throw new UnsupportedOperationException("Not implemented yet.");
        }

        return instance;
    }

}
