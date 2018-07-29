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
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.quotas.Quota;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.instances.Instance;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
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
        this.aaController.authorize(order.getFederationUser(), Operation.CREATE, order.getType());
        OrderStateTransitioner.activateOrder(order);
    }

    public Instance getResourceInstance(String orderId, FederationUser federationUser, ResourceType resourceType) throws
            Exception {

        Order order = this.orderController.getOrder(orderId);
        this.aaController.authorize(federationUser, Operation.GET, order.getType());

        return this.orderController.getResourceInstance(orderId);
    }

    public void deleteOrder(String orderId, FederationUser federationUser, ResourceType resourceType)
            throws FogbowManagerException, UnexpectedException {

        Order order = this.orderController.getOrder(orderId);
        this.aaController.authorize(federationUser, Operation.DELETE, order.getType());

        this.orderController.deleteOrder(orderId);
    }

    public Quota getUserQuota(String memberId, FederationUser federationUser, ResourceType resourceType) throws
            Exception {

        this.aaController.authorize(federationUser, Operation.GET_USER_QUOTA, resourceType);

        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId);
        return cloudConnector.getUserQuota(federationUser, resourceType);
    }

    public Image getImage(String memberId, String imageId, FederationUser federationUser) throws
            Exception {

        this.aaController.authorize(federationUser, Operation.GET_IMAGE, ResourceType.IMAGE);

        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId);
        return cloudConnector.getImage(imageId, federationUser);
    }

    public Map<String, String> getAllImages(String memberId, FederationUser federationUser) throws
            Exception {

        this.aaController.authorize(federationUser, Operation.GET_ALL_IMAGES, ResourceType.IMAGE);

        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId);
        return cloudConnector.getAllImages(federationUser);
    }

    public void handleRemoteEvent(Event event, Order remoteOrder) throws FogbowManagerException, UnexpectedException {
        // order is a java object that represents the order passed in the message
        // actualOrder is the java object that represents this order inside the current manager
        Order localOrder = this.orderController.getOrder(remoteOrder.getId()
        );
        updateLocalOrder(localOrder, remoteOrder, event);
        switch (event) {
            case INSTANCE_FULFILLED:
                OrderStateTransitioner.transition(localOrder, OrderState.FULFILLED);
                break;
            case INSTANCE_FAILED:
                OrderStateTransitioner.transition(localOrder, OrderState.FAILED);
                break;
        }
    }

    private void updateLocalOrder(Order localOrder, Order remoteOrder, Event event) {
        synchronized (localOrder) {
            if (localOrder.getOrderState() != OrderState.PENDING) {
                // The order has been deleted or already updated
                return;
            }
            localOrder.setInstanceId(remoteOrder.getInstanceId());
            if (event.equals(Event.INSTANCE_FULFILLED)) {
                localOrder.setCachedInstanceState(InstanceState.READY);
            } else if (event.equals(Event.INSTANCE_FAILED)) {
                localOrder.setCachedInstanceState(InstanceState.FAILED);
            }
            if (localOrder.getType().equals(ResourceType.COMPUTE)) {
                ComputeOrder localCompute = (ComputeOrder) localOrder;
                ComputeOrder remoteCompute = (ComputeOrder) remoteOrder;
                localCompute.setActualAllocation(remoteCompute.getActualAllocation());
            }
        }
    }

    public synchronized void setAaController(AaController AaController) {
        this.aaController = AaController;
    }

    public synchronized void setOrderController(OrderController orderController) {
        this.orderController = orderController;
    }
}
