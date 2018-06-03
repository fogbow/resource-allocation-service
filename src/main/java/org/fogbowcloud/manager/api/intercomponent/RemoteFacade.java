package org.fogbowcloud.manager.api.intercomponent;

import org.fogbowcloud.manager.api.intercomponent.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.core.OrderController;
import org.fogbowcloud.manager.core.UserQuotaController;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.constants.Operation;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.quotas.Quota;
import org.fogbowcloud.manager.core.plugins.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.plugins.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.instances.Instance;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.fogbowcloud.manager.core.services.AaController;

public class RemoteFacade {

    private static RemoteFacade instance;

    private AaController aaController;
    private OrderController orderController;
    private UserQuotaController userQuotaController;

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

    public Instance getResourceInstance(String orderId, FederationUser federationUser, InstanceType instanceType) throws
            UnauthorizedException, PropertyNotSpecifiedException,
            TokenCreationException, RequestException, InstanceNotFoundException, RemoteRequestException {

        Order order = this.orderController.getOrder(orderId, federationUser, instanceType);
        this.aaController.authorize(federationUser, Operation.GET, order);

        return this.orderController.getResourceInstance(order);
    }

    public void deleteOrder(String orderId, FederationUser federationUser, InstanceType instanceType)
            throws UnauthorizedException, OrderManagementException {

        Order order = this.orderController.getOrder(orderId, federationUser, instanceType);
        this.aaController.authorize(federationUser, Operation.DELETE, order);

        this.orderController.deleteOrder(order);
    }

    public Quota getUserQuota(String memberId, FederationUser federationUser, InstanceType instanceType) throws
            UnauthorizedException, QuotaException, TokenCreationException,
            PropertyNotSpecifiedException, RemoteRequestException {

        this.aaController.authorize(federationUser, Operation.GET_USER_QUOTA, instanceType);

        return this.userQuotaController.getUserQuota(memberId, federationUser, instanceType);
    }

    public void setAaController(AaController AaController) {
        this.aaController = AaController;
    }

    public void setOrderController(OrderController orderController) {
        this.orderController = orderController;
    }

}
