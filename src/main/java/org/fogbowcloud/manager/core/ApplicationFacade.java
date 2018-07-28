package org.fogbowcloud.manager.core;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnector;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.constants.Operation;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.models.InstanceStatus;
import org.fogbowcloud.manager.core.models.images.Image;
import org.fogbowcloud.manager.core.models.instances.AttachmentInstance;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.instances.Instance;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.instances.NetworkInstance;
import org.fogbowcloud.manager.core.models.instances.VolumeInstance;
import org.fogbowcloud.manager.core.models.orders.AttachmentOrder;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.NetworkOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.VolumeOrder;
import org.fogbowcloud.manager.core.models.quotas.ComputeQuota;
import org.fogbowcloud.manager.core.models.quotas.Quota;
import org.fogbowcloud.manager.core.models.quotas.allocation.Allocation;
import org.fogbowcloud.manager.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;

public class ApplicationFacade {

    private static ApplicationFacade instance;

    private final Logger LOGGER = Logger.getLogger(ApplicationFacade.class);

    private AaController aaController;
    private OrderController orderController;

    private ApplicationFacade() {}

    public static ApplicationFacade getInstance() {
        synchronized (ApplicationFacade.class) {
            if (instance == null) {
                instance = new ApplicationFacade();
            }
            return instance;
        }
    }

    public String createCompute(ComputeOrder order, String federationTokenValue) throws FogbowManagerException,
            UnexpectedException {
        changeNetworkOrderIdsToNetworInstanceIds(order);        
        return activateOrder(order, federationTokenValue);
    }

    /** protected visibility for tests */
    protected void changeNetworkOrderIdsToNetworInstanceIds(ComputeOrder order) {
                
        //as this content came from rest API the IDs are actually NetworkOrderIDs.
        //since we need NetworkInstanceIDs, we need to do proper replacement
        
        List<String> previousNetworkOrdersId = order.getNetworksId();//based on NetworkOrderIDs        
        List<String> newNetworkInstanceIDs = new LinkedList<String>();//based on NetworkInstanceIDs
               
        for (String previousID : previousNetworkOrdersId) {
            
            Order networkOrder = SharedOrderHolders.getInstance().getActiveOrdersMap().get(previousID);
            
            String newInstanceId = networkOrder.getInstanceId();
            newNetworkInstanceIDs.add(newInstanceId);      
        }
        
        //after collecting the list of networkInstaceIDs, we update the ComputeOrder
        order.setNetworksId(newNetworkInstanceIDs);
    }

    public List<ComputeInstance> getAllComputes(String federationTokenValue) throws Exception {
        List<Order> allOrders = getAllOrders(federationTokenValue, InstanceType.COMPUTE);
        return getAllInstances(allOrders, ComputeInstance.class);
    }

    public ComputeInstance getCompute(String orderId, String federationTokenValue) throws Exception {
        ComputeInstance instance = (ComputeInstance)
                getResourceInstance(orderId, federationTokenValue, InstanceType.COMPUTE);
        // The user believes that the order id is actually the instance id.
        // So we need to set the instance id accordingly before returning the instance.
        instance.setId(orderId);
        return instance;
    }

    public void deleteCompute(String computeId, String federationTokenValue) throws FogbowManagerException,
            UnexpectedException {
        deleteOrder(computeId, federationTokenValue, InstanceType.COMPUTE);
    }

    public ComputeAllocation getComputeAllocation(String memberId, String federationTokenValue)
            throws FogbowManagerException, UnexpectedException {
        return (ComputeAllocation) getUserAllocation(memberId, federationTokenValue, InstanceType.COMPUTE);
    }

    public ComputeQuota getComputeQuota(String memberId, String federationTokenValue) throws Exception {
        return (ComputeQuota) getUserQuota(memberId, federationTokenValue, InstanceType.COMPUTE);
    }

    public String createVolume(VolumeOrder volumeOrder, String federationTokenValue) throws FogbowManagerException,
            UnexpectedException {
        return activateOrder(volumeOrder, federationTokenValue);
    }

    public List<VolumeInstance> getAllVolumes(String federationTokenValue) throws Exception {
        List<Order> allOrders = getAllOrders(federationTokenValue, InstanceType.VOLUME);
        return getAllInstances(allOrders, VolumeInstance.class);
    }

