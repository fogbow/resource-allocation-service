package org.fogbowcloud.manager.core;

import java.util.ArrayList;
import java.util.List;
import org.fogbowcloud.manager.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import org.fogbowcloud.manager.core.manager.constants.Operation;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.NetworkOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.orders.AttachmentOrder;
import org.fogbowcloud.manager.core.models.orders.VolumeOrder;
import org.fogbowcloud.manager.core.models.orders.instances.AttachmentInstance;
import org.fogbowcloud.manager.core.models.orders.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.orders.instances.Instance;
import org.fogbowcloud.manager.core.models.orders.instances.NetworkInstance;
import org.fogbowcloud.manager.core.models.orders.instances.VolumeInstance;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.fogbowcloud.manager.core.services.AAAController;

public class ApplicationFacade {

    private static ApplicationFacade instance;

    private AAAController aaaController;
    private OrderController orderController;

    private ApplicationFacade() {
    }

    public static ApplicationFacade getInstance() {
        synchronized (ApplicationFacade.class) {
            if (instance == null) {
                instance = new ApplicationFacade();
            }
            return instance;
        }
    }

    public void createCompute(ComputeOrder order, String federationTokenValue)
        throws UnauthenticatedException, UnauthorizedException, OrderManagementException {
        activateOrder(order, federationTokenValue);
    }

    public List<ComputeInstance> getAllComputes(String federationTokenValue)
        throws UnauthorizedException, UnauthenticatedException, RequestException, TokenCreationException, PropertyNotSpecifiedException, InstanceNotFoundException {
        List<ComputeInstance> computeInstances = new ArrayList<ComputeInstance>();

        List<Order> allOrders = getAllOrders(federationTokenValue, OrderType.COMPUTE);
        for (Order order : allOrders) {
            ComputeInstance instance = (ComputeInstance) this.orderController
                .getResourceInstance(order);
            computeInstances.add(instance);
        }
        return computeInstances;
    }

    public ComputeInstance getCompute(String orderId, String federationTokenValue)
        throws UnauthenticatedException, TokenCreationException, RequestException, PropertyNotSpecifiedException, UnauthorizedException, InstanceNotFoundException {
        return (ComputeInstance) getResourceInstance(orderId, federationTokenValue,
            OrderType.COMPUTE);
    }

    public void deleteCompute(String computeId, String federationTokenValue)
        throws OrderManagementException, UnauthorizedException, UnauthenticatedException {
        deleteOrder(computeId, federationTokenValue, OrderType.COMPUTE);
    }

    public void createVolume(VolumeOrder volumeOrder, String federationTokenValue)
        throws OrderManagementException, UnauthorizedException, UnauthenticatedException {
        activateOrder(volumeOrder, federationTokenValue);
    }

    public List<VolumeInstance> getAllVolumes(String federationTokenValue)
        throws UnauthorizedException, UnauthenticatedException, RequestException, TokenCreationException, PropertyNotSpecifiedException, InstanceNotFoundException {
        List<VolumeInstance> volumeInstances = new ArrayList<VolumeInstance>();

        List<Order> allOrders = getAllOrders(federationTokenValue, OrderType.VOLUME);
        for (Order order : allOrders) {
            VolumeInstance instance = (VolumeInstance) this.orderController
                .getResourceInstance(order);
            volumeInstances.add(instance);
        }
        return volumeInstances;
    }

    public VolumeInstance getVolume(String orderId, String federationTokenValue)
        throws UnauthenticatedException, TokenCreationException, RequestException, PropertyNotSpecifiedException, UnauthorizedException, InstanceNotFoundException {
        return (VolumeInstance) getResourceInstance(orderId, federationTokenValue,
            OrderType.VOLUME);
    }

    public void deleteVolume(String orderId, String federationTokenValue)
        throws OrderManagementException, UnauthorizedException, UnauthenticatedException {
        deleteOrder(orderId, federationTokenValue, OrderType.VOLUME);
    }

    public void createNetwork(NetworkOrder networkOrder, String federationTokenValue)
        throws OrderManagementException, UnauthorizedException, UnauthenticatedException {
        activateOrder(networkOrder, federationTokenValue);
    }

    public List<NetworkInstance> getAllNetworks(String federationTokenValue)
        throws UnauthorizedException, UnauthenticatedException, RequestException, TokenCreationException, PropertyNotSpecifiedException, InstanceNotFoundException {
        List<NetworkInstance> networkInstances = new ArrayList<NetworkInstance>();

        List<Order> allOrders = getAllOrders(federationTokenValue, OrderType.NETWORK);
        for (Order order : allOrders) {
            NetworkInstance instance = (NetworkInstance) this.orderController
                .getResourceInstance(order);
            networkInstances.add(instance);
        }
        return networkInstances;
    }

