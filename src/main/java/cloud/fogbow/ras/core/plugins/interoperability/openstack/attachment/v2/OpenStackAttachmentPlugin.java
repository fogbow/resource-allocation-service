package cloud.fogbow.ras.core.plugins.interoperability.openstack.attachment.v2;

import java.util.Properties;

import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;

import com.google.gson.JsonSyntaxException;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
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
    protected static final String V2_API_ENDPOINT = "/v2/";
    protected static final String EMPTY_STRING = "";
    protected static final String ENDPOINT_SEPARATOR = "/";
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
        String projectId = getProjectIdFrom(cloudUser);
        String serverId = attachmentOrder.getComputeId();
        String endpoint = getPrefixEndpoint(projectId) 
                + SERVERS 
                + serverId 
                + OS_VOLUME_ATTACHMENTS;

        String volumeId = attachmentOrder.getVolumeId();
        String device = attachmentOrder.getDevice();
        String jsonRequest = generateJsonRequest(volumeId, device);

        CreateAttachmentResponse response = doRequestInstance(endpoint, jsonRequest, cloudUser);
        return response.getVolumeId();
    }

    @Override
    public void deleteInstance(AttachmentOrder attachmentOrder, OpenStackV3User cloudUser) throws FogbowException {
        String projectId = getProjectIdFrom(cloudUser);
        String serverId = attachmentOrder.getComputeId();
        String volumeId = attachmentOrder.getVolumeId();
        String endpoint = getPrefixEndpoint(projectId) 
                + SERVERS 
                + serverId 
                + OS_VOLUME_ATTACHMENTS 
                + ENDPOINT_SEPARATOR
                + volumeId;
        
        doDeleteInstance(endpoint, cloudUser);
    }

    @Override
    public AttachmentInstance getInstance(AttachmentOrder order, OpenStackV3User cloudUser) throws FogbowException {
        String projectId = getProjectIdFrom(cloudUser);
        String serverId = order.getComputeId();
        String volumeId = order.getVolumeId();
        String endpoint = getPrefixEndpoint(projectId) 
                + SERVERS 
                + serverId 
                + OS_VOLUME_ATTACHMENTS 
                + ENDPOINT_SEPARATOR 
                + volumeId;
        
        GetAttachmentResponse response = doGetInstance(endpoint, cloudUser);
        AttachmentInstance attachmentInstance = buildAttachmentInstanceFrom(response);
        return attachmentInstance;
    }

    protected AttachmentInstance buildAttachmentInstanceFrom(GetAttachmentResponse response) {
        String id = response.getId();
        String computeId = response.getServerId();
        String volumeId = response.getVolumeId();
        String device = response.getDevice();

        /*
         * There is no OpenStackState for attachments; we set it to empty string to
         * allow its mapping by the OpenStackStateMapper.map() function.
         */
        String openStackState = EMPTY_STRING;
        AttachmentInstance attachmentInstance = new AttachmentInstance(id, openStackState, computeId, volumeId, device);
        return attachmentInstance;
    }

    protected GetAttachmentResponse doGetInstance(String endpoint, OpenStackV3User cloudUser) throws FogbowException {
        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(endpoint, cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
        return doGetAttachmentResponseFrom(jsonResponse);
    }

    protected GetAttachmentResponse doGetAttachmentResponseFrom(String json) throws UnexpectedException {
        try {
            return GetAttachmentResponse.fromJson(json);
        } catch (JsonSyntaxException e) {
            String message = Messages.Error.UNABLE_TO_GET_ATTACHMENT_INSTANCE;
            LOGGER.error(message, e);
            throw new UnexpectedException(message, e);
        }
    }

    protected void doDeleteInstance(String endpoint, OpenStackV3User cloudUser) throws FogbowException {
        try {
            this.client.doDeleteRequest(endpoint, cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
    }

    protected String getProjectIdFrom(OpenStackV3User cloudUser) throws UnauthenticatedUserException {
        String projectId = cloudUser.getProjectId();
        if (projectId == null) {
            String message = Messages.Error.UNSPECIFIED_PROJECT_ID;
            LOGGER.error(message);
            throw new UnauthenticatedUserException(message);
        }
        return projectId;
    }

    protected CreateAttachmentResponse doRequestInstance(String endpoint, String jsonRequest,
            OpenStackV3User cloudUser) throws FogbowException {
        
        String jsonResponse = null;
        try {
            jsonResponse = this.client.doPostRequest(endpoint, jsonRequest, cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
        return doCreateAttachmentResponseFrom(jsonResponse);
    }

    protected CreateAttachmentResponse doCreateAttachmentResponseFrom(String json) throws UnexpectedException {
        try {
            return CreateAttachmentResponse.fromJson(json);
        } catch (JsonSyntaxException e) {
            String message = Messages.Error.UNABLE_TO_CREATE_ATTACHMENT;
            LOGGER.error(message, e);
            throw new UnexpectedException(message, e);
        }
    }

    protected String generateJsonRequest(String volumeId, String device) {
        CreateAttachmentRequest request = new CreateAttachmentRequest.Builder()
                .volumeId(volumeId)
                .device(device)
                .build();
        
        return request.toJson();
    }
    
    protected String getPrefixEndpoint(String projectId) {
        return this.properties.getProperty(COMPUTE_NOVAV2_URL_KEY) + V2_API_ENDPOINT + projectId;
    }

    private void initClient() {
        this.client = new OpenStackHttpClient();
    }

    protected void setClient(OpenStackHttpClient client) {
        this.client = client;
    }
    
}
