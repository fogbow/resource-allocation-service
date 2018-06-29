package org.fogbowcloud.manager.core.intercomponent;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.intercomponent.xmpp.Event;
import org.fogbowcloud.manager.core.OrderController;
import org.fogbowcloud.manager.core.OrderStateTransitioner;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnector;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.constants.Operation;
import org.fogbowcloud.manager.core.models.images.Image;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.quotas.Quota;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.instances.Instance;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.fogbowcloud.manager.core.AaController;

import java.util.Map;

public class RemoteFacade {

    private static final Logger LOGGER = Logger.getLogger(RemoteFacade.class);

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

    public void activateOrder(Order order) throws FogbowManagerException, UnexpectedException {
        this.aaController.authorize(order.getFederationUser(), Operation.CREATE, order);
        OrderStateTransitioner.activateOrder(order);
    }

    public Instance getResourceInstance(String orderId, FederationUser federationUser, InstanceType instanceType) throws
            Exception {

        Order order = this.orderController.getOrder(orderId, federationUser, instanceType);
        this.aaController.authorize(federationUser, Operation.GET, order);

        return this.orderController.getResourceInstance(order);
    }

    public void deleteOrder(String orderId, FederationUser federationUser, InstanceType instanceType)
            throws FogbowManagerException, UnexpectedException {

        Order order = this.orderController.getOrder(orderId, federationUser, instanceType);
        this.aaController.authorize(federationUser, Operation.DELETE, order);

        this.orderController.deleteOrder(order);
    }

    public Quota getUserQuota(String memberId, FederationUser federationUser, InstanceType instanceType) throws
            Exception {

        this.aaController.authorize(federationUser, Operation.GET_USER_QUOTA, instanceType);

        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId);
        return cloudConnector.getUserQuota(federationUser, instanceType);
    }

    public Image getImage(String memberId, String imageId, FederationUser federationUser) throws
            Exception {

        this.aaController.authorize(federationUser, Operation.GET_IMAGE);

        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId);
        return cloudConnector.getImage(imageId, federationUser);
    }

    public Map<String, String> getAllImages(String memberId, FederationUser federationUser) throws
            Exception {

        this.aaController.authorize(federationUser, Operation.GET_ALL_IMAGES);

        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId);
        return cloudConnector.getAllImages(federationUser);
    }

    public void handleRemoteEvent(Event event, Order order) throws FogbowManagerException, UnexpectedException {
        // order is a java object that represents the order passed in the message
        // actualOrder is the java object that represents this order inside the current manager
        Order actualOrder = this.orderController.getOrder(order.getId(), order.getFederationUser(), order.getType());
        actualOrder.setInstanceId(order.getInstanceId());
        switch (event) {
            case INSTANCE_FULFILLED:
                OrderStateTransitioner.transition(actualOrder, OrderState.FULFILLED);
                break;
            case INSTANCE_FAILED:
                OrderStateTransitioner.transition(actualOrder, OrderState.FAILED);
                break;
        }
    }

    public void setAaController(AaController AaController) {
        this.aaController = AaController;
    }

    public void setOrderController(OrderController orderController) {
        this.orderController = orderController;
    }
}
