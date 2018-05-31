package org.fogbowcloud.manager.core.instanceprovider;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.manager.plugins.cloud.attachment.AttachmentPlugin;
import org.fogbowcloud.manager.core.manager.plugins.cloud.compute.ComputePlugin;
import org.fogbowcloud.manager.core.manager.plugins.cloud.network.NetworkPlugin;
import org.fogbowcloud.manager.core.manager.plugins.cloud.volume.VolumePlugin;
import org.fogbowcloud.manager.core.manager.plugins.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.manager.plugins.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.orders.AttachmentOrder;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.NetworkOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.orders.VolumeOrder;
import org.fogbowcloud.manager.core.models.orders.instances.Instance;
import org.fogbowcloud.manager.core.models.orders.instances.InstanceState;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.services.AAAController;

public class LocalInstanceProvider implements InstanceProvider {

    private final ComputePlugin computePlugin;
    private final NetworkPlugin networkPlugin;
    private final VolumePlugin volumePlugin;
    private final AttachmentPlugin attachmentPlugin;
    private final AAAController aaaController;

    private static final Logger LOGGER = Logger.getLogger(LocalInstanceProvider.class);

    public LocalInstanceProvider(ComputePlugin computePlugin, NetworkPlugin networkPlugin,
            VolumePlugin volumePlugin, AttachmentPlugin attachmentPlugin, AAAController aaaController) {
        super();
        this.computePlugin = computePlugin;
        this.networkPlugin = networkPlugin;
        this.volumePlugin = volumePlugin;
        this.attachmentPlugin = attachmentPlugin;
        this.aaaController = aaaController;
    }

    @Override
    public String requestInstance(Order order) throws PropertyNotSpecifiedException,
            UnauthorizedException, TokenCreationException, RequestException {
        String requestInstance = null;
        Token localToken = this.aaaController.getLocalToken();
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
        }
        if (requestInstance == null) {
            throw new UnsupportedOperationException("Not implemented yet.");
        }
        return requestInstance;
    }

    @Override
    public void deleteInstance(Order order) throws RequestException, TokenCreationException,
            UnauthorizedException, PropertyNotSpecifiedException {
        Token localToken = this.aaaController.getLocalToken();
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
            default:
                LOGGER.error("Undefined type " + order.getType());
                break;
        }
    }

    @Override
    public Instance getInstance(Order order) throws RequestException, TokenCreationException,
            UnauthorizedException, PropertyNotSpecifiedException, InstanceNotFoundException {
        Instance instance;
        Token localToken = this.aaaController.getLocalToken();

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

    private Instance getResourceInstance(Order order, OrderType orderType, Token localToken)
            throws RequestException {
        Instance instance;
        String instanceId = order.getInstanceId();

        switch (orderType) {
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
                throw new UnsupportedOperationException("Not implemented yet.");
        }

        return instance;
    }
    
}
