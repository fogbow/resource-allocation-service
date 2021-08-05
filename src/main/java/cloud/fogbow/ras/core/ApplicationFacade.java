package cloud.fogbow.ras.core;

import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.ras.api.http.response.quotas.allocation.*;
import org.apache.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;

import cloud.fogbow.as.core.util.AuthenticationUtil;
import cloud.fogbow.common.constants.FogbowConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnacceptableOperationException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
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
import cloud.fogbow.ras.core.intercomponent.RemoteFacade;
import cloud.fogbow.ras.core.intercomponent.xmpp.requesters.RemoteGetCloudNamesRequest;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.UserData;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;

public class ApplicationFacade {
    
    private static final Logger LOGGER = Logger.getLogger(ApplicationFacade.class);

    private static ApplicationFacade instance;

    private long onGoingRequests;
    private AuthorizationPlugin<RasOperation> authorizationPlugin;
    private OrderController orderController;
    private SecurityRuleController securityRuleController;
    private CloudListController cloudListController;
    private String providerId;
    private RSAPublicKey asPublicKey;
    private String buildNumber;
    private static final List<ResourceType> RESOURCE_TYPE_DELETION_ORDER_ON_PURGE_USER = 
            Arrays.asList(ResourceType.PUBLIC_IP, ResourceType.ATTACHMENT, 
            ResourceType.VOLUME, ResourceType.COMPUTE, ResourceType.NETWORK);

