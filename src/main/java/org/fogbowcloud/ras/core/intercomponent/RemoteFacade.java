package org.fogbowcloud.ras.core.intercomponent;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.AaaController;
import org.fogbowcloud.ras.core.OrderController;
import org.fogbowcloud.ras.core.OrderStateTransitioner;
import org.fogbowcloud.ras.core.SecurityRuleController;
import org.fogbowcloud.ras.core.cloudconnector.CloudConnector;
import org.fogbowcloud.ras.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.constants.Operation;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.intercomponent.xmpp.Event;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.images.Image;
import org.fogbowcloud.ras.core.models.instances.Instance;
import org.fogbowcloud.ras.core.models.orders.ComputeOrder;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.orders.OrderState;
import org.fogbowcloud.ras.core.models.quotas.Quota;
import org.fogbowcloud.ras.core.models.securityrules.SecurityRule;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.plugins.interoperability.NetworkPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.PublicIpPlugin;

import java.util.List;
import java.util.Map;

public class RemoteFacade {
    private static final Logger LOGGER = Logger.getLogger(RemoteFacade.class);

    private static RemoteFacade instance;
    private AaaController aaaController;
    private OrderController orderController;
    private SecurityRuleController securityRuleController;

    private RemoteFacade() {
    }

    public static RemoteFacade getInstance() {
        synchronized (RemoteFacade.class) {
            if (instance == null) {
                instance = new RemoteFacade();
            }
            return instance;
        }
    }

    public void activateOrder(String requestingMember, Order order) throws UnexpectedException, FogbowRasException {
        this.aaaController.remoteAuthenticateAndAuthorize(requestingMember, order.getFederationUserToken(),
                Operation.CREATE, order.getType(), order);
        OrderStateTransitioner.activateOrder(order);
    }

    public Instance getResourceInstance(String requestingMember, String orderId,
                            FederationUserToken federationUserToken, ResourceType resourceType) throws Exception {
        Order order = this.orderController.getOrder(orderId);
        this.aaaController.remoteAuthenticateAndAuthorize(requestingMember, federationUserToken, Operation.GET,
                resourceType, order);
        return this.orderController.getResourceInstance(orderId);
    }

    public void deleteOrder(String requestingMember, String orderId, FederationUserToken federationUserToken,
                            ResourceType resourceType) throws Exception {
        Order order = this.orderController.getOrder(orderId);
        this.aaaController.remoteAuthenticateAndAuthorize(requestingMember, federationUserToken, Operation.DELETE,
                resourceType, order);
        this.orderController.deleteOrder(orderId);
    }

