package org.fogbowcloud.manager.core;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnector;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.constants.Operation;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.models.InstanceStatus;
import org.fogbowcloud.manager.core.models.images.Image;
import org.fogbowcloud.manager.core.models.instances.*;
import org.fogbowcloud.manager.core.models.ResourceType;
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

    public synchronized void setAaController(AaController aaController) {
        this.aaController = aaController;
    }

    public synchronized void setOrderController(OrderController orderController) {
        this.orderController = orderController;
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

    public ComputeInstance getCompute(String orderId, String federationTokenValue) throws Exception {
        return (ComputeInstance) getResourceInstance(orderId, federationTokenValue, ResourceType.COMPUTE);
    }

    public void deleteCompute(String computeId, String federationTokenValue) throws FogbowManagerException,
            UnexpectedException {
        deleteOrder(computeId, federationTokenValue, ResourceType.COMPUTE);
    }

    public ComputeAllocation getComputeAllocation(String memberId, String federationTokenValue)
            throws FogbowManagerException, UnexpectedException {
        return (ComputeAllocation) getUserAllocation(memberId, federationTokenValue, ResourceType.COMPUTE);
    }

    public ComputeQuota getComputeQuota(String memberId, String federationTokenValue) throws Exception {
        return (ComputeQuota) getUserQuota(memberId, federationTokenValue, ResourceType.COMPUTE);
    }

    public String createVolume(VolumeOrder volumeOrder, String federationTokenValue) throws FogbowManagerException,
            UnexpectedException {
        return activateOrder(volumeOrder, federationTokenValue);
    }

    public VolumeInstance getVolume(String orderId, String federationTokenValue) throws Exception {
        return (VolumeInstance) getResourceInstance(orderId, federationTokenValue, ResourceType.VOLUME);
    }

    public void deleteVolume(String orderId, String federationTokenValue) throws FogbowManagerException,
            UnexpectedException {
        deleteOrder(orderId, federationTokenValue, ResourceType.VOLUME);
    }

    public String createNetwork(NetworkOrder networkOrder, String federationTokenValue) throws FogbowManagerException,
            UnexpectedException {
        return activateOrder(networkOrder, federationTokenValue);
    }

    public NetworkInstance getNetwork(String orderId, String federationTokenValue) throws Exception {
        return (NetworkInstance) getResourceInstance(orderId, federationTokenValue, ResourceType.NETWORK);
    }

    public void deleteNetwork(String orderId, String federationTokenValue) throws FogbowManagerException,
            UnexpectedException {
        deleteOrder(orderId, federationTokenValue, ResourceType.NETWORK);
    }

    public String createAttachment(AttachmentOrder attachmentOrder, String federationTokenValue) throws
            FogbowManagerException, UnexpectedException {
        // The request to create an attachment sent by the user carries OrderIds, instead of instanceIds, which is what
        // is needed by the attachment plugin to do the attachment in the cloud. Thus, before activating the order,
        // we need to map orderIds into the corresponding instanceIds.
        Order sourceOrder = SharedOrderHolders.getInstance().getActiveOrdersMap().get(attachmentOrder.getSource());
        Order targetOrder = SharedOrderHolders.getInstance().getActiveOrdersMap().get(attachmentOrder.getTarget());
        attachmentOrder.setSource(sourceOrder.getInstanceId());
        attachmentOrder.setTarget(targetOrder.getInstanceId());
        return activateOrder(attachmentOrder, federationTokenValue);
    }

    public AttachmentInstance getAttachment(String orderId, String federationTokenValue) throws Exception {
        return (AttachmentInstance) getResourceInstance(orderId, federationTokenValue, ResourceType.ATTACHMENT);
    }

    public void deleteAttachment(String orderId, String federationTokenValue) throws FogbowManagerException,
            UnexpectedException {
        deleteOrder(orderId, federationTokenValue, ResourceType.ATTACHMENT);
    }

    public List<InstanceStatus> getAllInstancesStatus(String federationTokenValue, ResourceType resourceType) throws
            UnauthenticatedUserException, InvalidParameterException, UnauthorizedRequestException {
        FederationUser federationUser = authenticateAndAuthorize(federationTokenValue, Operation.GET_ALL, resourceType);
        return this.orderController.getInstancesStatus(federationUser, resourceType);
    }

    private String activateOrder(Order order, String federationTokenValue) throws FogbowManagerException,
            UnexpectedException {
        FederationUser requester = authenticateAndAuthorize(federationTokenValue, Operation.CREATE, order.getType());
        order.setFederationUser(requester);
        return this.orderController.setAndActivateOrder(order);
    }

    private Instance getResourceInstance(String orderId, String federationTokenValue, ResourceType resourceType)
            throws Exception {
        authenticateAndAuthorize(federationTokenValue, Operation.GET, resourceType, orderId);
        return this.orderController.getResourceInstance(orderId);
    }

    private void deleteOrder(String orderId, String federationTokenValue, ResourceType resourceType) throws
            FogbowManagerException, UnexpectedException {
        authenticateAndAuthorize(federationTokenValue, Operation.DELETE, resourceType, orderId);
        this.orderController.deleteOrder(orderId);
    }

    private Allocation getUserAllocation(String memberId, String federationTokenValue, ResourceType resourceType)
            throws FogbowManagerException, UnexpectedException {
        FederationUser federationUser = authenticateAndAuthorize(federationTokenValue,
                Operation.GET_USER_ALLOCATION, resourceType);
        return this.orderController.getUserAllocation(memberId, federationUser, resourceType);
    }

    private Quota getUserQuota(String memberId, String federationTokenValue, ResourceType resourceType)
            throws Exception {
        FederationUser federationUser = authenticateAndAuthorize(federationTokenValue,
                Operation.GET_USER_QUOTA, resourceType);
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId);
        return cloudConnector.getUserQuota(federationUser, resourceType);
    }

    public Map<String, String> getAllImages(String memberId, String federationTokenValue) throws Exception {
        FederationUser federationUser = authenticateAndAuthorize(federationTokenValue, Operation.GET_ALL_IMAGES,
                ResourceType.IMAGE);
        if(memberId == null) {
            memberId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);
        }
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId);
        return cloudConnector.getAllImages(federationUser);
    }

    public Image getImage(String memberId, String imageId, String federationTokenValue) throws Exception {
        FederationUser federationUser = authenticateAndAuthorize(federationTokenValue, Operation.GET_IMAGE,
                ResourceType.IMAGE);
        if(memberId == null) {
            memberId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);
        }
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId);
        return (Image) cloudConnector.getImage(imageId, federationUser);
    }

    private FederationUser authenticateAndAuthorize(String federationTokenValue, Operation operation, ResourceType type)
            throws UnauthenticatedUserException, UnauthorizedRequestException, InvalidParameterException {
        // Authenticate user based on the token received
        this.aaController.authenticate(federationTokenValue);
        // Get authenticated user attributes from token
        FederationUser requester = this.aaController.getFederationUser(federationTokenValue);
        // Authorize the user based on user's attributes, requested operation and resource type
        this.aaController.authorize(requester, operation, type);
        return requester;
    }

    private FederationUser authenticateAndAuthorize(String federationTokenValue, Operation operation, ResourceType type,
                                          String orderId) throws FogbowManagerException {
        // Check if requested type matches order type
        Order order = this.orderController.getOrder(orderId);
        if (!order.getType().equals(type)) throw new InstanceNotFoundException("Mismatching resource type");
        // Authenticate user and get authorization to perform generic operation on the type of resource
        FederationUser requester = authenticateAndAuthorize(federationTokenValue, operation, type);
        // Check whether requester owns order
        FederationUser orderOwner = order.getFederationUser();
        if (!orderOwner.getId().equals(requester.getId())) {
            throw new UnauthorizedRequestException("Requester does not own order");
        }
        return requester;
    }
}
