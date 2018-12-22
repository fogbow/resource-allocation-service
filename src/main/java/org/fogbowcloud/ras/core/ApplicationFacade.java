package org.fogbowcloud.ras.core;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.cloudconnector.CloudConnector;
import org.fogbowcloud.ras.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.ras.core.cloudconnector.RemoteCloudConnector;
import org.fogbowcloud.ras.core.constants.*;
import org.fogbowcloud.ras.core.exceptions.*;
import org.fogbowcloud.ras.core.intercomponent.xmpp.requesters.RemoteGetCloudNamesRequest;
import org.fogbowcloud.ras.core.models.InstanceStatus;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.images.Image;
import org.fogbowcloud.ras.core.models.instances.*;
import org.fogbowcloud.ras.core.models.orders.*;
import org.fogbowcloud.ras.core.models.quotas.ComputeQuota;
import org.fogbowcloud.ras.core.models.quotas.Quota;
import org.fogbowcloud.ras.core.models.quotas.allocation.Allocation;
import org.fogbowcloud.ras.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.ras.core.models.securityrules.SecurityRule;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.plugins.interoperability.genericrequest.GenericRequest;
import org.fogbowcloud.ras.core.plugins.interoperability.genericrequest.GenericRequestResponse;
import org.fogbowcloud.ras.util.PropertiesUtil;

