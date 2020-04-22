package cloud.fogbow.ras.core;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import cloud.fogbow.ras.api.http.response.quotas.allocation.*;
import org.apache.log4j.Logger;

import cloud.fogbow.as.core.util.AuthenticationUtil;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.RemoteCommunicationException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
import cloud.fogbow.common.util.CryptoUtil;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.ServiceAsymmetricKeysHolder;
import cloud.fogbow.ras.api.http.response.AttachmentInstance;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.api.http.response.ImageSummary;
import cloud.fogbow.ras.api.http.response.Instance;
import cloud.fogbow.ras.api.http.response.InstanceStatus;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.http.response.VolumeInstance;
import cloud.fogbow.ras.api.http.response.quotas.Quota;
import cloud.fogbow.ras.api.http.response.quotas.ResourceQuota;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.cloudconnector.CloudConnector;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.intercomponent.xmpp.requesters.RemoteGetCloudNamesRequest;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.UserData;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;

public class ApplicationFacade {
    
    private static final Logger LOGGER = Logger.getLogger(ApplicationFacade.class);

    private static ApplicationFacade instance;

    private AuthorizationPlugin<RasOperation> authorizationPlugin;
    private OrderController orderController;
    private SecurityRuleController securityRuleController;
    private CloudListController cloudListController;
    private String providerId;
    private RSAPublicKey asPublicKey;
    private String buildNumber;