    public Quota getUserQuota(String requestingMember, String memberId, FederationUserToken federationUserToken,
                              ResourceType resourceType) throws Exception {
        LOGGER.debug("AA: for user: " + federationUserToken.toString());
        this.aaaController.remoteAuthenticateAndAuthorize(requestingMember, federationUserToken,
                Operation.GET_USER_QUOTA, resourceType, memberId);
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId);
        LOGGER.debug("Calling quota plugin at site: " + memberId);
        return cloudConnector.getUserQuota(federationUserToken, resourceType);
    }

    public Image getImage(String requestingMember, String memberId, String imageId,
                          FederationUserToken federationUserToken) throws Exception {
        this.aaaController.remoteAuthenticateAndAuthorize(requestingMember, federationUserToken, Operation.GET_IMAGE,
                ResourceType.IMAGE, memberId);
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId);
        return cloudConnector.getImage(imageId, federationUserToken);
    }

    public Map<String, String> getAllImages(String requestingMember, String memberId,
                                            FederationUserToken federationUserToken) throws Exception {
        this.aaaController.remoteAuthenticateAndAuthorize(requestingMember, federationUserToken,
                Operation.GET_ALL_IMAGES, ResourceType.IMAGE, memberId);
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId);
        return cloudConnector.getAllImages(federationUserToken);
    }

    public void handleRemoteEvent(String signallingMember, Event event, Order remoteOrder) throws FogbowRasException,
            UnexpectedException {
        // order is a java object that represents the order passed in the message
        // actualOrder is the java object that represents this order inside the current server
        Order localOrder = this.orderController.getOrder(remoteOrder.getId());
        if (!localOrder.getProvider().equals(signallingMember)) {
            throw new UnexpectedException(String.format(Messages.Exception.SIGNALING_MEMBER_DIFFERENT_OF_PROVIDER,
                    signallingMember, localOrder.getProvider()));
        }
        updateLocalOrder(localOrder, remoteOrder, event);
        switch (event) {
            case INSTANCE_FULFILLED:
                OrderStateTransitioner.transition(localOrder, OrderState.FULFILLED);
                break;
            case INSTANCE_FAILED:
                OrderStateTransitioner.transition(localOrder, OrderState.FAILED_AFTER_SUCCESSUL_REQUEST);
                break;
        }
    }

    private void updateLocalOrder(Order localOrder, Order remoteOrder, Event event) {
        synchronized (localOrder) {
            if (localOrder.getOrderState() != OrderState.PENDING) {
                // The order has been deleted or already updated
                return;
            }
            // The Order fields that have been changed remotely, need to be copied to the local Order.
            // Check the several cloud plugins to see which fields are changed.
            // The exception is the instanceId, which is only required at the providing member side.
            localOrder.setCachedInstanceState(remoteOrder.getCachedInstanceState());
            if (localOrder.getType().equals(ResourceType.COMPUTE)) {
                ComputeOrder localCompute = (ComputeOrder) localOrder;
                ComputeOrder remoteCompute = (ComputeOrder) remoteOrder;
                localCompute.setActualAllocation(remoteCompute.getActualAllocation());
            }
        }
    }

    public synchronized void setAaaController(AaaController AaaController) {
        this.aaaController = AaaController;
    }

    public synchronized void setOrderController(OrderController orderController) {
        this.orderController = orderController;
    }

    public synchronized void setSecurityRuleController(SecurityRuleController securityRuleController) {
        this.securityRuleController = securityRuleController;
    }

    public String createSecurityRule(String requestingMember, String orderId, SecurityRule securityRule,
            FederationUserToken federationUserToken) throws Exception {
        Order majorOrder = orderController.getOrder(orderId);
        this.aaaController.remoteAuthenticateAndAuthorize(requestingMember, federationUserToken, Operation.CREATE,
                ResourceType.SECURITY_RULE, majorOrder.getProvider());
        return securityRuleController.createSecurityRule(majorOrder, securityRule, federationUserToken);
    }

    public List<SecurityRule> getAllSecurityRules(String requestingMember, String orderId,
                                                  FederationUserToken federationUserToken) throws Exception {
        Order majorOrder = orderController.getOrder(orderId);
        this.aaaController.remoteAuthenticateAndAuthorize(requestingMember, federationUserToken, Operation.CREATE,
                ResourceType.SECURITY_RULE, majorOrder.getProvider());
        return securityRuleController.getAllSecurityRules(majorOrder, federationUserToken);
    }

    public void deleteSecurityRule(String requestingMember, String providerId, String ruleId,
        FederationUserToken federationUserToken) throws Exception {
        this.aaaController.remoteAuthenticateAndAuthorize(requestingMember, federationUserToken, Operation.CREATE,
                ResourceType.SECURITY_RULE, providerId);
        securityRuleController.deleteSecurityRule(ruleId, providerId, federationUserToken);
    }

    private String getOrderIdFromSecurityRuleName(String securityRuleName) throws InvalidParameterException {
        String splitRegex = NetworkPlugin.SECURITY_GROUP_PREFIX + "|" + PublicIpPlugin.SECURITY_GROUP_PREFIX;
        String[] securityRuleParts = securityRuleName.split(splitRegex);
        if (securityRuleParts.length > 2) {
            String orderId = securityRuleParts[1];
            return orderId;
        }
        throw new InvalidParameterException();
    }
}
