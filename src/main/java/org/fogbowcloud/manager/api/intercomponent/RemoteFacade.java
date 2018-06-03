package org.fogbowcloud.manager.api.intercomponent;

import org.fogbowcloud.manager.api.intercomponent.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.core.OrderController;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnectorSelector;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.constants.Operation;
import org.fogbowcloud.manager.core.models.quotas.Quota;
import org.fogbowcloud.manager.core.models.quotas.allocation.Allocation;
import org.fogbowcloud.manager.core.plugins.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.plugins.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.orders.instances.Instance;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.fogbowcloud.manager.core.services.AaController;

public class RemoteFacade {

    private static RemoteFacade instance;

    private AaController aaController;
    private OrderController orderController;

    public static RemoteFacade getInstance() {
        synchronized (RemoteFacade.class) {
            if (instance == null) {
                instance = new RemoteFacade();
            }
            return instance;
        }
    }

    public void activateOrder(Order order) throws UnauthorizedException, OrderManagementException {
        this.aaController.authorize(order.getFederationUser(), Operation.CREATE, order);
        this.orderController.activateOrder(order);
    }

    public Instance getResourceInstance(String orderId, FederationUser federationUser, OrderType orderType) throws
            UnauthorizedException, PropertyNotSpecifiedException,
            TokenCreationException, RequestException, InstanceNotFoundException, RemoteRequestException {

        Order order = this.orderController.getOrder(orderId, federationUser, orderType);
        this.aaController.authorize(federationUser, Operation.GET, order);

        return this.orderController.getResourceInstance(order);
    }

    public void deleteOrder(String orderId, FederationUser federationUser, OrderType orderType)
            throws UnauthorizedException, OrderManagementException {

        Order order = this.orderController.getOrder(orderId, federationUser, orderType);
        this.aaController.authorize(federationUser, Operation.DELETE, order);

        this.orderController.deleteOrder(order);
    }

    public Quota getUserQuota(FederationUser federationUser) throws UnauthorizedException, QuotaException,
            TokenCreationException, PropertyNotSpecifiedException {

        this.aaController.authorize(federationUser, Operation.GET_USER_QUOTA);

        CloudConnectorSelector selector = CloudConnectorSelector.getInstance();

        return (Quota) selector.getLocalCloudConnector().getComputeQuota(selector.getLocalMemberId(), federationUser);
    }

    public Allocation getUserAllocation(FederationUser federationUser) throws InstanceNotFoundException,
            RequestException, QuotaException, PropertyNotSpecifiedException, RemoteRequestException,
            TokenCreationException, UnauthorizedException {

        this.aaController.authorize(federationUser, Operation.GET_USER_ALLOCATION);

        return (Allocation) this.orderController.getComputeAllocation(federationUser);
    }

    public void setAaController(AaController AaController) {
        this.aaController = AaController;
    }

    public void setOrderController(OrderController orderController) {
        this.orderController = orderController;
    }

}
