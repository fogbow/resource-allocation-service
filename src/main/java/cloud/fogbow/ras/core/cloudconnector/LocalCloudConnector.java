package cloud.fogbow.ras.core.cloudconnector;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.util.connectivity.FogbowGenericResponse;
import cloud.fogbow.ras.api.http.response.*;
import cloud.fogbow.ras.api.http.response.quotas.ComputeQuota;
import cloud.fogbow.ras.api.http.response.quotas.Quota;
import cloud.fogbow.ras.api.http.response.securityrules.SecurityRule;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.InteroperabilityPluginInstantiator;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.auditing.AuditableRequest;
import cloud.fogbow.ras.core.models.orders.*;
import cloud.fogbow.ras.core.plugins.interoperability.*;
import cloud.fogbow.ras.core.plugins.interoperability.GenericRequestPlugin;
import cloud.fogbow.ras.core.plugins.mapper.SystemToCloudMapperPlugin;
import org.apache.log4j.Logger;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

public class LocalCloudConnector implements CloudConnector {
    private static final Logger LOGGER = Logger.getLogger(LocalCloudConnector.class);

    private SystemToCloudMapperPlugin mapperPlugin;
    private PublicIpPlugin publicIpPlugin;
    private AttachmentPlugin attachmentPlugin;
    private ComputePlugin computePlugin;
    private ComputeQuotaPlugin computeQuotaPlugin;
    private NetworkPlugin networkPlugin;
    private VolumePlugin volumePlugin;
    private ImagePlugin imagePlugin;
    private SecurityRulePlugin securityRulePlugin;
    private GenericRequestPlugin genericRequestPlugin;

    private boolean auditRequestsOn = true;

    public LocalCloudConnector(String cloudName) {
        InteroperabilityPluginInstantiator instantiator = new InteroperabilityPluginInstantiator();
        this.attachmentPlugin = instantiator.getAttachmentPlugin(cloudName);
        this.computePlugin = instantiator.getComputePlugin(cloudName);
        this.computeQuotaPlugin = instantiator.getComputeQuotaPlugin(cloudName);
        this.networkPlugin = instantiator.getNetworkPlugin(cloudName);
        this.volumePlugin = instantiator.getVolumePlugin(cloudName);
//        this.imagePlugin = instantiator.getImagePlugin(cloudName);
        this.publicIpPlugin = instantiator.getPublicIpPlugin(cloudName);
        this.securityRulePlugin = instantiator.getSecurityRulePlugin(cloudName);
        this.genericRequestPlugin = instantiator.getGenericRequestPlugin(cloudName);
        this.mapperPlugin = instantiator.getSystemToCloudMapperPlugin(cloudName);
    }

    @Override
    public String requestInstance(Order order) throws FogbowException {
        CloudUser cloudUser = this.mapperPlugin.map(order.getSystemUser());

        String response = null;
        try {
            response = doRequestInstance(order, cloudUser);
        } catch (Throwable e) {
            response = e.getClass().getName();
            throw e;
        } finally {
            auditRequest(Operation.CREATE, order.getType(), order.getSystemUser(), response);
        }

        return response;
    }

    @Override
    public void deleteInstance(Order order) throws FogbowException {
        CloudUser cloudUser = this.mapperPlugin.map(order.getSystemUser());

        String response = null;
        try {
            doDeleteInstance(order, cloudUser);
        } catch (Throwable e) {
            response = e.getClass().getName();
            throw e;
        } finally {
            auditRequest(Operation.DELETE, order.getType(), order.getSystemUser(), response);
        }
    }

    @Override
    public Instance getInstance(Order order) throws FogbowException {
        CloudUser token = this.mapperPlugin.map(order.getSystemUser());

        String auditableResponse = null;
        Instance instance = null;
        try {
            instance = doGetInstance(order, token);
            instance.setState(InstanceStatus.mapInstanceStateFromOrderState(order.getOrderState()));
            if (instance != null) {
                auditableResponse = instance.toString();
            }
        } catch (Throwable e) {
            auditableResponse = e.getClass().getName();
            throw e;
        } finally {
            auditRequest(Operation.GET, order.getType(), order.getSystemUser(), auditableResponse);
        }

        return instance;
    }

