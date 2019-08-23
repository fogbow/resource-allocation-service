package cloud.fogbow.ras.core.plugins.interoperability.openstack.attachment.v2;

import java.util.Properties;

import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.json.JSONException;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnauthenticatedUserException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpToFogbowExceptionMapper;
import cloud.fogbow.ras.api.http.response.AttachmentInstance;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.plugins.interoperability.AttachmentPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackStateMapper;

public class OpenStackAttachmentPlugin implements AttachmentPlugin<OpenStackV3User> {
    
    private static final Logger LOGGER = Logger.getLogger(OpenStackAttachmentPlugin.class);

    protected static final String COMPUTE_NOVAV2_URL_KEY = "openstack_nova_v2_url";
    protected static final String COMPUTE_V2_API_ENDPOINT = "/v2/";
    protected static final String OS_VOLUME_ATTACHMENTS = "/os-volume_attachments";
    protected static final String SERVERS = "/servers/";
    
    private Properties properties;
    private OpenStackHttpClient client;

    public OpenStackAttachmentPlugin(String confFilePath) throws FatalErrorException {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        initClient();
    }

    @Override
    public boolean isReady(String cloudState) {
        return OpenStackStateMapper.map(ResourceType.ATTACHMENT, cloudState).equals(InstanceState.READY);
    }

    @Override
    public boolean hasFailed(String cloudState) {
        return OpenStackStateMapper.map(ResourceType.ATTACHMENT, cloudState).equals(InstanceState.FAILED);
    }

    @Override
    public String requestInstance(AttachmentOrder attachmentOrder, OpenStackV3User cloudUser) throws FogbowException {
        String projectId = cloudUser.getProjectId();
        String serverId = attachmentOrder.getComputeId();
        String endpoint = getPrefixEndpoint(projectId) + SERVERS + serverId + OS_VOLUME_ATTACHMENTS;

        String volumeId = attachmentOrder.getVolumeId();
        String device = attachmentOrder.getDevice();
        String jsonRequest = generateJsonRequestToAttach(volumeId, device);

        CreateAttachmentResponse response = doCreateAttachmentResponse(endpoint, jsonRequest, cloudUser);
        return response.getVolumeId();
    }

    @Override
    public void deleteInstance(AttachmentOrder order, OpenStackV3User cloudUser) throws FogbowException {
        if (order == null) {
            throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
        }
        String serverId = order.getComputeId();
        String volumeId = order.getVolumeId();
        String projectId = cloudUser.getProjectId();
        if (projectId == null) {
            String message = Messages.Error.UNSPECIFIED_PROJECT_ID;
            LOGGER.error(message);
            throw new UnauthenticatedUserException(message);
        }
        String endpoint = getPrefixEndpoint(projectId) + SERVERS + serverId + OS_VOLUME_ATTACHMENTS + "/" + volumeId;
        try {
            this.client.doDeleteRequest(endpoint, cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
    }

    @Override
    public AttachmentInstance getInstance(AttachmentOrder order, OpenStackV3User cloudUser) throws FogbowException {
        if (order == null) {
            throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
        }
        String serverId = order.getComputeId();
        String volumeId = order.getVolumeId();
        String projectId = cloudUser.getProjectId();
        String requestEndpoint = getPrefixEndpoint(projectId) + SERVERS + serverId + OS_VOLUME_ATTACHMENTS + "/"
                + volumeId;
        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(requestEndpoint, cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
        AttachmentInstance attachmentInstance = getInstanceFromJson(jsonResponse);
        return attachmentInstance;
    }

    protected AttachmentInstance getInstanceFromJson(String jsonResponse) throws UnexpectedException {
        try {
            GetAttachmentResponse getAttachmentResponse = GetAttachmentResponse.fromJson(jsonResponse);
            String id = getAttachmentResponse.getId();
            String computeId = getAttachmentResponse.getServerId();
            String volumeId = getAttachmentResponse.getVolumeId();
            String device = getAttachmentResponse.getDevice();
            // There is no OpenStackState for attachments; we set it to empty string to
            // allow its mapping
            // by the OpenStackStateMapper.map() function.
            String openStackState = "";
            AttachmentInstance attachmentInstance = new AttachmentInstance(id, openStackState, computeId, volumeId,
                    device);
            return attachmentInstance;
        } catch (JSONException e) {
            String message = Messages.Error.UNABLE_TO_GET_ATTACHMENT_INSTANCE;
            LOGGER.error(message, e);
            throw new UnexpectedException(message, e);
        }
    }

    protected CreateAttachmentResponse doCreateAttachmentResponse(String endpoint, String jsonRequest,
            OpenStackV3User cloudUser) throws FogbowException {
        
        String jsonResponse = null;
        try {
            jsonResponse = this.client.doPostRequest(endpoint, jsonRequest, cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
        return CreateAttachmentResponse.fromJson(jsonResponse);
    }

    protected String generateJsonRequestToAttach(String volumeId, String device) throws FogbowException {
        CreateAttachmentRequest createAttachmentRequest = new CreateAttachmentRequest.Builder()
                .volumeId(volumeId)
                .device(device)
                .build();
        try {
            return createAttachmentRequest.toJson();
        } catch (JSONException e) {
            String message = Messages.Error.UNABLE_TO_GENERATE_JSON;
            LOGGER.error(message, e);
            throw new InvalidParameterException(message, e);
        }
    }
    
    private String getPrefixEndpoint(String projectId) {
        return this.properties.getProperty(COMPUTE_NOVAV2_URL_KEY) + COMPUTE_V2_API_ENDPOINT + projectId;
    }

    private void initClient() {
        this.client = new OpenStackHttpClient();
    }

    protected void setClient(OpenStackHttpClient client) {
        this.client = client;
    }

    protected void setProperties(Properties properties) {
        this.properties = properties;
    }
}
