package cloud.fogbow.ras.core.intercomponent;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.api.http.response.ImageSummary;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.*;
import cloud.fogbow.ras.core.cloudconnector.CloudConnector;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.api.http.response.Instance;
import cloud.fogbow.ras.api.http.response.quotas.Quota;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import org.apache.log4j.Logger;

import java.util.List;

public class RemoteFacade {
    private static final Logger LOGGER = Logger.getLogger(RemoteFacade.class);

    private static RemoteFacade instance;
    private AuthorizationPlugin<RasOperation> authorizationPlugin;
    private OrderController orderController;
    private SecurityRuleController securityRuleController;
    private CloudListController cloudListController;
    private String localProviderId;

    private RemoteFacade() {
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

    public void activateOrder(String requestingProvider, Order order) throws FogbowException {
        // The user has already been authenticated by the requesting provider.
        checkOrderConsistency(requestingProvider, order);
        RasOperation rasOperation = new RasOperation(Operation.CREATE, order.getType(), order.getCloudName(), order);
        this.authorizationPlugin.isAuthorized(order.getSystemUser(), rasOperation);
        this.orderController.activateOrder(order);
    }

    public Order getOrder(String requestingProvider, String orderId) throws FogbowException {
        Order order = this.orderController.getOrder(orderId);
        checkOrderConsistency(requestingProvider, order);
        return order;
    }

    public Instance getResourceInstance(String requestingProvider, String orderId, SystemUser systemUser,
                                        ResourceType resourceType) throws FogbowException {
        Order order = this.orderController.getOrder(orderId);
        // The user has already been authenticated by the requesting provider.
        checkOrderConsistency(requestingProvider, order);
        RasOperation rasOperation = new RasOperation(Operation.GET, resourceType, order.getCloudName(), order);
        this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
        return this.orderController.getResourceInstance(order);
    }

    public void deleteOrder(String requestingProvider, String orderId, SystemUser systemUser,
                            ResourceType resourceType) throws FogbowException {
        Order order = this.orderController.getOrder(orderId);
        // The user has already been authenticated by the requesting provider.
        checkOrderConsistency(requestingProvider, order);
        RasOperation rasOperation = new RasOperation(Operation.DELETE, resourceType, order.getCloudName(), order);
        this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
        this.orderController.deleteOrder(order);
    }

    public Quota getUserQuota(String requestingProvider, String cloudName, SystemUser systemUser) throws FogbowException {
        // The user has already been authenticated by the requesting provider.
        RasOperation rasOperation = new RasOperation(Operation.GET, ResourceType.QUOTA, cloudName);
        this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(this.localProviderId, cloudName);
        return cloudConnector.getUserQuota(systemUser);
    }

    public ImageInstance getImage(String requestingProvider, String cloudName, String imageId, SystemUser systemUser) throws FogbowException {
        // The user has already been authenticated by the requesting provider.
        RasOperation rasOperation = new RasOperation(Operation.GET, ResourceType.IMAGE, cloudName);
        this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(this.localProviderId, cloudName);
        return cloudConnector.getImage(imageId, systemUser);
    }

    public List<ImageSummary> getAllImages(String requestingProvider, String cloudName, SystemUser systemUser) throws FogbowException {
        // The user has already been authenticated by the requesting provider.
        RasOperation rasOperation = new RasOperation(Operation.GET_ALL, ResourceType.IMAGE, cloudName);
        this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(this.localProviderId, cloudName);
        return cloudConnector.getAllImages(systemUser);
    }

    public List<String> getCloudNames(String requestingProvider, SystemUser systemUser) throws UnauthorizedRequestException {
        // The user has already been authenticated by the requesting provider.
        RasOperation rasOperation = new RasOperation(Operation.GET, ResourceType.CLOUD_NAME);
        this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
        return this.cloudListController.getCloudNames();
    }

    public String createSecurityRule(String requestingProvider, String orderId, SecurityRule securityRule,
                                     SystemUser systemUser) throws FogbowException {
        Order order = this.orderController.getOrder(orderId);
        checkOrderConsistency(requestingProvider, order);
        // The user has already been authenticated by the requesting provider.
        RasOperation rasOperation = new RasOperation(Operation.CREATE, ResourceType.SECURITY_RULE, order.getCloudName(),
                order);
        this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
        return securityRuleController.createSecurityRule(order, securityRule, systemUser);
    }

    public List<SecurityRuleInstance> getAllSecurityRules(String requestingProvider, String orderId,
                                                          SystemUser systemUser) throws FogbowException {
        Order order = this.orderController.getOrder(orderId);
        checkOrderConsistency(requestingProvider, order);
        // The user has already been authenticated by the requesting provider.
        RasOperation rasOperation = new RasOperation(Operation.GET_ALL, ResourceType.SECURITY_RULE, order.getCloudName(),
                order);
        this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
        return securityRuleController.getAllSecurityRules(order, systemUser);
    }

    public void deleteSecurityRule(String requestingProvider, String cloudName, String ruleId,
                                   SystemUser systemUser) throws FogbowException {
        RasOperation rasOperation = new RasOperation(Operation.DELETE, ResourceType.SECURITY_RULE, cloudName);
        this.authorizationPlugin.isAuthorized(systemUser, rasOperation);
        this.securityRuleController.deleteSecurityRule(this.localProviderId, cloudName, ruleId, systemUser);
    }

    public void closeOrderAtRemoteRequester(String signallingProvider, String remoteOrderId) throws FogbowException {
        // order is the java object that represents the order passed in the message
        // actualOrder is the java object that represents this order inside this server
        Order localOrder = this.orderController.getOrder(remoteOrderId);
        if (localOrder != null) {
            synchronized (localOrder) {
                if (!localOrder.getProvider().equals(signallingProvider)) {
                    throw new UnexpectedException(String.format(Messages.Exception.SIGNALING_PROVIDER_DIFFERENT_OF_PROVIDER,
                            signallingProvider, localOrder.getProvider()));
                }
                this.orderController.closeOrder(localOrder);
            }
        } else {
            // The order no longer exists locally. This may only happen in rare corner cases when the remote provider
            // previously signalled that the order was closed, but failed before could save its order in stable storage.
            // When it recovers, it tries to signal again.
            LOGGER.warn(String.format(Messages.Warn.UNABLE_TO_LOCATE_ORDER_S_S, remoteOrderId, signallingProvider));
            return;
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

    private void checkOrderConsistency(String requestingProvider, Order order) throws UnexpectedException,
            InvalidParameterException {
        synchronized (order) {
            if (order == null || !order.getProvider().equals(this.localProviderId)) {
                throw new UnexpectedException(Messages.Exception.INCORRECT_PROVIDER);
            }
            if (!order.getRequester().equals(requestingProvider)) {
                throw new InvalidParameterException(Messages.Exception.INCORRECT_REQUESTING_PROVIDER);
            }
        }
    }
}