    public VolumeInstance getVolume(String orderId, String federationTokenValue) throws Exception {
        VolumeInstance instance = (VolumeInstance)
                getResourceInstance(orderId, federationTokenValue, InstanceType.VOLUME);
        // The user believes that the order id is actually the instance id.
        // So we need to set the instance id accordingly before returning the instance.
        instance.setId(orderId);
        return instance;
    }

    public void deleteVolume(String orderId, String federationTokenValue) throws FogbowManagerException,
            UnexpectedException {
        deleteOrder(orderId, federationTokenValue, InstanceType.VOLUME);
    }

    public String createNetwork(NetworkOrder networkOrder, String federationTokenValue) throws FogbowManagerException,
            UnexpectedException {
        return activateOrder(networkOrder, federationTokenValue);
    }

    public List<NetworkInstance> getAllNetworks(String federationTokenValue) throws Exception {
        List<Order> allOrders = getAllOrders(federationTokenValue, InstanceType.NETWORK);
        return getAllInstances(allOrders, NetworkInstance.class);
    }

    public NetworkInstance getNetwork(String orderId, String federationTokenValue) throws Exception {
        NetworkInstance instance = (NetworkInstance)
                getResourceInstance(orderId, federationTokenValue, InstanceType.NETWORK);
        // The user believes that the order id is actually the instance id.
        // So we need to set the instance id accordingly before returning the instance.
        instance.setId(orderId);
        return instance;
    }

    public void deleteNetwork(String orderId, String federationTokenValue) throws FogbowManagerException,
            UnexpectedException {
        deleteOrder(orderId, federationTokenValue, InstanceType.NETWORK);
    }

    public String createAttachment(AttachmentOrder attachmentOrder, String federationTokenValue) throws
            FogbowManagerException, UnexpectedException {
        // The request to create an attachment sent by the user carries OrderIds, instead of instanceIds, which is what
        // is needed by the attachment plugin to do the attachment in the cloud. Thus, before activating the order, we need
        // to map orderIds into the corresponding instanceIds.
        Order sourceOrder = SharedOrderHolders.getInstance().getActiveOrdersMap().get(attachmentOrder.getSource());
        Order targetOrder = SharedOrderHolders.getInstance().getActiveOrdersMap().get(attachmentOrder.getTarget());
        attachmentOrder.setSource(sourceOrder.getInstanceId());
        attachmentOrder.setTarget(targetOrder.getInstanceId());
        return activateOrder(attachmentOrder, federationTokenValue);
    }

    public List<AttachmentInstance> getAllAttachments(String federationTokenValue) throws
            Exception {
        List<Order> allOrders = getAllOrders(federationTokenValue, InstanceType.ATTACHMENT);
        return getAllInstances(allOrders, AttachmentInstance.class);
    }

    public AttachmentInstance getAttachment(String orderId, String federationTokenValue) throws Exception {
        return (AttachmentInstance) getResourceInstance(orderId, federationTokenValue, InstanceType.ATTACHMENT);
    }

    public void deleteAttachment(String orderId, String federationTokenValue) throws FogbowManagerException,
            UnexpectedException {
        deleteOrder(orderId, federationTokenValue, InstanceType.ATTACHMENT);
    }

    public Map<String, String> getAllImages(String memberId, String federationTokenValue) throws Exception {
        LOGGER.debug("getAllImages: " + memberId + " token: " + federationTokenValue);
        this.aaController.authenticate(federationTokenValue);
        LOGGER.debug("getAllImages: request authenticated");

        if(memberId == null) {
            memberId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);
        }