    @Override
    public Quota getUserQuota(SystemUser systemUser, ResourceType resourceType) throws FogbowException {
        CloudUser cloudUser = this.mapperPlugin.map(systemUser);

        String auditableResponse = null;
        Quota quota = null;
        try {
            quota = doGetUserQuota(cloudUser, resourceType);
            if (quota != null) {
                auditableResponse = quota.toString();
            }
        } catch (Throwable e) {
            auditableResponse = e.getClass().getName();
            throw e;
        } finally {
            auditRequest(Operation.GET_USER_QUOTA, resourceType, systemUser, auditableResponse);
        }

        return quota;
    }

    @Override
    public Map<String, String> getAllImages(SystemUser systemUser) throws FogbowException {
        CloudUser cloudUser = this.mapperPlugin.map(systemUser);

        Map<String, String> images = null;
        String auditableResponse = null;
        try {
            images = doGetAllImages(cloudUser);
            if (images != null) {
                auditableResponse = images.toString();
            }
        } catch (Throwable e) {
            auditableResponse = e.getClass().getName();
            throw e;
        } finally {
            auditRequest(Operation.GET_ALL, ResourceType.IMAGE, systemUser, auditableResponse);
        }

        return images;
    }

    @Override
    public Image getImage(String imageId, SystemUser systemUser) throws FogbowException {
        CloudUser cloudUser = this.mapperPlugin.map(systemUser);

        Image image = null;
        String auditableResponse = null;
        try {
            image = doGetImage(imageId, cloudUser);
            if (image != null) {
                auditableResponse = image.toString();
            }
        } catch (Throwable e) {
            auditableResponse = e.getClass().getName();
            throw e;
        } finally {
            auditRequest(Operation.GET, ResourceType.IMAGE, systemUser, auditableResponse);
        }

        return image;
    }

    @Override
    public FogbowGenericResponse genericRequest(String genericRequest, SystemUser systemUser) throws FogbowException {
        CloudUser cloudUser = this.mapperPlugin.map(systemUser);

        FogbowGenericResponse fogbowGenericResponse = null;
        String auditableResponse = null;
        try {
            fogbowGenericResponse = doGenericRequest(genericRequest, cloudUser);
            if (fogbowGenericResponse != null) {
                auditableResponse = fogbowGenericResponse.toString();
            }
        } catch (Throwable e) {
            auditableResponse = e.getClass().getName();
            throw e;
        } finally {
            auditRequest(Operation.CREATE, ResourceType.GENERIC_RESOURCE, systemUser, auditableResponse);
        }

        return fogbowGenericResponse;
    }

    @Override
    public List<SecurityRule> getAllSecurityRules(Order order, SystemUser systemUser) throws FogbowException {
        CloudUser cloudUser = this.mapperPlugin.map(systemUser);

        List<SecurityRule> securityRules = null;
        String auditableResponse = null;
        try {
            securityRules = doGetAllSecurityRules(order, cloudUser);
            if (securityRules != null) {
                auditableResponse = securityRules.toString();
            }
        } catch (Exception e) {
            auditableResponse = e.getClass().getName();
            throw e;
        } finally {
            auditRequest(Operation.GET_ALL, order.getType(), systemUser, auditableResponse);
        }

        return securityRules;
    }

    @Override
    public String requestSecurityRule(Order order, SecurityRule securityRule, SystemUser systemUser) throws FogbowException {
        CloudUser cloudUser = this.mapperPlugin.map(systemUser);

        String response = null;
        try {
            response = doRequestSecurityRule(order, securityRule, cloudUser);
        } catch (Exception e) {
            response = e.getClass().getName();
            throw e;
        } finally {
            auditRequest(Operation.CREATE, order.getType(), systemUser, response);
        }

        return response;
    }

    @Override
    public void deleteSecurityRule(String securityRuleId, SystemUser systemUser) throws FogbowException {
        CloudUser cloudUser = this.mapperPlugin.map(systemUser);

        String response = null;
        try {
            doDeleteSecurityRule(securityRuleId, cloudUser);
        } catch (Exception e) {
            response = e.getClass().getName();
            throw e;
        } finally {
            auditRequest(Operation.DELETE, ResourceType.SECURITY_RULE, systemUser, response);
        }
    }

    private String doRequestInstance(Order order, CloudUser cloudUser) throws FogbowException {
        String instanceId;
        OrderPlugin plugin = checkOrderCastingAndSetPlugin(order, order.getType());
        instanceId = plugin.requestInstance(order, cloudUser);
        if (instanceId == null) {
            throw new UnexpectedException(Messages.Exception.NULL_VALUE_RETURNED);
        }
        return instanceId;
    }

