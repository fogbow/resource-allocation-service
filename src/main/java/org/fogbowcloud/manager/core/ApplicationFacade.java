package org.fogbowcloud.manager.core;

import java.util.ArrayList;
import java.util.List;

import org.fogbowcloud.manager.api.intercomponent.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.exceptions.QuotaException;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import org.fogbowcloud.manager.core.constants.Operation;
import org.fogbowcloud.manager.core.models.orders.*;
import org.fogbowcloud.manager.core.models.quotas.Quota;
import org.fogbowcloud.manager.core.models.quotas.allocation.Allocation;
import org.fogbowcloud.manager.core.plugins.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.plugins.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.instances.AttachmentInstance;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.instances.Instance;
import org.fogbowcloud.manager.core.models.instances.NetworkInstance;
import org.fogbowcloud.manager.core.models.instances.VolumeInstance;
import org.fogbowcloud.manager.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.manager.core.models.quotas.ComputeQuota;
import org.fogbowcloud.manager.core.models.token.FederationUser;

public class ApplicationFacade {

    private static ApplicationFacade instance;

    private AaController aaController;
    private OrderController orderController;
    private UserQuotaController userQuotaController;

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
        throws UnauthorizedException, UnauthenticatedException, RequestException, TokenCreationException,
            PropertyNotSpecifiedException, InstanceNotFoundException, RemoteRequestException {
        List<ComputeInstance> computeInstances = new ArrayList<ComputeInstance>();

        List<Order> allOrders = getAllOrders(federationTokenValue, InstanceType.COMPUTE);
        for (Order order : allOrders) {
            ComputeInstance instance = (ComputeInstance) this.orderController.getResourceInstance(order);
            computeInstances.add(instance);
        }
        return computeInstances;
    }

    public ComputeInstance getCompute(String orderId, String federationTokenValue)
        throws UnauthenticatedException, TokenCreationException, RequestException, PropertyNotSpecifiedException,
            UnauthorizedException, InstanceNotFoundException, RemoteRequestException {
        return (ComputeInstance) getResourceInstance(orderId, federationTokenValue, InstanceType.COMPUTE);
    }

    public void deleteCompute(String computeId, String federationTokenValue)
        throws OrderManagementException, UnauthorizedException, UnauthenticatedException {
        deleteOrder(computeId, federationTokenValue, InstanceType.COMPUTE);
    }

    public ComputeAllocation getComputeAllocation(String memberId, String federationTokenValue)
            throws UnauthenticatedException, QuotaException, UnauthorizedException, RemoteRequestException,
            RequestException, TokenCreationException, PropertyNotSpecifiedException, InstanceNotFoundException {

        return (ComputeAllocation) getUserAllocation(memberId, federationTokenValue, InstanceType.COMPUTE);
    }

    public ComputeQuota getComputeQuota(String memberId, String federationTokenValue)
            throws UnauthenticatedException, QuotaException, UnauthorizedException, PropertyNotSpecifiedException,
            TokenCreationException, RemoteRequestException {

            return (ComputeQuota) getUserQuota(memberId, federationTokenValue, InstanceType.COMPUTE);
    }

    public void createVolume(VolumeOrder volumeOrder, String federationTokenValue)
        throws OrderManagementException, UnauthorizedException, UnauthenticatedException {
        activateOrder(volumeOrder, federationTokenValue);
    }

    public List<VolumeInstance> getAllVolumes(String federationTokenValue)
        throws UnauthorizedException, UnauthenticatedException, RequestException, TokenCreationException,
            PropertyNotSpecifiedException, InstanceNotFoundException, RemoteRequestException {
        List<VolumeInstance> volumeInstances = new ArrayList<VolumeInstance>();

        List<Order> allOrders = getAllOrders(federationTokenValue, InstanceType.VOLUME);
        for (Order order : allOrders) {
            VolumeInstance instance = (VolumeInstance) this.orderController.getResourceInstance(order);
            volumeInstances.add(instance);
        }
        return volumeInstances;
    }

    public VolumeInstance getVolume(String orderId, String federationTokenValue)
        throws UnauthenticatedException, TokenCreationException, RequestException, PropertyNotSpecifiedException,
            UnauthorizedException, InstanceNotFoundException, RemoteRequestException {
        return (VolumeInstance) getResourceInstance(orderId, federationTokenValue, InstanceType.VOLUME);
    }

    public void deleteVolume(String orderId, String federationTokenValue)
        throws OrderManagementException, UnauthorizedException, UnauthenticatedException {
        deleteOrder(orderId, federationTokenValue, InstanceType.VOLUME);
    }

    public void createNetwork(NetworkOrder networkOrder, String federationTokenValue)
        throws OrderManagementException, UnauthorizedException, UnauthenticatedException {
        activateOrder(networkOrder, federationTokenValue);
    }

    public List<NetworkInstance> getAllNetworks(String federationTokenValue)
        throws UnauthorizedException, UnauthenticatedException, RequestException, TokenCreationException,
            PropertyNotSpecifiedException, InstanceNotFoundException, RemoteRequestException {
        List<NetworkInstance> networkInstances = new ArrayList<NetworkInstance>();

        List<Order> allOrders = getAllOrders(federationTokenValue, InstanceType.NETWORK);
        for (Order order : allOrders) {
            NetworkInstance instance = (NetworkInstance) this.orderController.getResourceInstance(order);
            networkInstances.add(instance);
        }
        return networkInstances;
    }