    private ApplicationFacade() {
        this.onGoingRequests = 0;
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

    synchronized public long getOnGoingRequests() {
        return this.onGoingRequests;
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

    public String getPublicKey() throws InternalServerErrorException {
        startOperation();
        // There is no need to authenticate the user or authorize this operation
        try {
            return CryptoUtil.toBase64(ServiceAsymmetricKeysHolder.getInstance().getPublicKey());
        } catch (GeneralSecurityException e) {
            throw new InternalServerErrorException(e.getMessage());
        } finally {
            finishOperation();
        }
    }

    public List<String> getCloudNames(String providerId, String userToken) throws FogbowException {
        startOperation();
        try {
            SystemUser systemUser = authenticate(userToken);
            RasOperation rasOperation = new RasOperation(Operation.GET, ResourceType.CLOUD_NAME, 
                    this.providerId, providerId);
            
            this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
            if (providerId.equals(this.providerId)) {
                return this.cloudListController.getCloudNames();
            } else {
                RemoteGetCloudNamesRequest remoteGetCloudNames = getCloudNamesFromRemoteRequest(providerId, systemUser);
                List<String> cloudNames = remoteGetCloudNames.send();
                return cloudNames;
            }
        } catch (FogbowException e) {
            LOGGER.error(e.toString(), e);
            throw e;
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
            throw new InternalServerErrorException(e.getMessage());
        } finally {
            finishOperation();
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
        startOperation();
        try {
            SystemUser systemUser = authenticate(userToken);
            RasOperation rasOperation = new RasOperation(Operation.GET_ALL, resourceType, this.providerId, 
                    this.providerId);
            
            this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
            return this.orderController.getInstancesStatus(systemUser, resourceType);
        } finally {
            finishOperation();
        }
    }

    public List<ImageSummary> getAllImages(String providerId, String cloudName, String userToken)
            throws FogbowException {
        startOperation();
        try {
            SystemUser systemUser = authenticate(userToken);
            if (cloudName == null || cloudName.isEmpty()) {
                cloudName = this.cloudListController.getDefaultCloudName();
            }

            if (providerId == null) {
                providerId = this.providerId;
            }
            
            RasOperation rasOperation = new RasOperation(Operation.GET_ALL, ResourceType.IMAGE, cloudName, 
                    this.providerId, providerId);
            this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
            
            CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(providerId, cloudName);
            return cloudConnector.getAllImages(systemUser);
        } finally {
            finishOperation();
        }
    }

    public ImageInstance getImage(String providerId, String cloudName, String imageId, String userToken)
            throws FogbowException {
        startOperation();
        try {
            SystemUser systemUser = authenticate(userToken);
            if (cloudName == null || cloudName.isEmpty()) {
                cloudName = this.cloudListController.getDefaultCloudName();
            }

            if (providerId == null) {
                providerId = this.providerId;
            }
            
            RasOperation rasOperation = new RasOperation(Operation.GET, ResourceType.IMAGE, cloudName, 
                    this.providerId, providerId);
            this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
            
            CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(providerId, cloudName);
            return cloudConnector.getImage(imageId, systemUser);
        } finally {
            finishOperation();
        }
    }

    public String createSecurityRule(String orderId, SecurityRule securityRule, String userToken,
            ResourceType resourceTypeFromEndpoint) throws FogbowException {
        startOperation();
        try {
            Order order = this.orderController.getOrder(orderId);
            if (order.getType() != resourceTypeFromEndpoint) {
                throw new InstanceNotFoundException(
                        String.format(Messages.Exception.RESOURCE_TYPE_NOT_COMPATIBLE_S, resourceTypeFromEndpoint));
            }
            SystemUser systemUser = authenticate(userToken);
            String cloudName = order.getCloudName();
            
            RasOperation rasOperation = new RasOperation(Operation.CREATE, ResourceType.SECURITY_RULE, cloudName, order);
            this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
            
            return this.securityRuleController.createSecurityRule(order, securityRule, systemUser);
        } finally {
            finishOperation();
        }
    }

    public List<SecurityRuleInstance> getAllSecurityRules(String orderId, String userToken,
            ResourceType resourceTypeFromEndpoint) throws FogbowException {
        startOperation();
        try {
            Order order = orderController.getOrder(orderId);
            if (order.getType() != resourceTypeFromEndpoint) {
                throw new InstanceNotFoundException(
                        String.format(Messages.Exception.RESOURCE_TYPE_NOT_COMPATIBLE_S, resourceTypeFromEndpoint));
            }
            SystemUser systemUser = authenticate(userToken);
            String cloudName = order.getCloudName();
            
            RasOperation rasOperation = new RasOperation(Operation.GET_ALL, ResourceType.SECURITY_RULE, cloudName, order);
            this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
            
            return this.securityRuleController.getAllSecurityRules(order, systemUser);
        } finally {
            finishOperation();
        }
    }

    public void deleteSecurityRule(String orderId, String securityRuleId, String userToken,
            ResourceType resourceTypeFromEndpoint) throws FogbowException {
        startOperation();
        try {
            Order order = orderController.getOrder(orderId);
            if (order.getType() != resourceTypeFromEndpoint) {
                throw new InstanceNotFoundException(
                        String.format(Messages.Exception.RESOURCE_TYPE_NOT_COMPATIBLE_S, resourceTypeFromEndpoint));
            }
            SystemUser systemUser = authenticate(userToken);
            String cloudName = order.getCloudName();
            
            RasOperation rasOperation = new RasOperation(Operation.DELETE, ResourceType.SECURITY_RULE, cloudName, order);
            this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
            
            this.securityRuleController.deleteSecurityRule(order.getProvider(), cloudName, securityRuleId, systemUser);
        } finally {
            finishOperation();
        }
    }

    public void pauseCompute(String orderId, String userToken, ResourceType resourceType) throws FogbowException {
        SystemUser systemUser = authenticate(userToken);
        Order order = this.orderController.getOrder(orderId);

        if (!order.getType().equals(ResourceType.COMPUTE)) {
            throw new InvalidParameterException(Messages.Exception.INVALID_PARAMETER);
        }

        ComputeOrder computeOrder = (ComputeOrder) order;
        
        RasOperation rasOperation = new RasOperation(Operation.PAUSE, resourceType, computeOrder.getCloudName(), computeOrder);
        this.authorizationPlugin.isAuthorized(systemUser,rasOperation);
        this.orderController.pauseOrder(computeOrder);
    }

    public void hibernateCompute(String orderId, String userToken, ResourceType resourceType) throws FogbowException {
        SystemUser systemUser = authenticate(userToken);
        Order order = this.orderController.getOrder(orderId);

        if (!order.getType().equals(ResourceType.COMPUTE)) {
            throw new InvalidParameterException(Messages.Exception.INVALID_PARAMETER);
        }

        ComputeOrder computeOrder = (ComputeOrder) order;
        
        RasOperation rasOperation = new RasOperation(Operation.HIBERNATE, resourceType, computeOrder.getCloudName(), computeOrder);
        this.authorizationPlugin.isAuthorized(systemUser,rasOperation);
        this.orderController.hibernateOrder(computeOrder);
    }

    public void stopCompute(String orderId, String userToken, ResourceType resourceType) throws FogbowException {
        startOperation();
        try {
            SystemUser systemUser = authenticate(userToken);
            Order order = this.orderController.getOrder(orderId);

            if (!order.getType().equals(ResourceType.COMPUTE)) {
                throw new InvalidParameterException(Messages.Exception.INVALID_PARAMETER);
            }

            ComputeOrder computeOrder = (ComputeOrder) order;

            RasOperation rasOperation = new RasOperation(Operation.STOP, resourceType, computeOrder.getCloudName(),
                    computeOrder);
            this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
            this.orderController.stopOrder(computeOrder);
        } finally {
            finishOperation();
        }
    }
    
    public void resumeCompute(String orderId, String userToken, ResourceType resourceType) throws FogbowException {
        SystemUser systemUser = authenticate(userToken);
        Order order = this.orderController.getOrder(orderId);

        if (!order.getType().equals(ResourceType.COMPUTE)) {
            throw new InvalidParameterException(Messages.Exception.INVALID_PARAMETER);
        }

        ComputeOrder computeOrder = (ComputeOrder) order;
        
        RasOperation rasOperation = new RasOperation(Operation.RESUME, resourceType, computeOrder.getCloudName(), computeOrder);
        this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
        this.orderController.resumeOrder(computeOrder);
    }

	public void pauseUserComputes(String userId, String userProviderId, String userToken) throws FogbowException {
		startOperation();
		try {
			SystemUser systemUser = authenticate(userToken);
	        RasOperation rasOperation = new RasOperation(Operation.PAUSE_ALL, ResourceType.COMPUTE, this.providerId, 
	                this.providerId);
	        
	        this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
	        List<InstanceStatus> statuses = this.orderController.getUserInstancesStatus(
	                userId, userProviderId, ResourceType.COMPUTE);
			
	        for (InstanceStatus status : statuses) {
	        	String orderId = status.getInstanceId();
	        	// FIXME catch UnacceptableOperationException, in the case the order can not be paused
	        	this.orderController.pauseOrder(this.orderController.getOrder(orderId));
	        }	
		} finally {
			finishOperation();
		}	
	}
	
    public void hibernateUserComputes(String userId, String userProviderId, String userToken) throws FogbowException {
        startOperation();
        try {
            SystemUser systemUser = authenticate(userToken);
            RasOperation rasOperation = new RasOperation(Operation.HIBERNATE_ALL, ResourceType.COMPUTE, this.providerId, 
                    this.providerId);
            
            this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
            List<InstanceStatus> statuses = this.orderController.getUserInstancesStatus(
                    userId, userProviderId, ResourceType.COMPUTE);
            
            for (InstanceStatus status : statuses) {
                String orderId = status.getInstanceId();
                // FIXME catch UnacceptableOperationException, in the case the order can not be hibernated
                this.orderController.hibernateOrder(this.orderController.getOrder(orderId));
            }   
        } finally {
            finishOperation();
        }
    }

    public void stopUserComputes(String userId, String userProviderId, String userToken) throws FogbowException {
        startOperation();
        try {
            SystemUser systemUser = authenticate(userToken);
            RasOperation rasOperation = new RasOperation(Operation.STOP_ALL, ResourceType.COMPUTE, this.providerId, 
                    this.providerId);
            
            this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
            List<InstanceStatus> statuses = this.orderController.getUserInstancesStatus(
                    userId, userProviderId, ResourceType.COMPUTE);
            
            for (InstanceStatus status : statuses) {
                String orderId = status.getInstanceId();
                // FIXME catch UnacceptableOperationException, in the case the order can not be stopped
                this.orderController.stopOrder(this.orderController.getOrder(orderId));
            }   
        } finally {
            finishOperation();
        }   
    }
	
	public void resumeUserComputes(String userId, String userProviderId, String userToken) throws FogbowException {
		startOperation();
		try {
			SystemUser systemUser = authenticate(userToken);
	        RasOperation rasOperation = new RasOperation(Operation.RESUME_ALL, ResourceType.COMPUTE, this.providerId, 
	                this.providerId);
	        
	        this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
	        List<InstanceStatus> statuses = this.orderController.getUserInstancesStatus(
	                userId, userProviderId, ResourceType.COMPUTE);
			
	        for (InstanceStatus status : statuses) {
	        	String orderId = status.getInstanceId();
	        	// FIXME catch UnacceptableOperationException, in the case the order can not be resumed
	        	this.orderController.resumeOrder(this.orderController.getOrder(orderId));
	        }	
		} finally {
			finishOperation();
		}
	}
    
    public void takeSnapshot(String orderId, String name, String userToken, ResourceType resourceType) throws FogbowException{
        SystemUser systemUser = authenticate(userToken);
        Order order = this.orderController.getOrder(orderId);

        if (!order.getType().equals(ResourceType.COMPUTE)) {
            throw new InvalidParameterException(Messages.Exception.INVALID_PARAMETER);
        }

        ComputeOrder computeOrder = (ComputeOrder) order;

        RasOperation rasOperation =
                new RasOperation(Operation.TAKE_SNAPSHOT, resourceType, computeOrder.getCloudName(), computeOrder);
        this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
        this.orderController.takeSnapshot(computeOrder, name, systemUser);

    }

    public void purgeUser(String userToken, String userId, String provider) throws FogbowException {
        startOperation();
        try {
            SystemUser systemUser = authenticate(userToken);
            RasOperation rasOperation = new RasOperation(Operation.DELETE, ResourceType.USER,
                    this.providerId, this.providerId);
            
            this.authorizationPlugin.isAuthorized(systemUser, rasOperation);

            doPurgeUser(systemUser);
        } finally {
            finishOperation();
        }
    }
    
    private void doPurgeUser(SystemUser user) 
            throws FogbowException {
        List<Order> userOrders = new ArrayList<>();
        userOrders.addAll(this.orderController.getAllOrders(user, ResourceType.PUBLIC_IP));
        userOrders.addAll(this.orderController.getAllOrders(user, ResourceType.ATTACHMENT));
        userOrders.addAll(this.orderController.getAllOrders(user, ResourceType.VOLUME));
        userOrders.addAll(this.orderController.getAllOrders(user, ResourceType.COMPUTE));
        userOrders.addAll(this.orderController.getAllOrders(user, ResourceType.NETWORK));

        boolean removedAll;
        
        do {
            removedAll = true;
            
            for (Order order : userOrders) {
                if (!safeRemove(order)) {
                    removedAll = false;
                }
            }
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        } while (!removedAll);
    }

    private boolean safeRemove(Order order) throws FogbowException {
        OrderState state = order.getOrderState();
        
        if (orderIsClosed(state)) {
            return true;
        }
        
        if (deletingOrder(state)) {
            return false;
        }
        
        if (this.orderController.dependenciesAreClosed(order)) {
            this.orderController.deleteOrder(order);
            return false;
        } else {
            return false;
        }
    }
    
    private boolean deletingOrder(OrderState orderState) {
        return orderState.equals(OrderState.CHECKING_DELETION) || 
                orderState.equals(OrderState.ASSIGNED_FOR_DELETION);
    }

    private boolean orderIsClosed(OrderState orderState) {
        return orderState.equals(OrderState.CLOSED);
    }

    // These methods are protected to be used in testing
    
    protected RemoteGetCloudNamesRequest getCloudNamesFromRemoteRequest(String providerId, SystemUser systemUser) {
        RemoteGetCloudNamesRequest remoteGetCloudNames = new RemoteGetCloudNamesRequest(providerId, systemUser);
        return remoteGetCloudNames;
    }
    
    protected SystemUser authenticate(String userToken) throws FogbowException {
        RSAPublicKey keyRSA = getAsPublicKey();
        return AuthenticationUtil.authenticate(keyRSA, userToken);
    }
    
    protected String activateOrder(Order order, String userToken) throws FogbowException {
        startOperation();
        try {
            // Check if the user is authentic
            SystemUser systemUser = authenticate(userToken);
            // Set requester field in the order
            order.setSystemUser(systemUser);
            // Check consistency of orders that have other orders embedded (eg. an AttachmentOrder embeds
            // both a ComputeOrder and a VolumeOrder).
            checkEmbeddedOrdersConsistency(order);
            // Check if the authenticated user is authorized to perform the requested operation
            RasOperation rasOperation = new RasOperation(Operation.CREATE, order.getType(), order.getCloudName(), order);
            this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
            // Add order to the poll of active orders and to the OPEN linked list
            return this.orderController.activateOrder(order);
        } finally {
            finishOperation();
        }
    }

    protected Instance getResourceInstance(String orderId, String userToken, ResourceType resourceType) throws FogbowException {
        startOperation();
        try {
            SystemUser systemUser = authenticate(userToken);
            Order order = this.orderController.getOrder(orderId);
            RasOperation rasOperation = new RasOperation(Operation.GET, resourceType, order.getCloudName(), order);
            this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
            return this.orderController.getResourceInstance(order);
        } finally {
            finishOperation();
        }
    }

    protected void deleteOrder(String orderId, String userToken, ResourceType resourceType) throws FogbowException {
        startOperation();
        try {
            SystemUser systemUser = authenticate(userToken);
            Order order = this.orderController.getOrder(orderId);
            RasOperation rasOperation = new RasOperation(Operation.DELETE, resourceType, order.getCloudName(), order);
            this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
            this.orderController.deleteOrder(order);
        } finally {
            finishOperation();
        }
    }

    protected Allocation getUserAllocation(String providerId, String cloudName, String userToken,
            ResourceType resourceType) throws FogbowException {
        startOperation();
        try {
            SystemUser systemUser = authenticate(userToken);
            if (cloudName == null || cloudName.isEmpty())
                cloudName = this.cloudListController.getDefaultCloudName();
            RasOperation rasOperation = new RasOperation(Operation.GET_USER_ALLOCATION, resourceType, cloudName, 
                    this.providerId, providerId);
            this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
            return this.orderController.getUserAllocation(providerId, cloudName, systemUser, resourceType);
        } finally {
            finishOperation();
        }
    }

    protected Quota getUserQuota(String providerId, String cloudName, String userToken)
            throws FogbowException {
        startOperation();
        try {
            SystemUser systemUser = authenticate(userToken);
            if (cloudName == null || cloudName.isEmpty())
                cloudName = this.cloudListController.getDefaultCloudName();
            RasOperation rasOperation = new RasOperation(Operation.GET, ResourceType.QUOTA, cloudName, 
                    this.providerId, providerId);
            this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
            CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(providerId, cloudName);
            return cloudConnector.getUserQuota(systemUser);
        } finally {
            finishOperation();
        }
    }
    
    protected RSAPublicKey getAsPublicKey() throws FogbowException {
        if (this.asPublicKey == null) {
            this.asPublicKey = RasPublicKeysHolder.getInstance().getAsPublicKey();
        }
        return this.asPublicKey;
    }

    protected void checkEmbeddedOrdersConsistency(Order order) throws InvalidParameterException, InternalServerErrorException {
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
                throw new InternalServerErrorException(String.format(Messages.Exception.UNSUPPORTED_REQUEST_TYPE_S, order.getType()));
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
            throw new InvalidParameterException(Messages.Exception.CLOUD_NAMES_DO_NOT_MATCH);
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

    public void reload(String userToken) throws FogbowException {
        SystemUser systemUser = authenticate(userToken);
        RasOperation rasOperation = new RasOperation(Operation.RELOAD, ResourceType.CONFIGURATION, 
                this.providerId, this.providerId);
        this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
        
        SynchronizationManager.getInstance().setAsReloading();
        
        while (this.onGoingRequests != 0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        while (!RemoteFacade.getInstance().noOnGoingRequests()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        reloadPropertiesHolder();
        reloadASPublicKeys();
        reloadAuthorizationPlugin();
        reloadCloudListController();
        reloadKeys();
        RemoteFacade.getInstance().reload();
        SynchronizationManager.getInstance().reload();
    }

    @VisibleForTesting
    void startOperation() {
        while (SynchronizationManager.getInstance().isReloading())
            ;
        synchronized (this) {
            this.onGoingRequests++;
        }
    }

    @VisibleForTesting
    synchronized void finishOperation() {
        this.onGoingRequests--;
    }
    
    private void reloadPropertiesHolder() {
        LOGGER.info(Messages.Log.RESETTING_PROPERTIES_HOLDER);
        PropertiesHolder.reset();
    }

    private void reloadASPublicKeys() throws FogbowException {
        LOGGER.info(Messages.Log.RESETTING_AS_PUBLIC_KEYS);
        this.asPublicKey = null;
        RasPublicKeysHolder.reset();
        getAsPublicKey();
    }

    private void reloadAuthorizationPlugin() {
        LOGGER.info(Messages.Log.RESETTING_AUTHORIZATION_PLUGIN);
        String className = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.AUTHORIZATION_PLUGIN_CLASS_KEY);
        this.authorizationPlugin = AuthorizationPluginInstantiator.getAuthorizationPlugin(className);
    }

    private void reloadCloudListController() {
        LOGGER.info(Messages.Log.RESETTING_CLOUD_LIST_CONTROLLER);
        this.cloudListController = new CloudListController();
    }
    
    private void reloadKeys() {
        String publicKeyFilePath = PropertiesHolder.getInstance().getProperty(FogbowConstants.PUBLIC_KEY_FILE_PATH);
        String privateKeyFilePath = PropertiesHolder.getInstance().getProperty(FogbowConstants.PRIVATE_KEY_FILE_PATH);
        ServiceAsymmetricKeysHolder.reset(publicKeyFilePath, privateKeyFilePath);
    }

    // used for testing only
    protected void setBuildNumber(String fileName) {
        Properties properties = PropertiesUtil.readProperties(fileName);
        this.buildNumber = properties.getProperty(ConfigurationPropertyKeys.BUILD_NUMBER_KEY,
                ConfigurationPropertyDefaults.BUILD_NUMBER);
    }

	public void setPolicy(String userToken, String policy) throws FogbowException {
        SystemUser systemUser = authenticate(userToken);
        RasOperation rasOperation = new RasOperation(Operation.CREATE, ResourceType.POLICY, 
                this.providerId, this.providerId);
        this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
        
        SynchronizationManager.getInstance().setAsReloading();
        
        while (this.onGoingRequests != 0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        this.authorizationPlugin.setPolicy(policy);
        
        SynchronizationManager.getInstance().setAsNotReloading();
	}

	public void updatePolicy(String userToken, String policy) throws FogbowException {
        SystemUser systemUser = authenticate(userToken);
        RasOperation rasOperation = new RasOperation(Operation.CREATE, ResourceType.POLICY, 
                this.providerId, this.providerId);
        this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
        
        SynchronizationManager.getInstance().setAsReloading();
        
        while (this.onGoingRequests != 0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        this.authorizationPlugin.updatePolicy(policy);
        
        SynchronizationManager.getInstance().setAsNotReloading();
	}
}
