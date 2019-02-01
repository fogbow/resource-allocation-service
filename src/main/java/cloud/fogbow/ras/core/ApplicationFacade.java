package cloud.fogbow.ras.core;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.common.models.FederationUser;
import cloud.fogbow.common.plugins.authorization.AuthorizationController;
import cloud.fogbow.common.util.AuthenticationUtil;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.RSAUtil;
import cloud.fogbow.common.util.ServiceAsymmetricKeysHolder;
import cloud.fogbow.ras.core.cloudconnector.CloudConnector;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.constants.ConfigurationConstants;
import cloud.fogbow.ras.core.constants.DefaultConfigurationConstants;
import cloud.fogbow.ras.core.constants.Messages;
import cloud.fogbow.ras.core.constants.SystemConstants;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.instances.*;
import cloud.fogbow.ras.core.models.orders.*;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequest;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequestResponse;
import org.apache.log4j.Logger;
import cloud.fogbow.ras.core.intercomponent.xmpp.requesters.RemoteGetCloudNamesRequest;
import cloud.fogbow.ras.core.models.InstanceStatus;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.UserData;
import cloud.fogbow.ras.core.models.images.Image;
import cloud.fogbow.ras.core.models.quotas.ComputeQuota;
import cloud.fogbow.ras.core.models.quotas.Quota;
import cloud.fogbow.ras.core.models.quotas.allocation.Allocation;
import cloud.fogbow.ras.core.models.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.core.models.securityrules.SecurityRule;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ApplicationFacade {
    private final Logger LOGGER = Logger.getLogger(ApplicationFacade.class);

    private static ApplicationFacade instance;

    private AuthorizationController authorizationController;
    private OrderController orderController;
    private SecurityRuleController securityRuleController;
    private CloudListController cloudListController;
    private String memberId;
    private RSAPublicKey asPublicKey;
    private String buildNumber;

    private ApplicationFacade() {
        this.memberId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID_KEY);
        this.buildNumber = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.BUILD_NUMBER_KEY,
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

    public void setAuthorizationController(AuthorizationController authorizationController) {
        this.authorizationController = authorizationController;
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
            return RSAUtil.savePublicKey(ServiceAsymmetricKeysHolder.getInstance().getPublicKey());
        } catch (IOException | GeneralSecurityException e) {
            throw new UnexpectedException(e.getMessage(), e);
        }
    }

    public List<String> getCloudNames(String memberId, String federationTokenValue) throws RemoteCommunicationException,
            UnexpectedException, UnavailableProviderException, UnauthenticatedUserException, InvalidTokenException,
            UnauthorizedRequestException, ConfigurationErrorException {
        FederationUser requester = AuthenticationUtil.authenticate(getAsPublicKey(), federationTokenValue);
        this.authorizationController.authorize(requester, Operation.GET_CLOUD_NAMES.getValue(),
                ResourceType.CLOUD_NAMES.getValue());
        if (memberId.equals(this.memberId)) {
            return this.cloudListController.getCloudNames();
        } else {
            try {
                RemoteGetCloudNamesRequest remoteGetCloudNames = new RemoteGetCloudNamesRequest(memberId, requester);
                List<String> cloudNames = remoteGetCloudNames.send();
                return cloudNames;
            } catch (Exception e) {
                LOGGER.error(e.toString(), e);
                throw new RemoteCommunicationException(e.getMessage(), e);
            }
        }
    }

    public String createCompute(ComputeOrder order, String federationTokenValue) throws FogbowException {
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

    public ComputeInstance getCompute(String orderId, String federationTokenValue) throws FogbowException {
        return (ComputeInstance) getResourceInstance(orderId, federationTokenValue, ResourceType.COMPUTE);
    }

    public void deleteCompute(String computeId, String federationTokenValue) throws FogbowException {
        deleteOrder(computeId, federationTokenValue, ResourceType.COMPUTE);
    }

    public ComputeAllocation getComputeAllocation(String memberId, String cloudName, String federationTokenValue)
            throws FogbowException {
        return (ComputeAllocation) getUserAllocation(memberId, cloudName, federationTokenValue, ResourceType.COMPUTE);
    }

    public ComputeQuota getComputeQuota(String memberId, String cloudName, String federationTokenValue)
            throws FogbowException {
        return (ComputeQuota) getUserQuota(memberId, cloudName, federationTokenValue, ResourceType.COMPUTE);
    }

    public String createVolume(VolumeOrder volumeOrder, String federationTokenValue) throws FogbowException {
        return activateOrder(volumeOrder, federationTokenValue);
    }

    public VolumeInstance getVolume(String orderId, String federationTokenValue) throws FogbowException {
        return (VolumeInstance) getResourceInstance(orderId, federationTokenValue, ResourceType.VOLUME);
    }

    public void deleteVolume(String orderId, String federationTokenValue) throws FogbowException {
        deleteOrder(orderId, federationTokenValue, ResourceType.VOLUME);
    }

    public String createNetwork(NetworkOrder networkOrder, String federationTokenValue) throws FogbowException {
        return activateOrder(networkOrder, federationTokenValue);
    }

    public NetworkInstance getNetwork(String orderId, String federationTokenValue) throws FogbowException {
        return (NetworkInstance) getResourceInstance(orderId, federationTokenValue, ResourceType.NETWORK);
    }

    public void deleteNetwork(String orderId, String federationTokenValue) throws FogbowException {
        deleteOrder(orderId, federationTokenValue, ResourceType.NETWORK);
    }

    public String createAttachment(AttachmentOrder attachmentOrder, String federationTokenValue) throws FogbowException {
        return activateOrder(attachmentOrder, federationTokenValue);
    }

    public AttachmentInstance getAttachment(String orderId, String federationTokenValue) throws FogbowException {
        return (AttachmentInstance) getResourceInstance(orderId, federationTokenValue, ResourceType.ATTACHMENT);
    }

    public void deleteAttachment(String orderId, String federationTokenValue) throws FogbowException {
        deleteOrder(orderId, federationTokenValue, ResourceType.ATTACHMENT);
    }

    public String createPublicIp(PublicIpOrder publicIpOrder, String federationTokenValue) throws FogbowException {
        return activateOrder(publicIpOrder, federationTokenValue);
    }

    public PublicIpInstance getPublicIp(String publicIpOrderId, String federationTokenValue) throws FogbowException {
        return (PublicIpInstance) getResourceInstance(publicIpOrderId, federationTokenValue, ResourceType.PUBLIC_IP);
    }

    public void deletePublicIp(String publicIpOrderId, String federationTokenValue) throws FogbowException {
        deleteOrder(publicIpOrderId, federationTokenValue, ResourceType.PUBLIC_IP);
    }

    public List<InstanceStatus> getAllInstancesStatus(String federationTokenValue, ResourceType resourceType)
            throws UnexpectedException, UnauthorizedRequestException, UnavailableProviderException,
            UnauthenticatedUserException, InvalidTokenException, ConfigurationErrorException {
        FederationUser requester = AuthenticationUtil.authenticate(getAsPublicKey(), federationTokenValue);
        this.authorizationController.authorize(requester, Operation.GET_ALL.getValue(), resourceType.getValue());
        return this.orderController.getInstancesStatus(requester, resourceType);
    }

    public Map<String, String> getAllImages(String memberId, String cloudName, String federationTokenValue)
            throws FogbowException {
        FederationUser requester = AuthenticationUtil.authenticate(getAsPublicKey(), federationTokenValue);
        if (cloudName == null || cloudName.isEmpty()) cloudName = this.cloudListController.getDefaultCloudName();
        this.authorizationController.authorize(requester, cloudName, Operation.GET_ALL_IMAGES.getValue(), ResourceType.IMAGE.getValue());
        if (memberId == null) {
            memberId = this.memberId;
        }
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId, cloudName);
        return cloudConnector.getAllImages(requester);
    }

    public Image getImage(String memberId, String cloudName, String imageId, String federationTokenValue)
            throws FogbowException {
        FederationUser requester = AuthenticationUtil.authenticate(getAsPublicKey(), federationTokenValue);
        if (cloudName == null || cloudName.isEmpty()) cloudName = this.cloudListController.getDefaultCloudName();
        this.authorizationController.authorize(requester, cloudName, Operation.GET_ALL.getValue(), ResourceType.IMAGE.getValue());
        if (memberId == null) {
            memberId = this.memberId;
        }
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId, cloudName);
        return cloudConnector.getImage(imageId, requester);
    }

    public String createSecurityRule(String orderId, SecurityRule securityRule,
                                     String federationTokenValue, ResourceType resourceTypeFromEndpoint)
                                     throws FogbowException {
        Order order = orderController.getOrder(orderId);
        if (order.getType() != resourceTypeFromEndpoint) {
            throw new InstanceNotFoundException();
        }

        FederationUser requester = AuthenticationUtil.authenticate(getAsPublicKey(), federationTokenValue);
        this.authorizationController.authorize(requester, order.getCloudName(), Operation.CREATE.getValue(),
                ResourceType.SECURITY_RULE.getValue());
        return securityRuleController.createSecurityRule(order, securityRule, requester);
    }

    public List<SecurityRule> getAllSecurityRules(String orderId, String federationTokenValue,
                      ResourceType resourceTypeFromEndpoint) throws FogbowException {
        Order order = orderController.getOrder(orderId);
        if (order.getType() != resourceTypeFromEndpoint) {
            throw new InstanceNotFoundException();
        }
        FederationUser requester = AuthenticationUtil.authenticate(getAsPublicKey(), federationTokenValue);
        this.authorizationController.authorize(requester, order.getCloudName(), Operation.GET_ALL.getValue(),
                ResourceType.SECURITY_RULE.getValue());
        return securityRuleController.getAllSecurityRules(order, requester);
    }

    public void deleteSecurityRule(String orderId, String securityRuleId, String federationTokenValue,
                               ResourceType resourceTypeFromEndpoint) throws FogbowException {
        Order order = orderController.getOrder(orderId);
        if (order.getType() != resourceTypeFromEndpoint) {
            throw new InstanceNotFoundException();
        }
        FederationUser requester = AuthenticationUtil.authenticate(getAsPublicKey(), federationTokenValue);
        this.authorizationController.authorize(requester, order.getCloudName(), Operation.DELETE.getValue(),
                ResourceType.SECURITY_RULE.getValue());
        securityRuleController.deleteSecurityRule(order.getProvider(), order.getCloudName(), securityRuleId, requester);
    }

    public GenericRequestResponse genericRequest(String cloudName, String memberId, GenericRequest genericRequest,
                                                 String federationTokenValue) throws FogbowException {
        FederationUser requester = AuthenticationUtil.authenticate(getAsPublicKey(), federationTokenValue);
        this.authorizationController.authorize(requester, cloudName, Operation.GENERIC_REQUEST.getValue(),
                ResourceType.GENERIC_REQUEST.getValue());
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId, cloudName);
        return cloudConnector.genericRequest(genericRequest, requester);
    }

    private String activateOrder(Order order, String federationTokenValue) throws FogbowException {
        // Set order fields that have not been provided by the requester in the body of the HTTP request
        order.setRequester(this.memberId);
        if (order.getProvider() == null || order.getProvider().isEmpty()) order.setProvider(this.memberId);
        if (order.getCloudName() == null || order.getCloudName().isEmpty())
            order.setCloudName(this.cloudListController.getDefaultCloudName());
        // Check if the user is authentic
        FederationUser requester = AuthenticationUtil.authenticate(getAsPublicKey(), federationTokenValue);
        order.setFederationUser(requester);
        // Check if the authenticated user is authorized to perform the requested operation
        this.authorizationController.authorize(requester, order.getCloudName(), Operation.CREATE.getValue(),
                order.getType().getValue());
        // Set an initial state for the resource instance that is yet to be created in the cloud
        order.setCachedInstanceState(InstanceState.DISPATCHED);
        // Add order to the poll of active orders and to the OPEN linked list
        OrderStateTransitioner.activateOrder(order);
        return order.getId();
    }

    private Instance getResourceInstance(String orderId, String federationTokenValue, ResourceType resourceType)
            throws FogbowException, UnexpectedException {
        FederationUser requester = AuthenticationUtil.authenticate(getAsPublicKey(), federationTokenValue);
        Order order = this.orderController.getOrder(orderId);
        authorizeOrder(requester, order.getCloudName(), Operation.GET, resourceType, order);
        return this.orderController.getResourceInstance(orderId);
    }

    private void deleteOrder(String orderId, String federationTokenValue, ResourceType resourceType)
            throws FogbowException, UnexpectedException {
        FederationUser requester = AuthenticationUtil.authenticate(getAsPublicKey(), federationTokenValue);
        Order order = this.orderController.getOrder(orderId);
        authorizeOrder(requester, order.getCloudName(), Operation.DELETE, resourceType, order);
        this.orderController.deleteOrder(orderId);
    }

    private Allocation getUserAllocation(String memberId, String cloudName, String federationTokenValue,
                                         ResourceType resourceType) throws FogbowException, UnexpectedException {
        FederationUser requester = AuthenticationUtil.authenticate(getAsPublicKey(), federationTokenValue);
        if (cloudName == null || cloudName.isEmpty()) cloudName = this.cloudListController.getDefaultCloudName();
        this.authorizationController.authorize(requester, cloudName, Operation.GET_USER_ALLOCATION.getValue(),
                resourceType.getValue());
        return this.orderController.getUserAllocation(memberId, requester, resourceType);
    }

    private Quota getUserQuota(String memberId, String cloudName, String federationTokenValue,
                               ResourceType resourceType) throws FogbowException, UnexpectedException {
        FederationUser requester = AuthenticationUtil.authenticate(getAsPublicKey(), federationTokenValue);
        if (cloudName == null || cloudName.isEmpty()) cloudName = this.cloudListController.getDefaultCloudName();
        this.authorizationController.authorize(requester, cloudName, Operation.GET_USER_QUOTA.getValue(),
                resourceType.getValue());
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId, cloudName);
        return cloudConnector.getUserQuota(requester, resourceType);
    }

	protected void authorizeOrder(FederationUser requester, String cloudName, Operation operation, ResourceType type,
			Order order) throws UnexpectedException, UnauthorizedRequestException, InstanceNotFoundException {
		// Check if requested type matches order type
		if (!order.getType().equals(type))
			throw new InstanceNotFoundException(Messages.Exception.MISMATCHING_RESOURCE_TYPE);
		// Check whether requester owns order
		FederationUser orderOwner = order.getFederationUser();
		String ownerUserId = orderOwner.getUserId();
		String requestUserId = requester.getUserId();
		if (!ownerUserId.equals(requestUserId)) {
			throw new UnauthorizedRequestException(Messages.Exception.REQUESTER_DOES_NOT_OWN_REQUEST);
		}
		this.authorizationController.authorize(requester, cloudName, operation.getValue(), type.getValue());
	}

    public RSAPublicKey getAsPublicKey() throws UnexpectedException, UnavailableProviderException, ConfigurationErrorException {
        if (this.asPublicKey == null) {
            this.asPublicKey = PublicKeysHolder.getInstance().getAsPublicKey();
        }
        return this.asPublicKey;
    }

    // Used for testing
    protected void setBuildNumber(String fileName) {
        Properties properties = PropertiesUtil.readProperties(fileName);
        this.buildNumber = properties.getProperty(ConfigurationConstants.BUILD_NUMBER_KEY,
                DefaultConfigurationConstants.BUILD_NUMBER);
    }
}
