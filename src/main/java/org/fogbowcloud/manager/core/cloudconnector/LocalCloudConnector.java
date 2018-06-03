package org.fogbowcloud.manager.core.cloudconnector;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.api.intercomponent.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.core.CloudPluginsHolder;
import org.fogbowcloud.manager.core.OrderController;
import org.fogbowcloud.manager.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.exceptions.QuotaException;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.orders.*;
import org.fogbowcloud.manager.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.manager.core.plugins.cloud.attachment.AttachmentPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.compute.ComputePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.network.NetworkPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.quota.ComputeQuotaPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.volume.VolumePlugin;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.instances.Instance;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.quotas.Quota;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.plugins.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.plugins.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.services.AaController;
import org.fogbowcloud.manager.core.models.quotas.allocation.Allocation;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class LocalCloudConnector implements CloudConnector {

    private final String memberId;
    private final AaController aaController;
    private final OrderController orderController;
    private final AttachmentPlugin attachmentPlugin;
    private final ComputePlugin computePlugin;
    private final ComputeQuotaPlugin computeQuotaPlugin;
    private final NetworkPlugin networkPlugin;
    private final VolumePlugin volumePlugin;

    private static final Logger LOGGER = Logger.getLogger(LocalCloudConnector.class);

    public LocalCloudConnector(String memberId, AaController aaController, OrderController orderController,
                               CloudPluginsHolder cloudPluginsHolder) {
        super();
        this.memberId = memberId;
        this.aaController = aaController;
        this.orderController = orderController;
        this.attachmentPlugin = cloudPluginsHolder.getAttachmentPlugin();
        this.computePlugin = cloudPluginsHolder.getComputePlugin();
        this.computeQuotaPlugin = cloudPluginsHolder.getComputeQuotaPlugin();
        this.networkPlugin = cloudPluginsHolder.getNetworkPlugin();
        this.volumePlugin = cloudPluginsHolder.getVolumePlugin();
    }

    @Override
    public String requestInstance(Order order) throws PropertyNotSpecifiedException, RequestException,
            UnauthorizedException, TokenCreationException {
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
        }
        if (requestInstance == null) {
            throw new UnsupportedOperationException("Not implemented yet.");
        }
        return requestInstance;
    }

    @Override
    public void deleteInstance(Order order) throws RequestException, PropertyNotSpecifiedException,
            UnauthorizedException, TokenCreationException {

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
    public Instance getInstance(Order order) throws RequestException, PropertyNotSpecifiedException,
            InstanceNotFoundException, UnauthorizedException, TokenCreationException {
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
            PropertyNotSpecifiedException, QuotaException, UnauthorizedException, TokenCreationException {

        Token localToken = this.aaController.getLocalToken(federationUser);

        switch (instanceType) {
            case COMPUTE:
                return this.computeQuotaPlugin.getUserQuota(localToken);
            default:
                throw new UnsupportedOperationException("Not yet implemented.");
        }
    }

    @Override
    public Allocation getUserAllocation(Collection<Order> orders, InstanceType instanceType)
            throws RemoteRequestException, InstanceNotFoundException, RequestException, QuotaException,
            TokenCreationException, PropertyNotSpecifiedException, UnauthorizedException {

        switch (instanceType) {
            case COMPUTE:
                return (Allocation) getUserComputeAllocation(orders);
            default:
                throw new UnsupportedOperationException("Not yet implemented.");
        }
    }

    private ComputeAllocation getUserComputeAllocation(Collection<Order> computeOrders) throws
            QuotaException, RemoteRequestException, RequestException, TokenCreationException,
            UnauthorizedException, PropertyNotSpecifiedException, InstanceNotFoundException {

        int vCPU = 0, ram = 0, instances = 0;

        for (Order order : computeOrders) {
            ComputeInstance computeInstance = (ComputeInstance) this.getInstance(order);
            vCPU += computeInstance.getvCPU();
            ram += computeInstance.getMemory();
            instances++;
        }

        return new ComputeAllocation(vCPU, ram, instances);
    }

    private Instance getResourceInstance(Order order, InstanceType instanceType, Token localToken)
            throws RequestException {
        Instance instance;
        String instanceId = order.getInstanceId();

        switch (instanceType) {
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