    private void doDeleteInstance(Order order, CloudUser cloudUser) throws FogbowException {
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
            return;
        }
    }

    private Instance doGetInstance(Order order, CloudUser cloudUser) throws FogbowException {
        Instance instance;
        String instanceId = order.getInstanceId();
        if (instanceId != null) {
            instance = getResourceInstance(order, order.getType(), cloudUser);
        } else {
            // When there is no instance, an empty one is created with the appropriate state
            instance = createEmptyInstance(order);
        }
        return instance;
    }

    private Instance createEmptyInstance(Order order) throws UnexpectedException {
        Instance instance = null;
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

    private Instance getResourceInstance(Order order, ResourceType resourceType, CloudUser cloudUser) throws FogbowException {
        OrderPlugin plugin = checkOrderCastingAndSetPlugin(order, resourceType);
        Instance instance = plugin.getInstance(order, cloudUser);
        boolean instanceHasFailed = plugin.hasFailed(instance.getCloudState());
        boolean instanceIsReady = plugin.isReady(instance.getCloudState());
        if (instanceHasFailed) instance.setHasFailed();
        if (instanceIsReady) instance.setReady();
        return instance;
    }

    private Quota doGetUserQuota(CloudUser token, ResourceType resourceType) throws FogbowException {
        switch (resourceType) {
            case COMPUTE:
                ComputeQuota userQuota = this.computeQuotaPlugin.getUserQuota(token);
                return userQuota;
            default:
                throw new UnexpectedException(String.format(Messages.Exception.QUOTA_ENDPOINT_NOT_IMPLEMENTED, resourceType));
        }
    }

    private Map<String, String> doGetAllImages(CloudUser token) throws FogbowException {
        return this.imagePlugin.getAllImages(token);
    }

    private Image doGetImage(String imageId, CloudUser token) throws FogbowException {
        return this.imagePlugin.getImage(imageId, token);
    }

    private FogbowGenericResponse doGenericRequest(String genericRequest, CloudUser token)
            throws FogbowException {
        return this.genericRequestPlugin.redirectGenericRequest(genericRequest, token);
    }

    private List<SecurityRule> doGetAllSecurityRules(Order order, CloudUser token)
            throws FogbowException {
        return this.securityRulePlugin.getSecurityRules(order, token);
    }

    private String doRequestSecurityRule(Order order, SecurityRule securityRule, CloudUser token)
            throws FogbowException {
        return this.securityRulePlugin.requestSecurityRule(securityRule, order, token);
    }

    private void doDeleteSecurityRule(String securityRuleId, CloudUser token) throws FogbowException {
        this.securityRulePlugin.deleteSecurityRule(securityRuleId, token);
    }

    private OrderPlugin checkOrderCastingAndSetPlugin(Order order, ResourceType resourceType)
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

    // Used only in tests

    protected void setMapperPlugin(SystemToCloudMapperPlugin mapperPlugin) {
        this.mapperPlugin = mapperPlugin;
    }

    protected void setPublicIpPlugin(PublicIpPlugin publicIpPlugin) {
        this.publicIpPlugin = publicIpPlugin;
    }

    protected void setAttachmentPlugin(AttachmentPlugin attachmentPlugin) {
        this.attachmentPlugin = attachmentPlugin;
    }

    protected void setComputePlugin(ComputePlugin computePlugin) {
        this.computePlugin = computePlugin;
    }

    protected void setComputeQuotaPlugin(ComputeQuotaPlugin computeQuotaPlugin) {
        this.computeQuotaPlugin = computeQuotaPlugin;
    }

    protected void setNetworkPlugin(NetworkPlugin networkPlugin) {
        this.networkPlugin = networkPlugin;
    }

    protected void setVolumePlugin(VolumePlugin volumePlugin) {
        this.volumePlugin = volumePlugin;
    }

    protected void setImagePlugin(ImagePlugin imagePlugin) {
        this.imagePlugin = imagePlugin;
    }

    protected void setGenericRequestPlugin(GenericRequestPlugin genericRequestPlugin) {
        this.genericRequestPlugin = genericRequestPlugin;
    }

    private void auditRequest(Operation operation, ResourceType resourceType, SystemUser systemUser,
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