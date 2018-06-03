package org.fogbowcloud.manager.core.cloudconnector;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.CloudPluginsHolder;
import org.fogbowcloud.manager.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.exceptions.QuotaException;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.plugins.cloud.attachment.AttachmentPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.compute.ComputePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.network.NetworkPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.quota.ComputeQuotaPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.volume.VolumePlugin;
import org.fogbowcloud.manager.core.models.orders.AttachmentOrder;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.NetworkOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.orders.VolumeOrder;
import org.fogbowcloud.manager.core.models.orders.instances.Instance;
import org.fogbowcloud.manager.core.models.orders.instances.InstanceState;
import org.fogbowcloud.manager.core.models.quotas.ComputeQuota;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.services.AaController;

public class LocalCloudConnector implements CloudConnector {

    private final AaController AaController;
    private final AttachmentPlugin attachmentPlugin;
    private final ComputePlugin computePlugin;
    private final ComputeQuotaPlugin computeQuotaPlugin;
    private final NetworkPlugin networkPlugin;
    private final VolumePlugin volumePlugin;

    private static final Logger LOGGER = Logger.getLogger(LocalCloudConnector.class);

    public LocalCloudConnector(AaController AaController, CloudPluginsHolder cloudPluginsHolder) {
        super();
        this.AaController = AaController;
        this.attachmentPlugin = cloudPluginsHolder.getAttachmentPlugin();
        this.computePlugin = cloudPluginsHolder.getComputePlugin();
        this.computeQuotaPlugin = cloudPluginsHolder.getComputeQuotaPlugin();
        this.networkPlugin = cloudPluginsHolder.getNetworkPlugin();
        this.volumePlugin = cloudPluginsHolder.getVolumePlugin();
    }

    @Override
    public String requestInstance(Order order) throws PropertyNotSpecifiedException, RequestException {
        String requestInstance = null;
        Token localToken = this.AaController.getLocalToken(order.getFederationUser());
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
    public void deleteInstance(Order order) throws RequestException, PropertyNotSpecifiedException {
        Token localToken = this.AaController.getLocalToken(order.getFederationUser());
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
    public Instance getInstance(Order order) throws RequestException, PropertyNotSpecifiedException,
            InstanceNotFoundException {
        Instance instance;
        Token localToken = this.AaController.getLocalToken(order.getFederationUser());

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
    public ComputeQuota getComputeQuota(FederationUser federationUser) throws PropertyNotSpecifiedException,
            QuotaException {

        Token localToken = this.AaController.getLocalToken(federationUser);

        return this.computeQuotaPlugin.getComputeQuota(localToken);
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