    public NetworkInstance getNetwork(String orderId, String federationTokenValue)
        throws UnauthenticatedException, TokenCreationException, RequestException, PropertyNotSpecifiedException, UnauthorizedException, InstanceNotFoundException {
        return (NetworkInstance) getResourceInstance(orderId, federationTokenValue,
            OrderType.NETWORK);
    }

    public void deleteNetwork(String orderId, String federationTokenValue)
        throws OrderManagementException, UnauthorizedException, UnauthenticatedException {
        deleteOrder(orderId, federationTokenValue, OrderType.NETWORK);
    }

    public void setAAAController(AAAController aaaController) {
        this.aaaController = aaaController;
    }

    public void setOrderController(OrderController orderController) {
        this.orderController = orderController;
    }

    private void activateOrder(Order order, String federationTokenValue)
        throws OrderManagementException, UnauthorizedException, UnauthenticatedException {
        this.aaaController.authenticate(federationTokenValue);
        FederationUser federationUser = this.aaaController.getFederationUser(federationTokenValue);
        this.aaaController.authorize(federationUser, Operation.CREATE, order);

        this.orderController.activateOrder(order, federationUser);
    }

    private void deleteOrder(String orderId, String federationTokenValue, OrderType orderType)
        throws UnauthenticatedException, UnauthorizedException, OrderManagementException {
        this.aaaController.authenticate(federationTokenValue);

        FederationUser federationUser = this.aaaController.getFederationUser(federationTokenValue);
        Order order = this.orderController.getOrder(orderId, federationUser, orderType);
        this.aaaController.authorize(federationUser, Operation.DELETE, order);

        this.orderController.deleteOrder(order);
    }

    private List<Order> getAllOrders(String federationTokenValue, OrderType orderType)
        throws UnauthorizedException, UnauthenticatedException {
        this.aaaController.authenticate(federationTokenValue);
        FederationUser federationUser = this.aaaController.getFederationUser(federationTokenValue);
        this.aaaController.authorize(federationUser, Operation.GET_ALL, orderType);

        return this.orderController.getAllOrders(federationUser, orderType);
    }

    private Instance getResourceInstance(String orderId, String federationTokenValue,
        OrderType type)
        throws UnauthenticatedException, UnauthorizedException, RequestException, TokenCreationException, PropertyNotSpecifiedException, InstanceNotFoundException {
        this.aaaController.authenticate(federationTokenValue);

        FederationUser federationUser = this.aaaController.getFederationUser(federationTokenValue);
        Order order = this.orderController.getOrder(orderId, federationUser, type);
        this.aaaController.authorize(federationUser, Operation.GET, order);

        return this.orderController.getResourceInstance(order);
    }

    public void createAttachment(AttachmentOrder attachmentOrder,
            String federationTokenValue) throws OrderManagementException, UnauthorizedException, UnauthenticatedException {
        activateOrder(attachmentOrder, federationTokenValue, OrderType.ATTACHMENT);
    }

    public List<AttachmentInstance> getAllAttachments(String federationTokenValue) throws UnauthenticatedException, UnauthorizedException, PropertyNotSpecifiedException, TokenCreationException, RequestException, InstanceNotFoundException {
    	List<AttachmentInstance> attachmentInstances = new ArrayList<AttachmentInstance>();

        List<Order> allOrders = getAllOrders(federationTokenValue, OrderType.ATTACHMENT);
        for (Order order : allOrders) {
            AttachmentInstance instance = (AttachmentInstance) this.orderController
                .getResourceInstance(order);
            attachmentInstances.add(instance);
        }
        return attachmentInstances;
    }

    public AttachmentInstance getAttachment(String orderId,
            String federationTokenValue) throws UnauthenticatedException, UnauthorizedException, RequestException, TokenCreationException, PropertyNotSpecifiedException, InstanceNotFoundException {
    	return (AttachmentInstance) getResourceInstance(orderId, federationTokenValue,
                OrderType.ATTACHMENT);
    }

    public void deleteAttachment(String orderId, String federationTokenValue) throws UnauthenticatedException, UnauthorizedException, OrderManagementException {
    	deleteOrder(orderId, federationTokenValue, OrderType.ATTACHMENT);        
    }

}