        FederationUser federationUser = this.aaController.getFederationUser(federationTokenValue);
        this.aaController.authorize(federationUser, Operation.GET_ALL_IMAGES);
        LOGGER.debug("getAllImages: request authorized");

        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId);
        return cloudConnector.getAllImages(federationUser);
    }

    public Image getImage(String memberId, String imageId, String federationTokenValue) throws Exception {

        this.aaController.authenticate(federationTokenValue);

        if(memberId == null) {
            memberId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);
        }

        FederationUser federationUser = this.aaController.getFederationUser(federationTokenValue);
        this.aaController.authorize(federationUser, Operation.GET_IMAGE);

        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId);
        return (Image) cloudConnector.getImage(imageId, federationUser);
    }

    public List<InstanceStatus> getAllInstancesStatus(String federationTokenValue, InstanceType instanceType) throws
            Exception {
        List<Order> allOrders = getAllOrders(federationTokenValue, instanceType);
        return getInstancesStatus(allOrders, instanceType);
    }

    private List<InstanceStatus> getInstancesStatus(List<Order> allOrders, InstanceType instanceType)
            throws UnexpectedException {
        List<InstanceStatus> instanceStatusList = new ArrayList<>();
        for (Order order : allOrders) {
            // The state of the instance can be inferred from the state of the order
            InstanceStatus instanceStatus = new InstanceStatus(order.getId(), order.getProvidingMember(),
                    order.getCachedInstanceState());
            instanceStatusList.add(instanceStatus);
        }
        return instanceStatusList;
    }

    public void setAaController(AaController aaController) {
        this.aaController = aaController;
    }

    public void setOrderController(OrderController orderController) {
        this.orderController = orderController;
    }

    private String activateOrder(Order order, String federationTokenValue) throws FogbowManagerException,
            UnexpectedException {

        LOGGER.info("Normalizing the new compute order request received");
        this.aaController.authenticate(federationTokenValue);
        FederationUser federationUser = this.aaController.getFederationUser(federationTokenValue);
        this.aaController.authorize(federationUser, Operation.CREATE, order);
        order.setId(UUID.randomUUID().toString());
        order.setFederationUser(federationUser);
        order.setCachedInstanceState(InstanceState.DISPATCHED);

        String localMemberId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);

        order.setRequestingMember(localMemberId);
        if (order.getProvidingMember() == null) {
            order.setProvidingMember(localMemberId);
        }
        OrderStateTransitioner.activateOrder(order);
        return order.getId();
    }

    private void deleteOrder(String orderId, String federationTokenValue, InstanceType instanceType) throws
            FogbowManagerException, UnexpectedException {
        this.aaController.authenticate(federationTokenValue);

        FederationUser federationUser = this.aaController.getFederationUser(federationTokenValue);
        Order order = this.orderController.getOrder(orderId, federationUser, instanceType);
        this.aaController.authorize(federationUser, Operation.DELETE, order);

        this.orderController.deleteOrder(order);
    }

    private List<Order> getAllOrders(String federationTokenValue, InstanceType instanceType) 
    		throws UnauthenticatedUserException, InvalidParameterException, UnauthorizedRequestException  {
        this.aaController.authenticate(federationTokenValue);
        FederationUser federationUser = this.aaController.getFederationUser(federationTokenValue);
        this.aaController.authorize(federationUser, Operation.GET_ALL, instanceType);

        return this.orderController.getAllOrders(federationUser, instanceType);
    }

    private Instance getResourceInstance(String orderId, String federationTokenValue, InstanceType instanceType)
            throws Exception {
        this.aaController.authenticate(federationTokenValue);

        FederationUser federationUser = this.aaController.getFederationUser(federationTokenValue);
        Order order = this.orderController.getOrder(orderId, federationUser, instanceType);
        this.aaController.authorize(federationUser, Operation.GET, order);

        return this.orderController.getResourceInstance(order);
    }

    private Allocation getUserAllocation(String memberId, String federationTokenValue, InstanceType instanceType)
            throws FogbowManagerException, UnexpectedException {

        this.aaController.authenticate(federationTokenValue);
        FederationUser federationUser = this.aaController.getFederationUser(federationTokenValue);
        this.aaController.authorize(federationUser, Operation.GET_USER_ALLOCATION, instanceType);

        return this.orderController.getUserAllocation(memberId, federationUser, instanceType);
    }

    private Quota getUserQuota(String memberId, String federationTokenValue, InstanceType instanceType)
            throws Exception {

        this.aaController.authenticate(federationTokenValue);
        FederationUser federationUser = this.aaController.getFederationUser(federationTokenValue);
        this.aaController.authorize(federationUser, Operation.GET_USER_QUOTA, instanceType);

        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId);
        return cloudConnector.getUserQuota(federationUser, instanceType);
    }

    @SuppressWarnings("unchecked")
    private <T extends Instance> List<T> getAllInstances(List<Order> orders, Class<T> tClass) throws Exception {
        List<T> instances = new ArrayList<>();
        for (Order order : orders) {
            Instance instance = this.orderController.getResourceInstance(order);
            // The user believes that the order id is actually the instance id.
            // So we need to set the instance id accordingly before returning the instance.
            instance.setId(order.getId());
            if (tClass.isInstance(order)) {
                instances.add(tClass.cast(instance));
            } else {
                instances.add((T) instance);
            }
        }
        return instances;

    }
}
