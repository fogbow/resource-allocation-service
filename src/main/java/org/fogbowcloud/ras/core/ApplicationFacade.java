package org.fogbowcloud.ras.core;

import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.cloudconnector.CloudConnector;
import org.fogbowcloud.ras.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.constants.Operation;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.UnauthenticatedUserException;
import org.fogbowcloud.ras.core.exceptions.UnauthorizedRequestException;
import org.fogbowcloud.ras.core.exceptions.UnavailableProviderException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.InstanceStatus;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.images.Image;
import org.fogbowcloud.ras.core.models.instances.AttachmentInstance;
import org.fogbowcloud.ras.core.models.instances.ComputeInstance;
import org.fogbowcloud.ras.core.models.instances.Instance;
import org.fogbowcloud.ras.core.models.instances.NetworkInstance;
import org.fogbowcloud.ras.core.models.instances.PublicIpInstance;
import org.fogbowcloud.ras.core.models.instances.VolumeInstance;
import org.fogbowcloud.ras.core.models.orders.*;
import org.fogbowcloud.ras.core.models.quotas.ComputeQuota;
import org.fogbowcloud.ras.core.models.quotas.Quota;
import org.fogbowcloud.ras.core.models.quotas.allocation.Allocation;
import org.fogbowcloud.ras.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;

public class ApplicationFacade {
    private final Logger LOGGER = Logger.getLogger(ApplicationFacade.class);

    private static ApplicationFacade instance;
    public static final String VERSION_NUMBER = "2.1.1";
    private AaaController aaaController;
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

    public synchronized void setAaaController(AaaController aaaController) {
        this.aaaController = aaaController;
    }

    public synchronized void setOrderController(OrderController orderController) {
        this.orderController = orderController;
    }

    public String createCompute(ComputeOrder order, String federationTokenValue) throws FogbowRasException,
            UnexpectedException {
        if (order.getPublicKey() != null && order.getPublicKey().length() > ComputeOrder.MAX_PUBLIC_KEY_SIZE) {
            throw new InvalidParameterException(Messages.Exception.TOO_BIG_PUBLIC_KEY);
        }
        if (order.getUserData() != null && order.getUserData().getExtraUserDataFileContent() != null &&
                order.getUserData().getExtraUserDataFileContent().length() > UserData.MAX_EXTRA_USER_DATA_FILE_CONTENT) {
            throw new InvalidParameterException(Messages.Exception.TOO_BIG_USER_DATA_FILE_CONTENT);
        }
        return activateOrder(order, federationTokenValue);
    }

    public ComputeInstance getCompute(String orderId, String federationTokenValue) throws Exception {
        return (ComputeInstance) getResourceInstance(orderId, federationTokenValue, ResourceType.COMPUTE);
    }

    public void deleteCompute(String computeId, String federationTokenValue) throws FogbowRasException,
            UnexpectedException {
        deleteOrder(computeId, federationTokenValue, ResourceType.COMPUTE);
    }

    public ComputeAllocation getComputeAllocation(String memberId, String federationTokenValue)
            throws FogbowRasException, UnexpectedException {
        return (ComputeAllocation) getUserAllocation(memberId, federationTokenValue, ResourceType.COMPUTE);
    }

    public ComputeQuota getComputeQuota(String memberId, String federationTokenValue) throws Exception {
        return (ComputeQuota) getUserQuota(memberId, federationTokenValue, ResourceType.COMPUTE);
    }

    public String createVolume(VolumeOrder volumeOrder, String federationTokenValue) throws FogbowRasException,
            UnexpectedException {
        return activateOrder(volumeOrder, federationTokenValue);
    }

    public VolumeInstance getVolume(String orderId, String federationTokenValue) throws Exception {
        return (VolumeInstance) getResourceInstance(orderId, federationTokenValue, ResourceType.VOLUME);
    }

    public void deleteVolume(String orderId, String federationTokenValue) throws FogbowRasException,
            UnexpectedException {
        deleteOrder(orderId, federationTokenValue, ResourceType.VOLUME);
    }

    public String createNetwork(NetworkOrder networkOrder, String federationTokenValue) throws FogbowRasException,
            UnexpectedException {
        return activateOrder(networkOrder, federationTokenValue);
    }

    public NetworkInstance getNetwork(String orderId, String federationTokenValue) throws Exception {
        return (NetworkInstance) getResourceInstance(orderId, federationTokenValue, ResourceType.NETWORK);
    }

    public void deleteNetwork(String orderId, String federationTokenValue) throws FogbowRasException,
            UnexpectedException {
        deleteOrder(orderId, federationTokenValue, ResourceType.NETWORK);
    }

    public String createAttachment(AttachmentOrder attachmentOrder, String federationTokenValue) throws
            FogbowRasException, UnexpectedException {
        return activateOrder(attachmentOrder, federationTokenValue);
    }

