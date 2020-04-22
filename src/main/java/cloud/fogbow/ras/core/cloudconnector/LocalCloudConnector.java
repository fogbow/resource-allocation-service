package cloud.fogbow.ras.core.cloudconnector;

import java.sql.Timestamp;
import java.util.List;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.api.http.response.AttachmentInstance;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.api.http.response.ImageSummary;
import cloud.fogbow.ras.api.http.response.InstanceStatus;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.api.http.response.OrderInstance;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.http.response.VolumeInstance;
import cloud.fogbow.ras.api.http.response.quotas.Quota;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.InteroperabilityPluginInstantiator;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.auditing.AuditableRequest;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.AttachmentPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.ComputePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.ImagePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.NetworkPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.OrderPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.PublicIpPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.QuotaPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.SecurityRulePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.VolumePlugin;
import cloud.fogbow.ras.core.plugins.mapper.SystemToCloudMapperPlugin;

public class LocalCloudConnector implements CloudConnector {
    
    private static final Logger LOGGER = Logger.getLogger(LocalCloudConnector.class);

    private static final String DELETE_INSTANCE_OPERATION = "deleteInstance";
    private static final String DELETE_SECURITY_RULE_OPERATION = "deleteSecurityRule";
    private static final String GENERIC_REQUEST_OPERATION = "genericRequest";
    private static final String GET_ALL_IMAGES_OPERATION = "getAllImages";
    private static final String GET_ALL_SECURITY_RULES_OPERATION = "getAllSecurityRules";
    private static final String GET_IMAGE_OPERATION = "getImage";
    private static final String GET_INSTANCE_OPERATION = "getInstance";
    private static final String GET_QUOTA_OPERATION = "getQuota";
    private static final String REQUEST_INSTANCE_OPERATION = "requestInstance";
    private static final String REQUEST_SECURITY_RULES_OPERATION = "requestSecurityRules";

    private SystemToCloudMapperPlugin mapperPlugin;
    private PublicIpPlugin publicIpPlugin;
    private AttachmentPlugin attachmentPlugin;
    private ComputePlugin computePlugin;
    private NetworkPlugin networkPlugin;
    private VolumePlugin volumePlugin;
    private ImagePlugin imagePlugin;
    private SecurityRulePlugin securityRulePlugin;
    private QuotaPlugin quotaPlugin;

    private boolean auditRequestsOn = true;

    public LocalCloudConnector(InteroperabilityPluginInstantiator instantiator, String cloudName) {
        this.attachmentPlugin = instantiator.getAttachmentPlugin(cloudName);
        this.computePlugin = instantiator.getComputePlugin(cloudName);
        this.networkPlugin = instantiator.getNetworkPlugin(cloudName);
        this.volumePlugin = instantiator.getVolumePlugin(cloudName);
        this.imagePlugin = instantiator.getImagePlugin(cloudName);
        this.publicIpPlugin = instantiator.getPublicIpPlugin(cloudName);
        this.securityRulePlugin = instantiator.getSecurityRulePlugin(cloudName);
        this.mapperPlugin = instantiator.getSystemToCloudMapperPlugin(cloudName);
        this.quotaPlugin = instantiator.getQuotaPlugin(cloudName);
    }