import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ApplicationFacade {
    private final Logger LOGGER = Logger.getLogger(ApplicationFacade.class);

    private static ApplicationFacade instance;

    private AaaController aaaController;
    private OrderController orderController;
    private SecurityRuleController securityRuleController;
    private CloudListController cloudListController;
    private String memberId;
    private String buildNumber;

    private ApplicationFacade() {
        this.memberId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);
        this.buildNumber = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.BUILD_NUMBER,
                DefaultConfigurationConstants.BUILD_NUMBER);
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

    public synchronized void setSecurityRuleController(SecurityRuleController securityRuleController) {
        this.securityRuleController = securityRuleController;
    }

    public void setCloudListController(CloudListController cloudListController) {
        this.cloudListController = cloudListController;
    }

    public String getVersionNumber() {
        return SystemConstants.API_VERSION_NUMBER + "-" + this.buildNumber;
    }

    public List<String> getCloudNames(String memberId, String federationTokenValue) throws Exception {
        FederationUserToken requester = this.aaaController.getFederationUser(federationTokenValue);
        this.aaaController.authenticateAndAuthorize(this.memberId, requester, Operation.GET_CLOUD_NAMES,
                ResourceType.CLOUD_NAMES);
        if (memberId.equals(this.memberId)) {
            return this.cloudListController.getCloudNames();
        } else {
            RemoteGetCloudNamesRequest remoteGetCloudNames = new RemoteGetCloudNamesRequest(memberId, requester);
            List<String> cloudNames = remoteGetCloudNames.send();
            return cloudNames;
        }
    }

    public String createCompute(ComputeOrder order, String federationTokenValue) throws FogbowRasException,
            UnexpectedException {
        if (order.getPublicKey() != null && order.getPublicKey().length() > ComputeOrder.MAX_PUBLIC_KEY_SIZE) {
            throw new InvalidParameterException(Messages.Exception.TOO_BIG_PUBLIC_KEY);
        }

        if (order.getUserData() != null) {
            for (UserData userDataScript : order.getUserData()) {
                if (userDataScript != null && userDataScript.getExtraUserDataFileContent() != null &&
                    userDataScript.getExtraUserDataFileContent().length() > UserData.MAX_EXTRA_USER_DATA_FILE_CONTENT) {
                    throw new InvalidParameterException(Messages.Exception.TOO_BIG_USER_DATA_FILE_CONTENT);
                }
            }
        }

        return activateOrder(order, federationTokenValue);
    }

    public ComputeInstance getCompute(String orderId, String federationTokenValue) throws Exception {
        return (ComputeInstance) getResourceInstance(orderId, federationTokenValue, ResourceType.COMPUTE);
    }

    public void deleteCompute(String computeId, String federationTokenValue) throws Exception {
        deleteOrder(computeId, federationTokenValue, ResourceType.COMPUTE);
    }

    public ComputeAllocation getComputeAllocation(String memberId, String cloudName, String federationTokenValue)
            throws FogbowRasException, UnexpectedException {
        return (ComputeAllocation) getUserAllocation(memberId, cloudName, federationTokenValue, ResourceType.COMPUTE);
    }

    public ComputeQuota getComputeQuota(String memberId, String cloudName, String federationTokenValue)
            throws Exception {
        return (ComputeQuota) getUserQuota(memberId, cloudName, federationTokenValue, ResourceType.COMPUTE);
    }

    public String createVolume(VolumeOrder volumeOrder, String federationTokenValue) throws FogbowRasException,
            UnexpectedException {
        return activateOrder(volumeOrder, federationTokenValue);
    }

    public VolumeInstance getVolume(String orderId, String federationTokenValue) throws Exception {
        return (VolumeInstance) getResourceInstance(orderId, federationTokenValue, ResourceType.VOLUME);
    }

    public void deleteVolume(String orderId, String federationTokenValue) throws Exception {
        deleteOrder(orderId, federationTokenValue, ResourceType.VOLUME);
    }

    public String createNetwork(NetworkOrder networkOrder, String federationTokenValue) throws FogbowRasException,
            UnexpectedException {
        return activateOrder(networkOrder, federationTokenValue);
    }

    public NetworkInstance getNetwork(String orderId, String federationTokenValue) throws Exception {
        return (NetworkInstance) getResourceInstance(orderId, federationTokenValue, ResourceType.NETWORK);
    }

    public void deleteNetwork(String orderId, String federationTokenValue) throws Exception {
        deleteOrder(orderId, federationTokenValue, ResourceType.NETWORK);
    }

    public String createAttachment(AttachmentOrder attachmentOrder, String federationTokenValue)
            throws FogbowRasException, UnexpectedException {
        return activateOrder(attachmentOrder, federationTokenValue);
    }

    public AttachmentInstance getAttachment(String orderId, String federationTokenValue) throws Exception {
        return (AttachmentInstance) getResourceInstance(orderId, federationTokenValue, ResourceType.ATTACHMENT);
    }

    public void deleteAttachment(String orderId, String federationTokenValue) throws Exception {
        deleteOrder(orderId, federationTokenValue, ResourceType.ATTACHMENT);
    }

    public String createPublicIp(PublicIpOrder publicIpOrder, String federationTokenValue) throws UnexpectedException,
            FogbowRasException {
        return activateOrder(publicIpOrder, federationTokenValue);
    }

    public PublicIpInstance getPublicIp(String publicIpOrderId, String federationTokenValue)
            throws Exception {
        return (PublicIpInstance) getResourceInstance(publicIpOrderId, federationTokenValue, ResourceType.PUBLIC_IP);
    }

    public void deletePublicIp(String publicIpOrderId, String federationTokenValue) throws Exception {
        deleteOrder(publicIpOrderId, federationTokenValue, ResourceType.PUBLIC_IP);
    }

    public List<InstanceStatus> getAllInstancesStatus(String federationTokenValue, ResourceType resourceType) throws
            UnauthenticatedUserException, UnauthorizedRequestException, UnavailableProviderException,
            InvalidParameterException {
        FederationUserToken requester = this.aaaController.getFederationUser(federationTokenValue);
        this.aaaController.authenticateAndAuthorize(this.memberId, requester, Operation.GET_ALL, resourceType);
        return this.orderController.getInstancesStatus(requester, resourceType);
    }

    public Map<String, String> getAllImages(String memberId, String cloudName, String federationTokenValue)
            throws Exception {
        FederationUserToken requester = this.aaaController.getFederationUser(federationTokenValue);
        if (cloudName == null || cloudName.isEmpty()) cloudName = this.cloudListController.getDefaultCloudName();
        this.aaaController.authenticateAndAuthorize(this.memberId, requester, cloudName, Operation.GET_ALL_IMAGES,
                ResourceType.IMAGE);
        if (memberId == null) {
            memberId = this.memberId;
        }
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId, cloudName);
        return cloudConnector.getAllImages(requester);
    }

    public Image getImage(String memberId, String cloudName, String imageId, String federationTokenValue)
            throws Exception {
        FederationUserToken requester = this.aaaController.getFederationUser(federationTokenValue);
        if (cloudName == null || cloudName.isEmpty()) cloudName = this.cloudListController.getDefaultCloudName();
        this.aaaController.authenticateAndAuthorize(this.memberId, requester, cloudName, Operation.GET_IMAGE,
                ResourceType.IMAGE);
        if (memberId == null) {
            memberId = this.memberId;
        }
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId, cloudName);
        return cloudConnector.getImage(imageId, requester);
    }

    public String createTokenValue(Map<String, String> userCredentials) throws UnexpectedException, FogbowRasException {
        // There is no need to authenticate the user or authorize this operation
        return this.aaaController.createTokenValue(userCredentials);
    }

    public String createSecurityRule(String orderId, SecurityRule securityRule,
                                     String federationTokenValue, ResourceType resourceTypeFromEndpoint)
            throws Exception {
        Order order = orderController.getOrder(orderId);
        if (order.getType() != resourceTypeFromEndpoint) {
            throw new InstanceNotFoundException();
        }
        FederationUserToken requester = this.aaaController.getFederationUser(federationTokenValue);
        this.aaaController.authenticateAndAuthorize(this.memberId, requester, order.getCloudName(), Operation.CREATE,
                ResourceType.SECURITY_RULE);
        return securityRuleController.createSecurityRule(order, securityRule, requester);
    }

    public List<SecurityRule> getAllSecurityRules(String orderId, String federationTokenValue,
                                                  ResourceType resourceTypeFromEndpoint) throws Exception {
        Order order = orderController.getOrder(orderId);
        if (order.getType() != resourceTypeFromEndpoint) {
            throw new InstanceNotFoundException();
        }
        FederationUserToken requester = this.aaaController.getFederationUser(federationTokenValue);
        this.aaaController.authenticateAndAuthorize(this.memberId, requester, order.getCloudName(), Operation.GET_ALL,
                ResourceType.SECURITY_RULE);
        return securityRuleController.getAllSecurityRules(order, requester);
    }

    public void deleteSecurityRule(String orderId, String securityRuleId, String federationTokenValue,
                                   ResourceType resourceTypeFromEndpoint) throws Exception {
        Order order = orderController.getOrder(orderId);
        if (order.getType() != resourceTypeFromEndpoint) {
            throw new InstanceNotFoundException();
        }
        FederationUserToken requester = this.aaaController.getFederationUser(federationTokenValue);
        this.aaaController.authenticateAndAuthorize(this.memberId, requester, order.getCloudName(), Operation.DELETE,
                ResourceType.SECURITY_RULE);
        securityRuleController.deleteSecurityRule(securityRuleId, order.getProvider(), requester);
    }

    public GenericRequestResponse genericRequest(String cloudName, String memberId, GenericRequest genericRequest,
                                                 String federationTokenValue) throws Exception {
        FederationUserToken federationUserToken = this.aaaController.getFederationUser(federationTokenValue);
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId, cloudName);
        return cloudConnector.genericRequest(genericRequest, federationUserToken);
    }

    private String activateOrder(Order order, String federationTokenValue) throws FogbowRasException,
            UnexpectedException {
        // Set order fields that have not been provided by the requester in the body of the HTTP request
        order.setRequester(this.memberId);
        if (order.getProvider() == null || order.getProvider().isEmpty()) order.setProvider(this.memberId);
        if (order.getCloudName() == null || order.getCloudName().isEmpty())
            order.setCloudName(this.cloudListController.getDefaultCloudName());
        FederationUserToken requester = this.aaaController.getFederationUser(federationTokenValue);
        order.setFederationUserToken(requester);
        // Check if the user is authentic and authorized to perform the requested operation
        this.aaaController.authenticateAndAuthorize(this.memberId, requester, order.getCloudName(), Operation.CREATE,
                order.getType());
        // Set an initial state for the resource instance that is yet to be created in the cloud
        order.setCachedInstanceState(InstanceState.DISPATCHED);
        // Add order to the poll of active orders and to the OPEN linked list
        OrderStateTransitioner.activateOrder(order);
        return order.getId();
    }

    private Instance getResourceInstance(String orderId, String federationTokenValue, ResourceType resourceType)
            throws Exception {
        FederationUserToken requester = this.aaaController.getFederationUser(federationTokenValue);
        Order order = this.orderController.getOrder(orderId);
        this.aaaController.authenticateAndAuthorize(this.memberId, requester, order.getCloudName(), Operation.GET,
                resourceType, order);
        return this.orderController.getResourceInstance(orderId);
    }

    private void deleteOrder(String orderId, String federationTokenValue, ResourceType resourceType) throws Exception {
        FederationUserToken requester = this.aaaController.getFederationUser(federationTokenValue);
        Order order = this.orderController.getOrder(orderId);
        this.aaaController.authenticateAndAuthorize(this.memberId, requester, order.getCloudName(), Operation.DELETE,
                resourceType, order);
        this.orderController.deleteOrder(orderId);
    }

    private Allocation getUserAllocation(String memberId, String cloudName, String federationTokenValue,
                                         ResourceType resourceType)
            throws FogbowRasException, UnexpectedException {
        FederationUserToken requester = this.aaaController.getFederationUser(federationTokenValue);
        if (cloudName == null || cloudName.isEmpty()) cloudName = this.cloudListController.getDefaultCloudName();
        this.aaaController.authenticateAndAuthorize(this.memberId, requester, cloudName, Operation.GET_USER_ALLOCATION,
                resourceType);
        return this.orderController.getUserAllocation(memberId, requester, resourceType);
    }

    private Quota getUserQuota(String memberId, String cloudName, String federationTokenValue,
                               ResourceType resourceType)
            throws Exception {
        FederationUserToken requester = this.aaaController.getFederationUser(federationTokenValue);
        if (cloudName == null || cloudName.isEmpty()) cloudName = this.cloudListController.getDefaultCloudName();
        this.aaaController.authenticateAndAuthorize(this.memberId, requester, cloudName, Operation.GET_USER_QUOTA,
                resourceType);
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId, cloudName);
        return cloudConnector.getUserQuota(requester, resourceType);
    }

    // Used for testing
    protected void setBuildNumber(String fileName) {
        Properties properties = PropertiesUtil.readProperties(fileName);
        this.buildNumber = properties.getProperty(ConfigurationConstants.BUILD_NUMBER,
                DefaultConfigurationConstants.BUILD_NUMBER);
    }
}
