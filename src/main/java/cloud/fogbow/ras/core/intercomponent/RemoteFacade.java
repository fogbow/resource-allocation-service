package cloud.fogbow.ras.core.intercomponent;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.api.http.response.ImageSummary;
import cloud.fogbow.ras.api.http.response.Instance;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.http.response.quotas.Quota;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.AuthorizationPluginInstantiator;
import cloud.fogbow.ras.core.CloudListController;
import cloud.fogbow.ras.core.OrderController;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.SecurityRuleController;
import cloud.fogbow.ras.core.SynchronizationManager;
import cloud.fogbow.ras.core.cloudconnector.CloudConnector;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import org.apache.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;

import java.util.List;

public class RemoteFacade {
    private static final Logger LOGGER = Logger.getLogger(RemoteFacade.class);

    private static RemoteFacade instance;
    private AuthorizationPlugin<RasOperation> authorizationPlugin;
    private OrderController orderController;
    private SecurityRuleController securityRuleController;
    private CloudListController cloudListController;
    private String localProviderId;
    private long onGoingRequests;

    private RemoteFacade() {
        this.onGoingRequests = 0;
        this.localProviderId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.PROVIDER_ID_KEY);
    }

    public static RemoteFacade getInstance() {
        synchronized (RemoteFacade.class) {
            if (instance == null) {
                instance = new RemoteFacade();
            }
            return instance;
        }
    }

    public void activateOrder(String requester, Order order) throws FogbowException {
        startOperation();
        try {
            // The user has already been authenticated by the requesting provider.
            checkOrderConsistency(requester, order);
            RasOperation rasOperation = new RasOperation(Operation.CREATE, order.getType(), order.getCloudName(), order);
            this.authorizationPlugin.isAuthorized(order.getSystemUser(), rasOperation);
            this.orderController.activateOrder(order);
        } finally {
            finishOperation();
        }
    }

    public Order getOrder(String requester, String orderId) throws FogbowException {
        startOperation();
        try {
            Order order = this.orderController.getOrder(orderId);
            checkOrderConsistency(requester, order);
            return order;            
        } finally {
            finishOperation();
        }
    }

    public Instance getResourceInstance(String requester, String orderId, SystemUser systemUser,
                                        ResourceType resourceType) throws FogbowException {
        startOperation();
        try {
            Order order = this.orderController.getOrder(orderId);
            // The user has already been authenticated by the requesting provider.
            checkOrderConsistency(requester, order);
            RasOperation rasOperation = new RasOperation(Operation.GET, resourceType, order.getCloudName(), order);
            this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
            return this.orderController.getResourceInstance(order);            
        } finally {
            finishOperation();
        }
    }

    public void deleteOrder(String requester, String orderId, SystemUser systemUser,
                            ResourceType resourceType) throws FogbowException {
        startOperation();
        try {
            Order order = this.orderController.getOrder(orderId);
            // The user has already been authenticated by the requesting provider.
            checkOrderConsistency(requester, order);
            RasOperation rasOperation = new RasOperation(Operation.DELETE, resourceType, order.getCloudName(), order);
            this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
            this.orderController.deleteOrder(order);            
        } finally {
            finishOperation();
        }
    }

    public void pauseOrder(String requester, String orderId, SystemUser systemUser,
                            ResourceType resourceType) throws FogbowException {
        Order order = this.orderController.getOrder(orderId);
        // The user has already been authenticated by the requesting provider.
        checkOrderConsistency(requester, order);
        RasOperation rasOperation = new RasOperation(Operation.PAUSE, resourceType, order.getCloudName(), order);
        this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
        this.orderController.pauseOrder(order);
    }

    public void hibernateOrder(String requester, String orderId, SystemUser systemUser,
                            ResourceType resourceType) throws FogbowException {
        Order order = this.orderController.getOrder(orderId);
        // The user has already been authenticated by the requesting provider.
        checkOrderConsistency(requester, order);
        RasOperation rasOperation = new RasOperation(Operation.HIBERNATE, resourceType, order.getCloudName(), order);
        this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
        this.orderController.hibernateOrder(order);
    }

    public void stopOrder(String requester, String orderId, SystemUser systemUser, 
            ResourceType resourceType) throws FogbowException {
        Order order = this.orderController.getOrder(orderId);
        // The user has already been authenticated by the requesting provider.
        checkOrderConsistency(requester, order);
        RasOperation rasOperation = new RasOperation(Operation.STOP, resourceType, order.getCloudName(), order);
        this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
        this.orderController.stopOrder(order);
    }
    
    public void takeSnapshot(String requester, String orderId, String name, SystemUser systemUser) throws FogbowException {
        ComputeOrder computeOrder = (ComputeOrder) this.orderController.getOrder(orderId);
        checkOrderConsistency(requester, computeOrder);
        RasOperation rasOperation =
                new RasOperation(Operation.TAKE_SNAPSHOT, ResourceType.COMPUTE, computeOrder.getCloudName(), computeOrder);
        this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
        this.orderController.takeSnapshot(computeOrder, name, systemUser);
    }

    public void resumeOrder(String requester, String orderId, SystemUser systemUser,
                            ResourceType resourceType) throws FogbowException {
        Order order = this.orderController.getOrder(orderId);
        // The user has already been authenticated by the requesting provider.
        checkOrderConsistency(requester, order);
        RasOperation rasOperation = new RasOperation(Operation.RESUME, resourceType, order.getCloudName(), order);
        this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
        this.orderController.resumeOrder(order);
    }

    public Quota getUserQuota(String requester, String cloudName, SystemUser systemUser) throws FogbowException {
        startOperation();
        try {
            // The user has already been authenticated by the requesting provider.
            RasOperation rasOperation = new RasOperation(Operation.GET, ResourceType.QUOTA, cloudName, 
                    requester, this.localProviderId);
            this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
            CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(this.localProviderId, cloudName);
            return cloudConnector.getUserQuota(systemUser);            
        } finally {
            finishOperation();
        }
    }

    public ImageInstance getImage(String requester, String cloudName, String imageId, SystemUser systemUser) throws FogbowException {
        startOperation();
        try {
            // The user has already been authenticated by the requesting provider.
            RasOperation rasOperation = new RasOperation(Operation.GET, ResourceType.IMAGE, cloudName, 
                    requester, this.localProviderId);
            this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
            CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(this.localProviderId, cloudName);
            return cloudConnector.getImage(imageId, systemUser);            
        } finally {
            finishOperation();
        }
    }

    public List<ImageSummary> getAllImages(String requester, String cloudName, SystemUser systemUser) throws FogbowException {
        startOperation();
        try {
            // The user has already been authenticated by the requesting provider.
            RasOperation rasOperation = new RasOperation(Operation.GET_ALL, ResourceType.IMAGE, cloudName, 
                    requester, this.localProviderId);
            this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
            CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(this.localProviderId, cloudName);
            return cloudConnector.getAllImages(systemUser);            
        } finally {
            finishOperation();
        }
    }

    public List<String> getCloudNames(String requester, SystemUser systemUser) throws FogbowException {
        startOperation();
        try {
            // The user has already been authenticated by the requesting provider.
            RasOperation rasOperation = new RasOperation(Operation.GET, ResourceType.CLOUD_NAME, 
                    requester, this.localProviderId);
            this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
            return this.cloudListController.getCloudNames();            
        } finally {
            finishOperation();
        }
    }

    public String createSecurityRule(String requester, String orderId, SecurityRule securityRule,
                                     SystemUser systemUser) throws FogbowException {
        startOperation();
        try {
            Order order = this.orderController.getOrder(orderId);
            checkOrderConsistency(requester, order);
            // The user has already been authenticated by the requesting provider.
            RasOperation rasOperation = new RasOperation(Operation.CREATE, ResourceType.SECURITY_RULE, order.getCloudName(),
                    order);
            this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
            return securityRuleController.createSecurityRule(order, securityRule, systemUser);            
        } finally {
            finishOperation();
        }
    }

    public List<SecurityRuleInstance> getAllSecurityRules(String requester, String orderId,
                                                          SystemUser systemUser) throws FogbowException {
        startOperation();
        try {
            Order order = this.orderController.getOrder(orderId);
            checkOrderConsistency(requester, order);
            // The user has already been authenticated by the requesting provider.
            RasOperation rasOperation = new RasOperation(Operation.GET_ALL, ResourceType.SECURITY_RULE, order.getCloudName(),
                    order);
            this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
            return securityRuleController.getAllSecurityRules(order, systemUser);            
        } finally {
            finishOperation();
        }
    }

    public void deleteSecurityRule(String requester, String cloudName, String ruleId,
                                   SystemUser systemUser) throws FogbowException {
        startOperation();
        try {
            RasOperation rasOperation = new RasOperation(Operation.DELETE, ResourceType.SECURITY_RULE, cloudName,
                    requester, this.localProviderId);
            this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
            this.securityRuleController.deleteSecurityRule(this.localProviderId, cloudName, ruleId, systemUser);            
        } finally {
            finishOperation();
        }
    }

    public void closeOrderAtRemoteRequester(String signallingProvider, String remoteOrderId) throws FogbowException {
        startOperation();
        try {
            // order is the java object that represents the order passed in the message
            // actualOrder is the java object that represents this order inside this server
            Order localOrder = this.orderController.getOrder(remoteOrderId);
            if (localOrder != null) {
                synchronized (localOrder) {
                    if (!localOrder.getProvider().equals(signallingProvider)) {
                        throw new InternalServerErrorException(String.format(Messages.Exception.SIGNALING_PROVIDER_DIFFERENT_OF_PROVIDER_S_S,
                                signallingProvider, localOrder.getProvider()));
                    }
                    this.orderController.closeOrder(localOrder);
                }
            } else {
                // The order no longer exists locally. This may only happen in rare corner cases when the remote provider
                // previously signalled that the order was closed, but failed before could save its order in stable storage.
                // When it recovers, it tries to signal again.
                LOGGER.warn(String.format(Messages.Log.UNABLE_TO_LOCATE_ORDER_S_S, remoteOrderId, signallingProvider));
                return;
            }            
        } finally {
            finishOperation();
        }
    }

    public void setAuthorizationPlugin(AuthorizationPlugin<RasOperation> authorizationPlugin) {
        this.authorizationPlugin = authorizationPlugin;
    }

    public synchronized void setOrderController(OrderController orderController) {
        this.orderController = orderController;
    }

    public void setCloudListController(CloudListController cloudListController) {
        this.cloudListController = cloudListController;
    }

    public synchronized void setSecurityRuleController(SecurityRuleController securityRuleController) {
        this.securityRuleController = securityRuleController;
    }

    private void checkOrderConsistency(String requester, Order order)
            throws InternalServerErrorException, InvalidParameterException {

        synchronized (order) {
            if (order == null || !order.getProvider().equals(this.localProviderId)) {
                throw new InternalServerErrorException(Messages.Exception.INCORRECT_PROVIDER);
            }
            if (!order.getRequester().equals(requester)) {
                throw new InvalidParameterException(Messages.Exception.INCORRECT_REQUESTING_PROVIDER);
            }
        }
    }

    public boolean noOnGoingRequests() {
        return this.onGoingRequests == 0;
    }

    public void reload() {
        this.reloadAuthorizationPlugin();
        this.reloadCloudListController();
    }
    
    private void reloadAuthorizationPlugin() {
        LOGGER.info(Messages.Log.RESETTING_AUTHORIZATION_PLUGIN_ON_REMOTE_FACADE);
        String className = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.AUTHORIZATION_PLUGIN_CLASS_KEY);
        this.authorizationPlugin = AuthorizationPluginInstantiator.getAuthorizationPlugin(className);
    }

    private void reloadCloudListController() {
        LOGGER.info(Messages.Log.RESETTING_CLOUD_LIST_CONTROLLER_ON_REMOTE_FACADE);
        this.cloudListController = new CloudListController();
    }
    
    //@VisibleForTesting
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
}
