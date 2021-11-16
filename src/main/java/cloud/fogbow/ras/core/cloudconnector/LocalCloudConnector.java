package cloud.fogbow.ras.core.cloudconnector;

import java.sql.Timestamp;
import java.util.List;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.ras.api.http.response.*;
import cloud.fogbow.ras.core.models.orders.*;
import com.google.common.annotations.VisibleForTesting;
import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.api.http.response.quotas.Quota;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.InteroperabilityPluginInstantiator;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.auditing.AuditableRequest;
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
    private static final String GET_ALL_IMAGES_OPERATION = "getAllImages";
    private static final String GET_ALL_SECURITY_RULES_OPERATION = "getAllSecurityRules";
    private static final String GET_IMAGE_OPERATION = "getImage";
    private static final String GET_INSTANCE_OPERATION = "getInstance";
    private static final String GET_QUOTA_OPERATION = "getQuota";
    private static final String HIBERNATE_INSTANCE_OPERATION = "hibernateInstance";
    private static final String PAUSE_INSTANCE_OPERATION = "pauseInstance";
    private static final String STOP_INSTANCE_OPERATION = "stopInstance";
    private static final String RESUME_INSTANCE_OPERATION = "resumeInstance";
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
        LOGGER.debug(String.format(Messages.Log.MAPPING_USER_OP_S, REQUEST_INSTANCE_OPERATION, order));
        CloudUser cloudUser = this.mapperPlugin.map(order.getSystemUser());
        LOGGER.debug(String.format(Messages.Log.MAPPED_USER_S, cloudUser));

        String response = null;
        try {
            response = doRequestInstance(order, cloudUser);
            LOGGER.debug(String.format(Messages.Log.RESPONSE_RECEIVED_S, response));
        } catch (Throwable e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION_S, e + e.getMessage()));
            response = e.getClass().getName();
            throw e;
        } finally {
            auditRequest(Operation.CREATE, order.getType(), order.getSystemUser(), response);
        }

        return response;
    }

    @Override
    public void deleteInstance(Order order) throws FogbowException {
        LOGGER.debug(String.format(Messages.Log.MAPPING_USER_OP_S, DELETE_INSTANCE_OPERATION, order));
        CloudUser cloudUser = this.mapperPlugin.map(order.getSystemUser());
        LOGGER.debug(String.format(Messages.Log.MAPPED_USER_S, cloudUser));

        String response = null;
        try {
            doDeleteInstance(order, cloudUser);
            LOGGER.debug(Messages.Log.SUCCESS);
        } catch (Throwable e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION_S, e + e.getMessage()));
            response = e.getClass().getName();
            throw e;
        } finally {
            auditRequest(Operation.DELETE, order.getType(), order.getSystemUser(), response);
        }
    }

    @Override
    public OrderInstance getInstance(Order order) throws FogbowException {
        LOGGER.debug(String.format(Messages.Log.MAPPING_USER_OP_S, GET_INSTANCE_OPERATION, order));
        CloudUser cloudUser = this.mapperPlugin.map(order.getSystemUser());
        LOGGER.debug(String.format(Messages.Log.MAPPED_USER_S, cloudUser));

        String auditableResponse = null;
        OrderInstance instance = null;
        try {
            instance = doGetInstance(order, cloudUser);
            LOGGER.debug(String.format(Messages.Log.RESPONSE_RECEIVED_S, instance));
            instance.setState(InstanceStatus.mapInstanceStateFromOrderState(order.getOrderState(),
                    true, instance.isReady(), instance.hasFailed()));
            auditableResponse = instance.toString();
        } catch (Throwable e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION_S, e + e.getMessage()));
            auditableResponse = e.getClass().getName();
            throw e;
        } finally {
            auditRequest(Operation.GET, order.getType(), order.getSystemUser(), auditableResponse);
        }

        return instance;
    }

    @Override
    public Quota getUserQuota(SystemUser systemUser) throws FogbowException {
        LOGGER.debug(String.format(Messages.Log.MAPPING_USER_OP_S, GET_QUOTA_OPERATION, systemUser));
        CloudUser cloudUser = this.mapperPlugin.map(systemUser);
        LOGGER.debug(String.format(Messages.Log.MAPPED_USER_S, cloudUser));

        String auditableResponse = null;
        Quota quota = null;
        try {
            quota = this.quotaPlugin.getUserQuota(cloudUser);
            LOGGER.debug(String.format(Messages.Log.RESPONSE_RECEIVED_S, quota));
            auditableResponse = quota.toString();
        } catch (Throwable e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION_S, e + e.getMessage()));
            auditableResponse = e.getClass().getName();
            throw e;
        } finally {
            auditRequest(Operation.GET, ResourceType.QUOTA, systemUser, auditableResponse);
        }

        return quota;
    }

    @Override
    public List<ImageSummary> getAllImages(SystemUser systemUser) throws FogbowException {
        LOGGER.debug(String.format(Messages.Log.MAPPING_USER_OP_S, GET_ALL_IMAGES_OPERATION, systemUser));
        CloudUser cloudUser = this.mapperPlugin.map(systemUser);
        LOGGER.debug(String.format(Messages.Log.MAPPED_USER_S, cloudUser));

        List<ImageSummary> images = null;
        String auditableResponse = null;
        try {
            images = doGetAllImages(cloudUser);
            LOGGER.debug(String.format(Messages.Log.RESPONSE_RECEIVED_S, images));
            auditableResponse = images.toString();
        } catch (Throwable e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION_S, e + e.getMessage()));
            auditableResponse = e.getClass().getName();
            throw e;
        } finally {
            auditRequest(Operation.GET_ALL, ResourceType.IMAGE, systemUser, auditableResponse);
        }

        return images;
    }

    @Override
    public void takeSnapshot(ComputeOrder computeOrder, String name, SystemUser systemUser) throws FogbowException{
        //todo: fix auditing logic

        LOGGER.debug(String.format(Messages.Log.MAPPING_USER_OP_S, GET_IMAGE_OPERATION, systemUser));
        CloudUser cloudUser = this.mapperPlugin.map(systemUser);
        LOGGER.debug(String.format(Messages.Log.MAPPED_USER_S, cloudUser));

        //String auditableResponse = null;
        try {
            doTakeSnapshot(computeOrder, name, cloudUser);
            //auditableResponse = imageInstance.toString();
        } catch (Throwable e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION_S, e + e.getMessage()));
            //auditableResponse = e.getClass().getName();
            throw e;
        } /*finally {
            auditRequest(Operation.GET, ResourceType.IMAGE, systemUser, auditableResponse);
        }*/
    }

    @Override
    public ImageInstance getImage(String imageId, SystemUser systemUser) throws FogbowException {
        LOGGER.debug(String.format(Messages.Log.MAPPING_USER_OP_S, GET_IMAGE_OPERATION, systemUser));
        CloudUser cloudUser = this.mapperPlugin.map(systemUser);
        LOGGER.debug(String.format(Messages.Log.MAPPED_USER_S, cloudUser));

        ImageInstance imageInstance = null;
        String auditableResponse = null;
        try {
            imageInstance = doGetImage(imageId, cloudUser);
            LOGGER.debug(String.format(Messages.Log.RESPONSE_RECEIVED_S, imageInstance));
            auditableResponse = imageInstance.toString();
        } catch (Throwable e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION_S, e + e.getMessage()));
            auditableResponse = e.getClass().getName();
            throw e;
        } finally {
            auditRequest(Operation.GET, ResourceType.IMAGE, systemUser, auditableResponse);
        }

        return imageInstance;
    }

    @Override
    public List<SecurityRuleInstance> getAllSecurityRules(Order order, SystemUser systemUser) throws FogbowException {
        LOGGER.debug(String.format(Messages.Log.MAPPING_USER_OP_S, GET_ALL_SECURITY_RULES_OPERATION, order));
        CloudUser cloudUser = this.mapperPlugin.map(systemUser);
        LOGGER.debug(String.format(Messages.Log.MAPPED_USER_S, cloudUser));

        List<SecurityRuleInstance> securityRuleInstances = null;
        String auditableResponse = null;
        try {
            securityRuleInstances = doGetAllSecurityRules(order, cloudUser);
            LOGGER.debug(String.format(Messages.Log.RESPONSE_RECEIVED_S, securityRuleInstances));
            auditableResponse = securityRuleInstances.toString();
        } catch (Throwable e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION_S, e + e.getMessage()));
            auditableResponse = e.getClass().getName();
            throw e;
        } finally {
            auditRequest(Operation.GET_ALL, order.getType(), systemUser, auditableResponse);
        }

        return securityRuleInstances;
    }

    @Override
    public String requestSecurityRule(Order order, SecurityRule securityRule, SystemUser systemUser) throws FogbowException {
        LOGGER.debug(String.format(Messages.Log.MAPPING_USER_OP_S, REQUEST_SECURITY_RULES_OPERATION, order));
        CloudUser cloudUser = this.mapperPlugin.map(systemUser);
        LOGGER.debug(String.format(Messages.Log.MAPPED_USER_S, cloudUser));

        String response = null;
        try {
            response = doRequestSecurityRule(order, securityRule, cloudUser);
            LOGGER.debug(String.format(Messages.Log.RESPONSE_RECEIVED_S, response));
        } catch (Throwable e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION_S, e + e.getMessage()));
            response = e.getClass().getName();
            throw e;
        } finally {
            auditRequest(Operation.CREATE, order.getType(), systemUser, response);
        }

        return response;
    }

    @Override
    public void deleteSecurityRule(String securityRuleId, SystemUser systemUser) throws FogbowException {
        LOGGER.debug(String.format(Messages.Log.MAPPING_USER_OP_S, DELETE_SECURITY_RULE_OPERATION, securityRuleId));
        CloudUser cloudUser = this.mapperPlugin.map(systemUser);
        LOGGER.debug(String.format(Messages.Log.MAPPED_USER_S, cloudUser));

        String response = null;
        try {
            doDeleteSecurityRule(securityRuleId, cloudUser);
            LOGGER.debug(Messages.Log.SUCCESS);
        } catch (Throwable e) {
            response = e.getClass().getName();
            throw e;
        } finally {
            auditRequest(Operation.DELETE, ResourceType.SECURITY_RULE, systemUser, response);
        }
    }

    @Override
    public void pauseComputeInstance(Order order) throws FogbowException {
        LOGGER.debug(String.format(Messages.Log.MAPPING_USER_OP_S, PAUSE_INSTANCE_OPERATION, order));
        CloudUser cloudUser = this.mapperPlugin.map(order.getSystemUser());
        LOGGER.debug(String.format(Messages.Log.MAPPED_USER_S, cloudUser));

        String response = null;
        try {
            doPauseInstance(order, cloudUser);
            LOGGER.debug(Messages.Log.SUCCESS);
        } catch (Throwable e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION_S, e + e.getMessage()));
            response = e.getClass().getName();
            throw e;
        } finally {
            auditRequest(Operation.PAUSE, order.getType(), order.getSystemUser(), response);
        }
    }

    @Override
    public void hibernateComputeInstance(Order order) throws FogbowException {
        LOGGER.debug(String.format(Messages.Log.MAPPING_USER_OP_S, HIBERNATE_INSTANCE_OPERATION, order));
        CloudUser cloudUser = this.mapperPlugin.map(order.getSystemUser());
        LOGGER.debug(String.format(Messages.Log.MAPPED_USER_S, cloudUser));

        String response = null;
        try {
            doHibernateInstance(order, cloudUser);
            LOGGER.debug(Messages.Log.SUCCESS);
        } catch (Throwable e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION_S, e + e.getMessage()));
            response = e.getClass().getName();
            throw e;
        } finally {
            auditRequest(Operation.HIBERNATE, order.getType(), order.getSystemUser(), response);
        }
    }

    @Override
    public void stopComputeInstance(Order order) throws FogbowException {
        LOGGER.debug(String.format(Messages.Log.MAPPING_USER_OP_S, STOP_INSTANCE_OPERATION, order));
        CloudUser cloudUser = this.mapperPlugin.map(order.getSystemUser());
        LOGGER.debug(String.format(Messages.Log.MAPPED_USER_S, cloudUser));

        String response = null;
        try {
            doStopInstance(order, cloudUser);
            LOGGER.debug(Messages.Log.SUCCESS);
        } catch (Throwable e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION_S, e + e.getMessage()));
            response = e.getClass().getName();
            throw e;
        } finally {
            auditRequest(Operation.STOP, order.getType(), order.getSystemUser(), response);
        }
    }

    @Override
    public void resumeComputeInstance(Order order) throws FogbowException {
        LOGGER.debug(String.format(Messages.Log.MAPPING_USER_OP_S, RESUME_INSTANCE_OPERATION, order));
        CloudUser cloudUser = this.mapperPlugin.map(order.getSystemUser());
        LOGGER.debug(String.format(Messages.Log.MAPPED_USER_S, cloudUser));

        String response = null;
        try {
            ComputeOrder computeOrder = (ComputeOrder) order;
            doResumeInstance(computeOrder, cloudUser);
            LOGGER.debug(Messages.Log.SUCCESS);
        } catch (Throwable e) {
            LOGGER.debug(String.format(Messages.Exception.GENERIC_EXCEPTION_S, e + e.getMessage()));
            response = e.getClass().getName();
            throw e;
        } finally {
            auditRequest(Operation.RESUME, order.getType(), order.getSystemUser(), response);
        }
    }

    protected String doRequestInstance(Order order, CloudUser cloudUser) throws FogbowException {
        String instanceId;
        OrderPlugin plugin = checkOrderCastingAndSetPlugin(order, order.getType());
        instanceId = plugin.requestInstance(order, cloudUser);
        if (instanceId == null) {
            throw new InternalServerErrorException(Messages.Exception.NULL_VALUE_RETURNED);
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
            LOGGER.warn(String.format(Messages.Log.INSTANCE_S_ALREADY_DELETED, order.getId()));
            throw e;
        }
    }

    protected void doPauseInstance(Order order, CloudUser cloudUser) throws FogbowException {
        try {
            if (order.getInstanceId() != null) {
                this.computePlugin.pauseInstance((ComputeOrder) order, cloudUser);
            } else {
                return;
            }
        } catch (InstanceNotFoundException e) {
            LOGGER.warn(String.format(Messages.Log.INSTANCE_S_ALREADY_PAUSED, order.getId()));
            throw e;
        }
    }

    protected void doHibernateInstance(Order order, CloudUser cloudUser) throws FogbowException {
        try {
            if (order.getInstanceId() != null) {
                this.computePlugin.hibernateInstance((ComputeOrder) order, cloudUser);
            } else {
                return;
            }
        } catch (InstanceNotFoundException e) {
            LOGGER.warn(String.format(Messages.Log.INSTANCE_S_ALREADY_HIBERNATED, order.getId()));
            throw e;
        }
    }
    
    protected void doStopInstance(Order order, CloudUser cloudUser) throws FogbowException {
        try {
            if (order.getInstanceId() != null) {
                this.computePlugin.stopInstance((ComputeOrder) order, cloudUser);
            } else {
                return;
            }
        } catch (InstanceNotFoundException e) {
            LOGGER.warn(String.format(Messages.Log.INSTANCE_S_ALREADY_STOPPED, order.getId()));
            throw e;
        }
    }

    protected void doResumeInstance(ComputeOrder order, CloudUser cloudUser) throws FogbowException {
        try {
            if (order.getInstanceId() != null) {
                this.computePlugin.resumeInstance(order, cloudUser);
            } else {
                return;
            }
        } catch (InstanceNotFoundException e) {
            LOGGER.warn(String.format(Messages.Log.INSTANCE_S_ALREADY_RUNNING, order.getId()));
            throw e;
        }
    }

    protected OrderInstance doGetInstance(Order order, CloudUser cloudUser) throws FogbowException {
        String instanceId = order.getInstanceId();
        if (instanceId != null) {
            return getResourceInstance(order, order.getType(), cloudUser);
        } else if (order.getOrderState().equals(OrderState.CHECKING_DELETION)) {
            // The instance has been deleted, so, instead of returning an empty instance, InstanceNotFoundException
            // is thrown.
            throw new InstanceNotFoundException();
        } else {
            // When there is no instance and the instance was not deleted, an empty one is created
            // with the appropriate state.
            return EmptyOrderInstanceGenerator.createEmptyInstance(order);
        }
    }

    protected void doTakeSnapshot(ComputeOrder computeOrder, String name, CloudUser cloudUser) throws FogbowException {
        this.computePlugin.takeSnapshot(computeOrder, name, cloudUser);
    }

    private OrderInstance getResourceInstance(Order order, ResourceType resourceType, CloudUser cloudUser) throws FogbowException {
        OrderPlugin plugin = checkOrderCastingAndSetPlugin(order, resourceType);
        OrderInstance instance = plugin.getInstance(order, cloudUser);
        if (instance != null) {
            boolean instanceHasFailed = plugin.hasFailed(instance.getCloudState());
            boolean instanceIsReady = plugin.isReady(instance.getCloudState());
            if (instanceHasFailed) instance.setHasFailed();
            if (instanceIsReady) instance.setReady();
            return checkInstanceSpecificStatus(instance, resourceType);
        } else {
            throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
        }
    }

    @VisibleForTesting
    OrderInstance checkInstanceSpecificStatus(OrderInstance instance, ResourceType resourceType) throws FogbowException {
        switch (resourceType) {
            case COMPUTE:
                boolean isPaused = this.computePlugin.isPaused(instance.getCloudState());
                boolean isHibernated = this.computePlugin.isHibernated(instance.getCloudState());
                boolean isStopped = this.computePlugin.isStopped(instance.getCloudState());
                ComputeInstance computeInstance = (ComputeInstance) instance;
                if (isPaused) computeInstance.setPaused();
                if (isHibernated) computeInstance.setHibernated();
                if (isStopped) computeInstance.setStopped();
                return computeInstance;
            default:
                return instance;
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
            throws InternalServerErrorException {
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
                throw new InternalServerErrorException(String.format(Messages.Exception.UNSUPPORTED_REQUEST_TYPE_S, order.getType()));
        }
        if (!orderTypeMatch) {
            throw new InternalServerErrorException(Messages.Exception.MISMATCHING_RESOURCE_TYPE);
        }
        return plugin;
    }

    public void switchOffAuditing() {
        this.auditRequestsOn = false;
    }

    protected void auditRequest(Operation operation, ResourceType resourceType, SystemUser systemUser,
                              String response) throws InternalServerErrorException {
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