    private ApplicationFacade() {
        this.providerId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.PROVIDER_ID_KEY);
        this.buildNumber = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.BUILD_NUMBER_KEY,
                ConfigurationPropertyDefaults.BUILD_NUMBER);
    }

    public static ApplicationFacade getInstance() {
        synchronized (ApplicationFacade.class) {
            if (instance == null) {
                instance = new ApplicationFacade();
            }
            return instance;
        }
    }

    public void setAuthorizationPlugin(AuthorizationPlugin<RasOperation> authorizationPlugin) {
        this.authorizationPlugin = authorizationPlugin;
    }

    public synchronized void setOrderController(OrderController orderController) {
        this.orderController = orderController;
    }

    public synchronized void setSecurityRuleController(SecurityRuleController securityRuleController) {
        this.securityRuleController = securityRuleController;
    }

    public void setCloudListController(CloudListController cloudListController) {
        this.cloudListController = cloudListController;
    }

    public String getVersionNumber() {
        return SystemConstants.API_VERSION_NUMBER + "-" + this.buildNumber;
    }

    public String getPublicKey() throws UnexpectedException {
        // There is no need to authenticate the user or authorize this operation
        try {
            return CryptoUtil.toBase64(ServiceAsymmetricKeysHolder.getInstance().getPublicKey());
        } catch (IOException | GeneralSecurityException e) {
            throw new UnexpectedException(e.getMessage(), e);
        }
    }

    public List<String> getCloudNames(String providerId, String userToken) throws FogbowException {
        SystemUser requester = authenticate(userToken);
        RasOperation rasOperation = new RasOperation(Operation.GET, ResourceType.CLOUD_NAMES);
        this.authorizationPlugin.isAuthorized(requester, rasOperation);
        if (providerId.equals(this.providerId)) {
            return this.cloudListController.getCloudNames();
        } else {
            try {
                RemoteGetCloudNamesRequest remoteGetCloudNames = getCloudNamesFromRemoteRequest(providerId, requester);
                List<String> cloudNames = remoteGetCloudNames.send();
                return cloudNames;
            } catch (Throwable e) {
                LOGGER.error(e.toString(), e);
                throw new RemoteCommunicationException(e.getMessage(), e);
            }
        }
    }

    public String createCompute(ComputeOrder order, String userToken) throws FogbowException {
        // if userData is null we need to prevent a NullPointerException when trying to save the order
        // in the database
        if (order.getUserData() == null) {
            order.setUserData(new ArrayList<>());
        } else {
            for (UserData userDataScript : order.getUserData()) {
                if (userDataScript != null && userDataScript.getExtraUserDataFileContent() != null &&
                    userDataScript.getExtraUserDataFileContent().length() > UserData.MAX_EXTRA_USER_DATA_FILE_CONTENT) {
                    throw new InvalidParameterException(Messages.Exception.TOO_BIG_USER_DATA_FILE_CONTENT);
                }
            }
        }
        return activateOrder(order, userToken);
    }

    public ComputeInstance getCompute(String orderId, String userToken) throws FogbowException {
        return (ComputeInstance) getResourceInstance(orderId, userToken, ResourceType.COMPUTE);
    }

    public void deleteCompute(String orderId, String userToken) throws FogbowException {
        deleteOrder(orderId, userToken, ResourceType.COMPUTE);
    }

    public ComputeAllocation getComputeAllocation(String providerId, String cloudName, String userToken)
            throws FogbowException {
        return (ComputeAllocation) getUserAllocation(providerId, cloudName, userToken, ResourceType.COMPUTE);
    }

    public VolumeAllocation getVolumeAllocation(String providerId, String cloudName, String userToken)
            throws FogbowException {
        return (VolumeAllocation) getUserAllocation(providerId, cloudName, userToken, ResourceType.VOLUME);
    }

    public NetworkAllocation getNetworkAllocation(String providerId, String cloudName, String userToken)
            throws FogbowException {
        return (NetworkAllocation) getUserAllocation(providerId, cloudName, userToken, ResourceType.NETWORK);
    }

    public PublicIpAllocation getPublicIpAllocation(String providerId, String cloudName, String userToken)
            throws FogbowException {
        return (PublicIpAllocation) getUserAllocation(providerId, cloudName, userToken, ResourceType.PUBLIC_IP);
    }

    public ResourceQuota getResourceQuota(String providerId, String cloudName, String userToken)
            throws FogbowException {
        return (ResourceQuota) getUserQuota(providerId, cloudName, userToken);
    }

    public String createVolume(VolumeOrder volumeOrder, String userToken) throws FogbowException {
        return activateOrder(volumeOrder, userToken);
    }

    public VolumeInstance getVolume(String orderId, String userToken) throws FogbowException {
        return (VolumeInstance) getResourceInstance(orderId, userToken, ResourceType.VOLUME);
    }

    public void deleteVolume(String orderId, String userToken) throws FogbowException {
        deleteOrder(orderId, userToken, ResourceType.VOLUME);
    }

    public String createNetwork(NetworkOrder networkOrder, String userToken) throws FogbowException {
        return activateOrder(networkOrder, userToken);
    }

    public NetworkInstance getNetwork(String orderId, String userToken) throws FogbowException {
        return (NetworkInstance) getResourceInstance(orderId, userToken, ResourceType.NETWORK);
    }

    public void deleteNetwork(String orderId, String userToken) throws FogbowException {
        deleteOrder(orderId, userToken, ResourceType.NETWORK);
    }

    public String createAttachment(AttachmentOrder attachmentOrder, String userToken) throws FogbowException {
        return activateOrder(attachmentOrder, userToken);
    }

    public AttachmentInstance getAttachment(String orderId, String userToken) throws FogbowException {
        return (AttachmentInstance) getResourceInstance(orderId, userToken, ResourceType.ATTACHMENT);
    }

    public void deleteAttachment(String orderId, String userToken) throws FogbowException {
        deleteOrder(orderId, userToken, ResourceType.ATTACHMENT);
    }

    public String createPublicIp(PublicIpOrder publicIpOrder, String userToken) throws FogbowException {
        return activateOrder(publicIpOrder, userToken);
    }

    public PublicIpInstance getPublicIp(String publicIpOrderId, String userToken) throws FogbowException {
        return (PublicIpInstance) getResourceInstance(publicIpOrderId, userToken, ResourceType.PUBLIC_IP);
    }

    public void deletePublicIp(String publicIpOrderId, String userToken) throws FogbowException {
        deleteOrder(publicIpOrderId, userToken, ResourceType.PUBLIC_IP);
    }

    public List<InstanceStatus> getAllInstancesStatus(String userToken, ResourceType resourceType)
            throws FogbowException {
        
        SystemUser requester = authenticate(userToken);
        RasOperation rasOperation = new RasOperation(Operation.GET_ALL, resourceType);
        this.authorizationPlugin.isAuthorized(requester, rasOperation);
        return this.orderController.getInstancesStatus(requester, resourceType);
    }

    public List<ImageSummary> getAllImages(String providerId, String cloudName, String userToken)
            throws FogbowException {

        SystemUser requester = authenticate(userToken);
        if (cloudName == null || cloudName.isEmpty()) {
            cloudName = this.cloudListController.getDefaultCloudName();
        }
        RasOperation rasOperation = new RasOperation(Operation.GET_ALL, ResourceType.IMAGE, cloudName);
        this.authorizationPlugin.isAuthorized(requester, rasOperation);
        if (providerId == null) {
            providerId = this.providerId;
        }
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(providerId, cloudName);
        return cloudConnector.getAllImages(requester);
    }

    public ImageInstance getImage(String providerId, String cloudName, String imageId, String userToken)
            throws FogbowException {

        SystemUser requester = authenticate(userToken);
        if (cloudName == null || cloudName.isEmpty()) {
            cloudName = this.cloudListController.getDefaultCloudName();
        }
        RasOperation rasOperation = new RasOperation(Operation.GET, ResourceType.IMAGE, cloudName);
        this.authorizationPlugin.isAuthorized(requester, rasOperation);
        if (providerId == null) {
            providerId = this.providerId;
        }
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(providerId, cloudName);
        return cloudConnector.getImage(imageId, requester);
    }

    public String createSecurityRule(String orderId, SecurityRule securityRule, String userToken,
            ResourceType resourceTypeFromEndpoint) throws FogbowException {

        Order order = this.orderController.getOrder(orderId);
        if (order.getType() != resourceTypeFromEndpoint) {
            throw new InstanceNotFoundException(
                    String.format(Messages.Exception.RESOURCE_TYPE_NOT_COMPATIBLE_S, resourceTypeFromEndpoint));
        }
        SystemUser requester = authenticate(userToken);
        String cloudName = order.getCloudName();
        RasOperation rasOperation = new RasOperation(Operation.CREATE, ResourceType.SECURITY_RULE, cloudName, order);
        this.authorizationPlugin.isAuthorized(requester, rasOperation);
        return this.securityRuleController.createSecurityRule(order, securityRule, requester);
    }

    public List<SecurityRuleInstance> getAllSecurityRules(String orderId, String userToken,
            ResourceType resourceTypeFromEndpoint) throws FogbowException {

        Order order = orderController.getOrder(orderId);
        if (order.getType() != resourceTypeFromEndpoint) {
            throw new InstanceNotFoundException(
                    String.format(Messages.Exception.RESOURCE_TYPE_NOT_COMPATIBLE_S, resourceTypeFromEndpoint));
        }
        SystemUser requester = authenticate(userToken);
        String cloudName = order.getCloudName();
        RasOperation rasOperation = new RasOperation(Operation.GET_ALL, ResourceType.SECURITY_RULE, cloudName, order);
        this.authorizationPlugin.isAuthorized(requester, rasOperation);
        return this.securityRuleController.getAllSecurityRules(order, requester);
    }

    public void deleteSecurityRule(String orderId, String securityRuleId, String userToken,
            ResourceType resourceTypeFromEndpoint) throws FogbowException {

        Order order = orderController.getOrder(orderId);
        if (order.getType() != resourceTypeFromEndpoint) {
            throw new InstanceNotFoundException(
                    String.format(Messages.Exception.RESOURCE_TYPE_NOT_COMPATIBLE_S, resourceTypeFromEndpoint));
        }
        SystemUser requester = authenticate(userToken);
        String cloudName = order.getCloudName();
        RasOperation rasOperation = new RasOperation(Operation.DELETE, ResourceType.SECURITY_RULE, cloudName, order);
        this.authorizationPlugin.isAuthorized(requester, rasOperation);
        this.securityRuleController.deleteSecurityRule(order.getProvider(), cloudName, securityRuleId, requester);
    }

    // These methods are protected to be used in testing
    
    protected RemoteGetCloudNamesRequest getCloudNamesFromRemoteRequest(String providerId, SystemUser requester) {
        RemoteGetCloudNamesRequest remoteGetCloudNames = new RemoteGetCloudNamesRequest(providerId, requester);
        return remoteGetCloudNames;
    }
    
    protected SystemUser authenticate(String userToken) throws FogbowException {
        RSAPublicKey keyRSA = getAsPublicKey();
        return AuthenticationUtil.authenticate(keyRSA, userToken);
    }
    
    protected String activateOrder(Order order, String userToken) throws FogbowException {
        // Check if the user is authentic
        SystemUser requester = authenticate(userToken);
        // Set requester field in the order
        order.setSystemUser(requester);
        // Check consistency of orders that have other orders embedded (eg. an AttachmentOrder embeds
        // both a ComputeOrder and a VolumeOrder).
        checkEmbeddedOrdersConsistency(order);
        // Check if the authenticated user is authorized to perform the requested operation
        this.authorizationPlugin.isAuthorized(requester, new RasOperation(Operation.CREATE,
                order.getType(), order.getCloudName(), order));
        // Add order to the poll of active orders and to the OPEN linked list
        return this.orderController.activateOrder(order);
    }

    protected Instance getResourceInstance(String orderId, String userToken, ResourceType resourceType) throws FogbowException {
        SystemUser requester = authenticate(userToken);
        Order order = this.orderController.getOrder(orderId);
        authorizeOrder(requester, order.getCloudName(), Operation.GET, resourceType, order);
        return this.orderController.getResourceInstance(order);
    }

    protected void deleteOrder(String orderId, String userToken, ResourceType resourceType) throws FogbowException {
        SystemUser requester = authenticate(userToken);
        Order order = this.orderController.getOrder(orderId);
        authorizeOrder(requester, order.getCloudName(), Operation.DELETE, resourceType, order);
        this.orderController.deleteOrder(order);
    }

    protected Allocation getUserAllocation(String providerId, String cloudName, String userToken,
            ResourceType resourceType) throws FogbowException {

        SystemUser requester = authenticate(userToken);
        if (cloudName == null || cloudName.isEmpty())
            cloudName = this.cloudListController.getDefaultCloudName();
        RasOperation rasOperation = new RasOperation(Operation.GET_USER_ALLOCATION, resourceType, cloudName);
        this.authorizationPlugin.isAuthorized(requester, rasOperation);
        return this.orderController.getUserAllocation(providerId, cloudName, requester, resourceType);
    }

    protected Quota getUserQuota(String providerId, String cloudName, String userToken)
            throws FogbowException {
        
        SystemUser requester = authenticate(userToken);
        if (cloudName == null || cloudName.isEmpty())
            cloudName = this.cloudListController.getDefaultCloudName();
        RasOperation rasOperation = new RasOperation(Operation.GET_USER_QUOTA, cloudName);
        this.authorizationPlugin.isAuthorized(requester, rasOperation);
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(providerId, cloudName);
        return cloudConnector.getUserQuota(requester);
    }

    protected void authorizeOrder(SystemUser requester, String cloudName, Operation operation, ResourceType type,
            Order order) throws UnexpectedException, UnauthorizedRequestException, InstanceNotFoundException {
        // Check if requested type matches order type
        if (!order.getType().equals(type))
            throw new InstanceNotFoundException(Messages.Exception.MISMATCHING_RESOURCE_TYPE);
        // Check whether requester owns order
        SystemUser orderOwner = order.getSystemUser();
        if (!orderOwner.equals(requester)) {
            throw new UnauthorizedRequestException(Messages.Exception.REQUESTER_DOES_NOT_OWN_REQUEST);
        }
        this.authorizationPlugin.isAuthorized(requester, new RasOperation(operation, type, cloudName, order));
    }

    protected RSAPublicKey getAsPublicKey() throws FogbowException {
        if (this.asPublicKey == null) {
            this.asPublicKey = RasPublicKeysHolder.getInstance().getAsPublicKey();
        }
        return this.asPublicKey;
    }

    protected void checkEmbeddedOrdersConsistency(Order order) throws InvalidParameterException, UnexpectedException {
        // Orders that embed other orders (compute, attachment and publicip) need to check the consistency
        // of these orders when the order is being dispatched by the LocalCloudConnector.
        switch (order.getType()) {
            case COMPUTE:
                checkComputeOrderConsistency((ComputeOrder) order);
                break;
            case ATTACHMENT:
                checkAttachmentOrderConsistency((AttachmentOrder) order);
                break;
            case PUBLIC_IP:
                checkPublicIpOrderConsistency((PublicIpOrder) order);
                break;
            case NETWORK:
            case VOLUME:
                break;
            default:
                throw new UnexpectedException(String.format(Messages.Exception.UNSUPPORTED_REQUEST_TYPE, order.getType()));
        }
    }

    protected void checkComputeOrderConsistency(ComputeOrder computeOrder) throws InvalidParameterException {
        List<NetworkOrder> networkOrders = getNetworkOrders(computeOrder.getNetworkOrderIds());
        for (NetworkOrder networkOrder : networkOrders) {
            checkConsistencyOfEmbeddedOrder(computeOrder, networkOrder);
        }
    }

    protected void checkAttachmentOrderConsistency(AttachmentOrder attachmentOrder) throws InvalidParameterException {
        String attachComputeOrderId = attachmentOrder.getComputeOrderId();
        String attachVolumeOrderId = attachmentOrder.getVolumeOrderId();
        Order computeOrder = SharedOrderHolders.getInstance().getActiveOrdersMap().get(attachComputeOrderId);
        Order volumeOrder = SharedOrderHolders.getInstance().getActiveOrdersMap().get(attachVolumeOrderId);
        checkConsistencyOfEmbeddedOrder(attachmentOrder, computeOrder);
        checkConsistencyOfEmbeddedOrder(attachmentOrder, volumeOrder);
    }

    protected void checkPublicIpOrderConsistency(PublicIpOrder publicIpOrder) throws InvalidParameterException {
        String computeOrderId = publicIpOrder.getComputeOrderId();
        Order computeOrder = SharedOrderHolders.getInstance().getActiveOrdersMap().get(computeOrderId);
        checkConsistencyOfEmbeddedOrder(publicIpOrder, computeOrder);
    }

    protected void checkConsistencyOfEmbeddedOrder(Order mainOrder, Order embeddedOrder) throws InvalidParameterException {
        if (embeddedOrder == null) {
            throw new InvalidParameterException(Messages.Exception.INVALID_RESOURCE);
        }
        if (!mainOrder.getSystemUser().equals(embeddedOrder.getSystemUser())) {
            throw new InvalidParameterException(Messages.Exception.TRYING_TO_USE_RESOURCES_FROM_ANOTHER_USER);
        }
        if (!mainOrder.getProvider().equals(embeddedOrder.getProvider())) {
            throw new InvalidParameterException(Messages.Exception.PROVIDERS_DONT_MATCH);
        }
        if (!mainOrder.getCloudName().equals(embeddedOrder.getCloudName())) {
            throw new InvalidParameterException(Messages.Exception.CLOUD_NAMES_DONT_MATCH);
        }
        if (embeddedOrder.getProvider().equals(this.providerId) && embeddedOrder.getInstanceId() == null) {
            throw new InvalidParameterException(String.format(Messages.Exception.INSTANCE_NULL_S, embeddedOrder.getId()));
        }
    }

    protected List<NetworkOrder> getNetworkOrders(List<String> networkOrderIds) throws InvalidParameterException {
        List<NetworkOrder> networkOrders = new LinkedList<>();

        for (String orderId : networkOrderIds) {
            Order networkOrder = SharedOrderHolders.getInstance().getActiveOrdersMap().get(orderId);

            if (networkOrder == null) {
                throw new InvalidParameterException(Messages.Exception.INVALID_PARAMETER);
            }
            networkOrders.add((NetworkOrder) networkOrder);
        }
        return networkOrders;
    }

    // used for testing only
    protected void setBuildNumber(String fileName) {
        Properties properties = PropertiesUtil.readProperties(fileName);
        this.buildNumber = properties.getProperty(ConfigurationPropertyKeys.BUILD_NUMBER_KEY,
                ConfigurationPropertyDefaults.BUILD_NUMBER);
    }
}
