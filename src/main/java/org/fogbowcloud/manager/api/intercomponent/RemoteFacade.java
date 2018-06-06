package org.fogbowcloud.manager.api.intercomponent;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.api.intercomponent.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.api.intercomponent.xmpp.Event;
import org.fogbowcloud.manager.core.OrderController;
import org.fogbowcloud.manager.core.OrderStateTransitioner;
import org.fogbowcloud.manager.core.UserQuotaController;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.constants.Operation;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.quotas.Quota;
import org.fogbowcloud.manager.core.plugins.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.plugins.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.instances.Instance;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.fogbowcloud.manager.core.AaController;

public class RemoteFacade {

    private static final Logger LOGGER = Logger.getLogger(RemoteFacade.class);

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
        OrderStateTransitioner.activateOrder(order);
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

    public void handleRemoteEvent(Event event, Order order) {
        // order is a java object that represents the order passed in the message
        // actualOrder is the java object that represents this order inside the current manager
        Order actualOrder = this.orderController.getOrder(order.getId(), order.getFederationUser(), order.getType());
        switch (event) {
            case INSTANCE_FULFILLED:
                try {
                    OrderStateTransitioner.transition(actualOrder, OrderState.FULFILLED);
                } catch (OrderStateTransitionException e) {
                    LOGGER.error(e.getMessage(), e);
                }
                break;
            case INSTANCE_FAILED:
                try {
                    OrderStateTransitioner.transition(actualOrder, OrderState.FAILED);
                } catch (OrderStateTransitionException e) {
                    LOGGER.error(e.getMessage(), e);
                }
                break;
        }
    }

    public void setAaController(AaController AaController) {
        this.aaController = AaController;
    }

    public void setOrderController(OrderController orderController) {
        this.orderController = orderController;
    }

    public void setUserQuotaController(UserQuotaController userQuotaController) {
        this.userQuotaController = userQuotaController;
    }
}