    public NetworkInstance getNetwork(String orderId, String federationTokenValue)
        throws UnauthenticatedException, TokenCreationException, RequestException, PropertyNotSpecifiedException,
            UnauthorizedException, InstanceNotFoundException, RemoteRequestException {
        return (NetworkInstance) getResourceInstance(orderId, federationTokenValue, InstanceType.NETWORK);
    }

    public void deleteNetwork(String orderId, String federationTokenValue)
        throws OrderManagementException, UnauthorizedException, UnauthenticatedException {
        deleteOrder(orderId, federationTokenValue, InstanceType.NETWORK);
    }

    public void createAttachment(AttachmentOrder attachmentOrder, String federationTokenValue) throws
            OrderManagementException, UnauthorizedException, UnauthenticatedException {
        activateOrder(attachmentOrder, federationTokenValue);
    }

    public List<AttachmentInstance> getAllAttachments(String federationTokenValue) throws UnauthenticatedException,
            UnauthorizedException, PropertyNotSpecifiedException, TokenCreationException, RequestException,
            InstanceNotFoundException, RemoteRequestException {
        List<AttachmentInstance> attachmentInstances = new ArrayList<AttachmentInstance>();

        List<Order> allOrders = getAllOrders(federationTokenValue, InstanceType.ATTACHMENT);
        for (Order order : allOrders) {
            AttachmentInstance instance = (AttachmentInstance) this.orderController.getResourceInstance(order);
            attachmentInstances.add(instance);
        }
        return attachmentInstances;
    }

    public AttachmentInstance getAttachment(String orderId, String federationTokenValue) throws
            UnauthenticatedException, UnauthorizedException, RequestException, TokenCreationException,
            PropertyNotSpecifiedException, InstanceNotFoundException, RemoteRequestException {
        return (AttachmentInstance) getResourceInstance(orderId, federationTokenValue, InstanceType.ATTACHMENT);
    }

    public void deleteAttachment(String orderId, String federationTokenValue) throws UnauthenticatedException,
            UnauthorizedException, OrderManagementException {
        deleteOrder(orderId, federationTokenValue, InstanceType.ATTACHMENT);
    }

    public void setAaController(AaController aaController) {
        this.aaController = aaController;
    }

    public void setOrderController(OrderController orderController) {
        this.orderController = orderController;
    }

    public void setUserQuotaController(UserQuotaController userQuotaController) {
        this.userQuotaController = userQuotaController;
    }

    private void activateOrder(Order order, String federationTokenValue)
    			throws OrderManagementException, UnauthorizedException, UnauthenticatedException {

        this.aaController.authenticate(federationTokenValue);
        FederationUser federationUser = this.aaController.getFederationUser(federationTokenValue);
        this.aaController.authorize(federationUser, Operation.CREATE, order);

        order.setFederationUser(federationUser);

        String localMemberId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);

        order.setRequestingMember(localMemberId);
        if (order.getProvidingMember() == null) {
            order.setProvidingMember(localMemberId);
        }
        OrderStateTransitioner.activateOrder(order);
    }

    private void deleteOrder(String orderId, String federationTokenValue, InstanceType instanceType)
    			throws UnauthenticatedException, UnauthorizedException, OrderManagementException {
        this.aaController.authenticate(federationTokenValue);

        FederationUser federationUser = this.aaController.getFederationUser(federationTokenValue);
        Order order = this.orderController.getOrder(orderId, federationUser, instanceType);
        this.aaController.authorize(federationUser, Operation.DELETE, order);

        this.orderController.deleteOrder(order);
    }

    private List<Order> getAllOrders(String federationTokenValue, InstanceType instanceType)
    			throws UnauthorizedException, UnauthenticatedException {
        this.aaController.authenticate(federationTokenValue);
        FederationUser federationUser = this.aaController.getFederationUser(federationTokenValue);
        this.aaController.authorize(federationUser, Operation.GET_ALL, instanceType);

        return this.orderController.getAllOrders(federationUser, instanceType);
    }

    private Instance getResourceInstance(String orderId, String federationTokenValue, InstanceType instanceType)
        throws UnauthenticatedException, UnauthorizedException, RequestException, TokenCreationException,
            PropertyNotSpecifiedException, InstanceNotFoundException, RemoteRequestException {
        this.aaController.authenticate(federationTokenValue);

        FederationUser federationUser = this.aaController.getFederationUser(federationTokenValue);
        Order order = this.orderController.getOrder(orderId, federationUser, instanceType);
        this.aaController.authorize(federationUser, Operation.GET, order);

        return this.orderController.getResourceInstance(order);
    }

    private Allocation getUserAllocation(String memberId, String federationTokenValue, InstanceType instanceType)
            throws UnauthenticatedException, QuotaException, UnauthorizedException, RemoteRequestException,
            RequestException, TokenCreationException, PropertyNotSpecifiedException, InstanceNotFoundException {

        this.aaController.authenticate(federationTokenValue);
        FederationUser federationUser = this.aaController.getFederationUser(federationTokenValue);
        this.aaController.authorize(federationUser, Operation.GET_USER_ALLOCATION, instanceType);

        return this.orderController.getUserAllocation(memberId, federationUser, instanceType);
    }

    private Quota getUserQuota(String memberId, String federationTokenValue, InstanceType instanceType)
            throws UnauthenticatedException, QuotaException, UnauthorizedException, PropertyNotSpecifiedException,
            TokenCreationException, RemoteRequestException {

        this.aaController.authenticate(federationTokenValue);
        FederationUser federationUser = this.aaController.getFederationUser(federationTokenValue);
        this.aaController.authorize(federationUser, Operation.GET_USER_QUOTA, instanceType);

        return this.userQuotaController.getUserQuota(memberId, federationUser, instanceType);
    }
}