    public AttachmentInstance getAttachment(String orderId, String federationTokenValue) throws Exception {
        return (AttachmentInstance) getResourceInstance(orderId, federationTokenValue, ResourceType.ATTACHMENT);
    }

    public void deleteAttachment(String orderId, String federationTokenValue) throws FogbowRasException,
            UnexpectedException {
        deleteOrder(orderId, federationTokenValue, ResourceType.ATTACHMENT);
    }

    public List<InstanceStatus> getAllInstancesStatus(String federationTokenValue, ResourceType resourceType) throws
            UnauthenticatedUserException, UnauthorizedRequestException, UnavailableProviderException,
            InvalidParameterException {
        FederationUserToken requester = this.aaaController.getFederationUser(federationTokenValue);
        this.aaaController.authenticateAndAuthorize(requester, Operation.GET_ALL, resourceType);
        return this.orderController.getInstancesStatus(requester, resourceType);
    }

    public Map<String, String> getAllImages(String memberId, String federationTokenValue) throws Exception {
        FederationUserToken requester = this.aaaController.getFederationUser(federationTokenValue);
        this.aaaController.authenticateAndAuthorize(requester, Operation.GET_ALL_IMAGES, ResourceType.IMAGE);
        if (memberId == null) {
            memberId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);
        }
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId);
        return cloudConnector.getAllImages(requester);
    }

    public Image getImage(String memberId, String imageId, String federationTokenValue) throws Exception {
        FederationUserToken requester = this.aaaController.getFederationUser(federationTokenValue);
        this.aaaController.authenticateAndAuthorize(requester, Operation.GET_IMAGE, ResourceType.IMAGE);
        if (memberId == null) {
            memberId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);
        }
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId);
        return cloudConnector.getImage(imageId, requester);
    }

    public String createTokenValue(Map<String, String> userCredentials) throws UnexpectedException,
            FogbowRasException {
        // There is no need to authenticate the user or authorize this operation
        return this.aaaController.createTokenValue(userCredentials);
    }

    public String getVersionNumber() {
        return this.VERSION_NUMBER;
    }

    private String activateOrder(Order order, String federationTokenValue) throws FogbowRasException,
            UnexpectedException {
        FederationUserToken requester = this.aaaController.getFederationUser(federationTokenValue);
        this.aaaController.authenticateAndAuthorize(requester, Operation.CREATE, order.getType());
        this.orderController.setEmptyFieldsAndActivateOrder(order, requester);
        return order.getId();
    }

    private Instance getResourceInstance(String orderId, String federationTokenValue, ResourceType resourceType)
            throws Exception {
        FederationUserToken requester = this.aaaController.getFederationUser(federationTokenValue);
        Order order = this.orderController.getOrder(orderId);
        this.aaaController.authenticateAndAuthorize(requester, Operation.GET, resourceType, order);
        return this.orderController.getResourceInstance(orderId);
    }

    private void deleteOrder(String orderId, String federationTokenValue, ResourceType resourceType) throws
            FogbowRasException, UnexpectedException {
        FederationUserToken requester = this.aaaController.getFederationUser(federationTokenValue);
        Order order = this.orderController.getOrder(orderId);
        this.aaaController.authenticateAndAuthorize(requester, Operation.DELETE, resourceType, order);
        this.orderController.deleteOrder(orderId);
    }

    private Allocation getUserAllocation(String memberId, String federationTokenValue, ResourceType resourceType)
            throws FogbowRasException, UnexpectedException {
        FederationUserToken requester = this.aaaController.getFederationUser(federationTokenValue);
        this.aaaController.authenticateAndAuthorize(requester, Operation.GET_USER_ALLOCATION, resourceType);
        return this.orderController.getUserAllocation(memberId, requester, resourceType);
    }

    private Quota getUserQuota(String memberId, String federationTokenValue, ResourceType resourceType)
            throws Exception {
        FederationUserToken requester = this.aaaController.getFederationUser(federationTokenValue);
        this.aaaController.authenticateAndAuthorize(requester, Operation.GET_USER_QUOTA, resourceType);
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId);
        return cloudConnector.getUserQuota(requester, resourceType);
    }

    public String createPublicIp(PublicIpOrder publicIpOrder, String federationTokenValue)
        throws UnexpectedException, FogbowRasException {
        return activateOrder(publicIpOrder, federationTokenValue);
    }

    public PublicIpInstance getPublicIp(String publicIpOrderId, String federationTokenValue)
        throws Exception {
        return (PublicIpInstance) getResourceInstance(publicIpOrderId, federationTokenValue, ResourceType.PUBLIC_IP);
    }


    public void deletePublicIp(String publicIpOrderId, String federationTokenValue)
        throws UnexpectedException, FogbowRasException {
        deleteOrder(publicIpOrderId, federationTokenValue, ResourceType.PUBLIC_IP);
    }

}
