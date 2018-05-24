package org.fogbowcloud.manager.core.instanceprovider;

import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.manager.plugins.compute.ComputePlugin;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.manager.plugins.network.NetworkPlugin;
import org.fogbowcloud.manager.core.manager.plugins.volume.VolumePlugin;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.NetworkOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.VolumeOrder;
import org.fogbowcloud.manager.core.models.orders.instances.ComputeOrderInstance;
import org.fogbowcloud.manager.core.models.orders.instances.NetworkOrderInstance;
import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;
import org.fogbowcloud.manager.core.models.orders.instances.VolumeOrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.services.AAAController;

public class LocalInstanceProvider implements InstanceProvider {

    private final ComputePlugin computePlugin;
    private final NetworkPlugin networkPlugin;
    private final VolumePlugin volumePlugin;
    private final AAAController aaaController;

    public LocalInstanceProvider(ComputePlugin computePlugin, NetworkPlugin networkPlugin,
            VolumePlugin volumePlugin, AAAController aaaController) {
        super();
        this.computePlugin = computePlugin;
        this.networkPlugin = networkPlugin;
        this.volumePlugin = volumePlugin;
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
                String imageName = computeOrder.getImageName();
                requestInstance =
                        this.computePlugin.requestInstance(computeOrder, localToken, imageName);
                break;

            case NETWORK:
                NetworkOrder networkOrder = (NetworkOrder) order;
                requestInstance = this.networkPlugin.requestInstance(networkOrder, localToken);
                break;

            case VOLUME:
                VolumeOrder volumeOrder = (VolumeOrder) order;
                requestInstance = this.volumePlugin.requestInstance(volumeOrder, localToken);
                break;
        }
        if (requestInstance == null) {
            throw new UnsupportedOperationException("Not implemented yet.");
        }
        return requestInstance;
    }

    // TODO check the possibility of changing the parameter 'orderInstance' to 'order'
    @Override
    public void deleteInstance(OrderInstance orderInstance) throws Exception {
        Token localToken = this.aaaController.getLocalToken();
        if (orderInstance instanceof ComputeOrderInstance) {
            this.computePlugin.deleteInstance(localToken, orderInstance);
        } else if (orderInstance instanceof NetworkOrderInstance) {
            this.networkPlugin.deleteInstance(localToken, orderInstance.getId());
        } else if (orderInstance instanceof VolumeOrderInstance) {
            // TODO rename method 'removeInstance' to 'deleteInstance'
            this.volumePlugin.removeInstance(localToken, orderInstance.getId());
        }
    }

    @Override
    public OrderInstance getInstance(Order order) throws RequestException, TokenCreationException,
            UnauthorizedException, PropertyNotSpecifiedException {
        OrderInstance orderInstance = null;
        Token localToken = this.aaaController.getLocalToken();
        String orderInstanceId = order.getOrderInstance().getId();
        switch (order.getType()) {
            case COMPUTE:
                orderInstance = this.computePlugin.getInstance(localToken, orderInstanceId);
                break;

            case NETWORK:
                orderInstance = this.networkPlugin.getInstance(localToken, orderInstanceId);
                break;

            case VOLUME:
                orderInstance = this.volumePlugin.getInstance(localToken, orderInstanceId);
                break;
        }
        if (orderInstance == null) {
            throw new UnsupportedOperationException("Not implemented yet.");
        }
        return orderInstance;
    }
}