    @Override
    public String requestInstance(Order order) throws FogbowException {
        LOGGER.debug(String.format(Messages.Info.MAPPING_USER_OP, REQUEST_INSTANCE_OPERATION, order));
        CloudUser cloudUser = this.mapperPlugin.map(order.getSystemUser());
        LOGGER.debug(String.format(Messages.Info.MAPPED_USER, cloudUser));

        String response = null;
        try {
            response = doRequestInstance(order, cloudUser);
            LOGGER.debug(String.format(Messages.Info.RESPONSE_RECEIVED, response));
        } catch (Throwable e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION, e + e.getMessage()));
            response = e.getClass().getName();
            throw e;
        } finally {
            auditRequest(Operation.CREATE, order.getType(), order.getSystemUser(), response);
        }

        return response;
    }

    @Override
    public void deleteInstance(Order order) throws FogbowException {
        LOGGER.debug(String.format(Messages.Info.MAPPING_USER_OP, DELETE_INSTANCE_OPERATION, order));
        CloudUser cloudUser = this.mapperPlugin.map(order.getSystemUser());
        LOGGER.debug(String.format(Messages.Info.MAPPED_USER, cloudUser));

        String response = null;
        try {
            doDeleteInstance(order, cloudUser);
            LOGGER.debug(Messages.Info.SUCCESS);
        } catch (Throwable e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION, e + e.getMessage()));
            response = e.getClass().getName();
            throw e;
        } finally {
            auditRequest(Operation.DELETE, order.getType(), order.getSystemUser(), response);
        }
    }

    @Override
    public OrderInstance getInstance(Order order) throws FogbowException {
        LOGGER.debug(String.format(Messages.Info.MAPPING_USER_OP, GET_INSTANCE_OPERATION, order));
        CloudUser cloudUser = this.mapperPlugin.map(order.getSystemUser());
        LOGGER.debug(String.format(Messages.Info.MAPPED_USER, cloudUser));

        String auditableResponse = null;
        OrderInstance instance = null;
        try {
            instance = doGetInstance(order, cloudUser);
            LOGGER.debug(String.format(Messages.Info.RESPONSE_RECEIVED, instance));
            instance.setState(InstanceStatus.mapInstanceStateFromOrderState(order.getOrderState()));
            auditableResponse = instance.toString();
        } catch (Throwable e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION, e + e.getMessage()));
            auditableResponse = e.getClass().getName();
            throw e;
        } finally {
            auditRequest(Operation.GET, order.getType(), order.getSystemUser(), auditableResponse);
        }

        return instance;
    }

    @Override
    public Quota getUserQuota(SystemUser systemUser) throws FogbowException {
        LOGGER.debug(String.format(Messages.Info.MAPPING_USER_OP, GET_QUOTA_OPERATION, systemUser));
        CloudUser cloudUser = this.mapperPlugin.map(systemUser);
        LOGGER.debug(String.format(Messages.Info.MAPPED_USER, cloudUser));

        String auditableResponse = null;
        Quota quota = null;
        try {
            quota = this.quotaPlugin.getUserQuota(cloudUser);
            LOGGER.debug(String.format(Messages.Info.RESPONSE_RECEIVED, quota));
            auditableResponse = quota.toString();
        } catch (Throwable e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION, e + e.getMessage()));
            auditableResponse = e.getClass().getName();
            throw e;
        } finally {
            auditRequest(Operation.GET, ResourceType.QUOTA, systemUser, auditableResponse);
        }

        return quota;
    }

    @Override
    public List<ImageSummary> getAllImages(SystemUser systemUser) throws FogbowException {
        LOGGER.debug(String.format(Messages.Info.MAPPING_USER_OP, GET_ALL_IMAGES_OPERATION, systemUser));
        CloudUser cloudUser = this.mapperPlugin.map(systemUser);
        LOGGER.debug(String.format(Messages.Info.MAPPED_USER, cloudUser));

        List<ImageSummary> images = null;
        String auditableResponse = null;
        try {
            images = doGetAllImages(cloudUser);
            LOGGER.debug(String.format(Messages.Info.RESPONSE_RECEIVED, images));
            auditableResponse = images.toString();
        } catch (Throwable e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION, e + e.getMessage()));
            auditableResponse = e.getClass().getName();
            throw e;
        } finally {
            auditRequest(Operation.GET_ALL, ResourceType.IMAGE, systemUser, auditableResponse);
        }

        return images;
    }

    @Override
    public ImageInstance getImage(String imageId, SystemUser systemUser) throws FogbowException {
        LOGGER.debug(String.format(Messages.Info.MAPPING_USER_OP, GET_IMAGE_OPERATION, systemUser));
        CloudUser cloudUser = this.mapperPlugin.map(systemUser);
        LOGGER.debug(String.format(Messages.Info.MAPPED_USER, cloudUser));

        ImageInstance imageInstance = null;
        String auditableResponse = null;
        try {
            imageInstance = doGetImage(imageId, cloudUser);
            LOGGER.debug(String.format(Messages.Info.RESPONSE_RECEIVED, imageInstance));
            auditableResponse = imageInstance.toString();
        } catch (Throwable e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION, e + e.getMessage()));
            auditableResponse = e.getClass().getName();
            throw e;
        } finally {
            auditRequest(Operation.GET, ResourceType.IMAGE, systemUser, auditableResponse);
        }

        return imageInstance;
    }

    @Override
    public List<SecurityRuleInstance> getAllSecurityRules(Order order, SystemUser systemUser) throws FogbowException {
        LOGGER.debug(String.format(Messages.Info.MAPPING_USER_OP, GET_ALL_SECURITY_RULES_OPERATION, order));
        CloudUser cloudUser = this.mapperPlugin.map(systemUser);
        LOGGER.debug(String.format(Messages.Info.MAPPED_USER, cloudUser));

        List<SecurityRuleInstance> securityRuleInstances = null;
        String auditableResponse = null;
        try {
            securityRuleInstances = doGetAllSecurityRules(order, cloudUser);
            LOGGER.debug(String.format(Messages.Info.RESPONSE_RECEIVED, securityRuleInstances));
            auditableResponse = securityRuleInstances.toString();
        } catch (Throwable e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION, e + e.getMessage()));
            auditableResponse = e.getClass().getName();
            throw e;
        } finally {
            auditRequest(Operation.GET_ALL, order.getType(), systemUser, auditableResponse);
        }

        return securityRuleInstances;
    }

    @Override
    public String requestSecurityRule(Order order, SecurityRule securityRule, SystemUser systemUser) throws FogbowException {
        LOGGER.debug(String.format(Messages.Info.MAPPING_USER_OP, REQUEST_SECURITY_RULES_OPERATION, order));
        CloudUser cloudUser = this.mapperPlugin.map(systemUser);
        LOGGER.debug(String.format(Messages.Info.MAPPED_USER, cloudUser));

        String response = null;
        try {
            response = doRequestSecurityRule(order, securityRule, cloudUser);
            LOGGER.debug(String.format(Messages.Info.RESPONSE_RECEIVED, response));
        } catch (Throwable e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION, e + e.getMessage()));
            response = e.getClass().getName();
            throw e;
        } finally {
            auditRequest(Operation.CREATE, order.getType(), systemUser, response);
        }

        return response;
    }

    @Override
    public void deleteSecurityRule(String securityRuleId, SystemUser systemUser) throws FogbowException {
        LOGGER.debug(String.format(Messages.Info.MAPPING_USER_OP, DELETE_SECURITY_RULE_OPERATION, securityRuleId));
        CloudUser cloudUser = this.mapperPlugin.map(systemUser);
        LOGGER.debug(String.format(Messages.Info.MAPPED_USER, cloudUser));

        String response = null;
        try {
            doDeleteSecurityRule(securityRuleId, cloudUser);
            LOGGER.debug(Messages.Info.SUCCESS);
        } catch (Throwable e) {
            response = e.getClass().getName();
            throw e;
        } finally {
            auditRequest(Operation.DELETE, ResourceType.SECURITY_RULE, systemUser, response);
        }
    }

    protected String doRequestInstance(Order order, CloudUser cloudUser) throws FogbowException {
        String instanceId;
        OrderPlugin plugin = checkOrderCastingAndSetPlugin(order, order.getType());
        instanceId = plugin.requestInstance(order, cloudUser);
        if (instanceId == null) {
            throw new UnexpectedException(Messages.Exception.NULL_VALUE_RETURNED);
        }
        return instanceId;
    }

    protected void doDeleteInstance(Order order, CloudUser cloudUser) throws FogbowException {
        OrderPlugin plugin = checkOrderCastingAndSetPlugin(order, order.getType());
        try {
            if (order.getInstanceId() != null) {
                plugin.deleteInstance(order, cloudUser);
            } else {
                // If instanceId is null, then there is nothing to do.
                return;
            }
        } catch (InstanceNotFoundException e) {
            // This may happen if the RAS crashed after the instance was deleted, but before the new state
            // is updated in stable storage, or if the instance has been deleted directly in the cloud
            // without the intervention of the RAS.
            LOGGER.warn(String.format(Messages.Warn.INSTANCE_S_ALREADY_DELETED, order.getId()));
            throw e;
        }
    }

    protected OrderInstance doGetInstance(Order order, CloudUser cloudUser) throws FogbowException {
        String instanceId = order.getInstanceId();
        if (instanceId != null) {
            return getResourceInstance(order, order.getType(), cloudUser);
        } 
        // When there is no instance, an empty one is created with the appropriate state
        return createEmptyInstance(order);
    }

    protected OrderInstance createEmptyInstance(Order order) throws UnexpectedException {
        OrderInstance instance = null;
        switch (order.getType()) {
            case COMPUTE:
                instance = new ComputeInstance(order.getId());
                break;
            case VOLUME:
                instance = new VolumeInstance(order.getId());
                break;
            case NETWORK:
                instance = new NetworkInstance(order.getId());
                break;
            case ATTACHMENT:
                instance = new AttachmentInstance(order.getId());
                break;
            case PUBLIC_IP:
                instance = new PublicIpInstance(order.getId());
                break;
            default:
                throw new UnexpectedException(Messages.Exception.UNSUPPORTED_REQUEST_TYPE);
        }
        return instance;
    }

    private OrderInstance getResourceInstance(Order order, ResourceType resourceType, CloudUser cloudUser) throws FogbowException {
        OrderPlugin plugin = checkOrderCastingAndSetPlugin(order, resourceType);
        OrderInstance instance = plugin.getInstance(order, cloudUser);
        if (instance != null) {
            boolean instanceHasFailed = plugin.hasFailed(instance.getCloudState());
            boolean instanceIsReady = plugin.isReady(instance.getCloudState());
            if (instanceHasFailed) instance.setHasFailed();
            if (instanceIsReady) instance.setReady();
            return instance;
        } else {
            throw new UnexpectedException(Messages.Exception.NULL_VALUE_RETURNED);
        }
    }

    protected List<ImageSummary> doGetAllImages(CloudUser token) throws FogbowException {
        return this.imagePlugin.getAllImages(token);
    }

    protected ImageInstance doGetImage(String imageId, CloudUser token) throws FogbowException {
        return this.imagePlugin.getImage(imageId, token);
    }

    protected List<SecurityRuleInstance> doGetAllSecurityRules(Order order, CloudUser token)
            throws FogbowException {
        return this.securityRulePlugin.getSecurityRules(order, token);
    }

    protected String doRequestSecurityRule(Order order, SecurityRule securityRule, CloudUser token)
            throws FogbowException {
        return this.securityRulePlugin.requestSecurityRule(securityRule, order, token);
    }

    protected void doDeleteSecurityRule(String securityRuleId, CloudUser token) throws FogbowException {
        this.securityRulePlugin.deleteSecurityRule(securityRuleId, token);
    }

    protected OrderPlugin checkOrderCastingAndSetPlugin(Order order, ResourceType resourceType)
            throws UnexpectedException {
        OrderPlugin plugin;
        boolean orderTypeMatch = false;

        // Orders that embed other orders (compute, attachment and publicip) need to check the consistency
        // of these orders when the order is being dispatched by the LocalCloudConnector.
        switch (resourceType) {
            case COMPUTE:
                orderTypeMatch = order instanceof ComputeOrder;
                plugin = this.computePlugin;
                break;
            case NETWORK:
                orderTypeMatch = order instanceof NetworkOrder;
                plugin = this.networkPlugin;
                break;
            case VOLUME:
                orderTypeMatch = order instanceof VolumeOrder;
                plugin = this.volumePlugin;
                break;
            case ATTACHMENT:
                orderTypeMatch = order instanceof AttachmentOrder;
                plugin = this.attachmentPlugin;
                break;
            case PUBLIC_IP:
                orderTypeMatch = order instanceof PublicIpOrder;
                plugin = this.publicIpPlugin;
                break;
            default:
                throw new UnexpectedException(String.format(Messages.Exception.UNSUPPORTED_REQUEST_TYPE, order.getType()));
        }
        if (!orderTypeMatch) {
            throw new UnexpectedException(Messages.Exception.MISMATCHING_RESOURCE_TYPE);
        }
        return plugin;
    }

    public void switchOffAuditing() {
        this.auditRequestsOn = false;
    }

    protected void auditRequest(Operation operation, ResourceType resourceType, SystemUser systemUser,
                              String response) throws UnexpectedException {
        if (this.auditRequestsOn) {
            String userId = null, identityProviderId = null;
            if (systemUser != null) {
                userId = systemUser.getId();
                identityProviderId = systemUser.getIdentityProviderId();
            }

            Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
            AuditableRequest auditableRequest = new AuditableRequest(currentTimestamp, operation, resourceType, userId, identityProviderId, response);
            DatabaseManager.getInstance().auditRequest(auditableRequest);
        }
    }
    
}