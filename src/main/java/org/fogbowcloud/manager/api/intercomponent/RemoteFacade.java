package org.fogbowcloud.manager.api.intercomponent;

import org.fogbowcloud.manager.api.intercomponent.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.core.OrderController;
import org.fogbowcloud.manager.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import org.fogbowcloud.manager.core.constants.Operation;
import org.fogbowcloud.manager.core.manager.plugins.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.manager.plugins.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.orders.instances.Instance;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.fogbowcloud.manager.core.services.AaController;

public class RemoteFacade {

    private static RemoteFacade instance;

    private AaController AaController;
    private OrderController orderController;

    public static RemoteFacade getInstance() {
        synchronized (RemoteFacade.class) {
            if (instance == null) {
                instance = new RemoteFacade();
            }
            return instance;
        }
    }
    
//    public ComputeInstance getCompute(String orderId, FederationUser federationUser) throws UnauthenticatedException,
//            PropertyNotSpecifiedException, RequestException, InstanceNotFoundException, TokenCreationException, UnauthorizedException, RemoteRequestException {
//
//        return (ComputeInstance) getResourceInstance(orderId, federationUser,
//                OrderType.COMPUTE);
//    }

    public void activateOrder(Order order) throws UnauthorizedException, OrderManagementException {
        this.AaController.authorize(order.getFederationUser(), Operation.CREATE, order);
        this.orderController.activateOrder(order);
    }

    public Instance getResourceInstance(String orderId, FederationUser federationUser,
                                         OrderType type) throws UnauthorizedException, PropertyNotSpecifiedException,
            TokenCreationException, RequestException, InstanceNotFoundException, RemoteRequestException {

        Order order = this.orderController.getOrder(orderId, federationUser, type);
        this.AaController.authorize(federationUser, Operation.GET, order);

        return this.orderController.getResourceInstance(order);
    }

    public void deleteOrder(String orderId, FederationUser federationUser, OrderType orderType)
            throws UnauthenticatedException, UnauthorizedException, OrderManagementException {
        Order order = this.orderController.getOrder(orderId, federationUser, orderType);
        this.AaController.authorize(federationUser, Operation.DELETE, order);

        this.orderController.deleteOrder(order);
    }

    public void setAAAController(AaController AaController) {
        this.AaController = AaController;
    }

    public void setOrderController(OrderController orderController) {
        this.orderController = orderController;
    }

}
