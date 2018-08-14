package org.fogbowcloud.manager.core;

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
import org.fogbowcloud.manager.core.models.tokens.FederationUserToken;

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
        return activateOrder(order, federationTokenValue);
    }

    public ComputeInstance getCompute(String orderId, String federationTokenValue) throws Exception {
        return (ComputeInstance) getResourceInstance(orderId, federationTokenValue, ResourceType.COMPUTE);
    }

    public void deleteCompute(String computeId, String federationTokenValue) throws FogbowManagerException,
            UnexpectedException {
        LOGGER.debug("deleting order " + computeId + federationTokenValue);
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
            UnauthenticatedUserException, UnauthorizedRequestException, UnavailableProviderException,
            InvalidParameterException {
        FederationUserToken requester = this.aaController.getFederationUser(federationTokenValue);
        this.aaController.authenticateAndAuthorize(requester, Operation.GET_ALL, resourceType);
        return this.orderController.getInstancesStatus(requester, resourceType);
    }

    private String activateOrder(Order order, String federationTokenValue) throws FogbowManagerException,
            UnexpectedException {
        FederationUserToken requester = this.aaController.getFederationUser(federationTokenValue);
        this.aaController.authenticateAndAuthorize(requester, Operation.CREATE, order.getType());
        this.orderController.setEmptyFieldsAndActivateOrder(order, requester);
        return order.getId();
    }

    private Instance getResourceInstance(String orderId, String federationTokenValue, ResourceType resourceType)
            throws Exception {
        FederationUserToken requester = this.aaController.getFederationUser(federationTokenValue);
        Order order = this.orderController.getOrder(orderId);
        this.aaController.authenticateAndAuthorize(requester, Operation.GET, resourceType, order);
        return this.orderController.getResourceInstance(orderId);
    }

    private void deleteOrder(String orderId, String federationTokenValue, ResourceType resourceType) throws
            FogbowManagerException, UnexpectedException {
        FederationUserToken requester = this.aaController.getFederationUser(federationTokenValue);
        Order order = this.orderController.getOrder(orderId);
        this.aaController.authenticateAndAuthorize(requester, Operation.DELETE, resourceType, order);
        this.orderController.deleteOrder(orderId);
    }

    private Allocation getUserAllocation(String memberId, String federationTokenValue, ResourceType resourceType)
            throws FogbowManagerException, UnexpectedException {
        FederationUserToken requester = this.aaController.getFederationUser(federationTokenValue);
        this.aaController.authenticateAndAuthorize(requester, Operation.GET_USER_ALLOCATION, resourceType);
        return this.orderController.getUserAllocation(memberId, requester, resourceType);
    }

    private Quota getUserQuota(String memberId, String federationTokenValue, ResourceType resourceType)
            throws Exception {
        FederationUserToken requester = this.aaController.getFederationUser(federationTokenValue);
        this.aaController.authenticateAndAuthorize(requester, Operation.GET_USER_QUOTA, resourceType);
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId);
        return cloudConnector.getUserQuota(requester, resourceType);
    }

    public Map<String, String> getAllImages(String memberId, String federationTokenValue) throws Exception {
        FederationUserToken requester = this.aaController.getFederationUser(federationTokenValue);
        this.aaController.authenticateAndAuthorize(requester, Operation.GET_ALL_IMAGES, ResourceType.IMAGE);
        if(memberId == null) {
            memberId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);
        }
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId);
        return cloudConnector.getAllImages(requester);
    }

    public Image getImage(String memberId, String imageId, String federationTokenValue) throws Exception {
        FederationUserToken requester = this.aaController.getFederationUser(federationTokenValue);
        this.aaController.authenticateAndAuthorize(requester, Operation.GET_IMAGE, ResourceType.IMAGE);
        if(memberId == null) {
            memberId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);
        }
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId);
        return (Image) cloudConnector.getImage(imageId, requester);
    }
}
