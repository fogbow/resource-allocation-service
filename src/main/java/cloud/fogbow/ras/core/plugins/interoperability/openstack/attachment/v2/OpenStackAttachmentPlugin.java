package cloud.fogbow.ras.core.plugins.interoperability.openstack.attachment.v2;

import java.util.Properties;

import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.requests.CreateAttachmentRequest;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.responses.CreateAttachmentResponse;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.responses.GetAttachmentResponse;
import org.apache.log4j.Logger;

import com.google.gson.JsonSyntaxException;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.ras.api.http.response.AttachmentInstance;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.plugins.interoperability.AttachmentPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.OpenStackPluginUtils;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.OpenStackStateMapper;

public class OpenStackAttachmentPlugin implements AttachmentPlugin<OpenStackV3User> {
    
    private static final Logger LOGGER = Logger.getLogger(OpenStackAttachmentPlugin.class);

    protected static final String EMPTY_STRING = "";

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
        LOGGER.info(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER);
        String projectId = OpenStackPluginUtils.getProjectIdFrom(cloudUser);
        String serverId = attachmentOrder.getComputeId();
        String endpoint = getPrefixEndpoint(projectId)
                + OpenStackConstants.SERVERS_ENDPOINT
                + OpenStackConstants.ENDPOINT_SEPARATOR
                + serverId 
                + OpenStackConstants.OS_VOLUME_ATTACHMENTS;

        String volumeId = attachmentOrder.getVolumeId();
        String device = attachmentOrder.getDevice();
        String jsonRequest = generateJsonRequest(volumeId, device);

        CreateAttachmentResponse response = doRequestInstance(endpoint, jsonRequest, cloudUser);
        return response.getVolumeId();
    }

    @Override
    public void deleteInstance(AttachmentOrder attachmentOrder, OpenStackV3User cloudUser) throws FogbowException {
        String instanceId = attachmentOrder.getInstanceId();
        LOGGER.info(String.format(Messages.Log.DELETING_INSTANCE_S, instanceId));

        String projectId = OpenStackPluginUtils.getProjectIdFrom(cloudUser);
        String serverId = attachmentOrder.getComputeId();
        String volumeId = attachmentOrder.getVolumeId();
        String endpoint = getPrefixEndpoint(projectId) 
                + OpenStackConstants.SERVERS_ENDPOINT
                + OpenStackConstants.ENDPOINT_SEPARATOR
                + serverId 
                + OpenStackConstants.OS_VOLUME_ATTACHMENTS
                + OpenStackConstants.ENDPOINT_SEPARATOR
                + volumeId;
        
        doDeleteInstance(endpoint, cloudUser);
    }

    @Override
    public AttachmentInstance getInstance(AttachmentOrder order, OpenStackV3User cloudUser) throws FogbowException {
        String instanceId = order.getInstanceId();
        LOGGER.info(String.format(Messages.Log.GETTING_INSTANCE_S, instanceId));

        String projectId = OpenStackPluginUtils.getProjectIdFrom(cloudUser);
        String serverId = order.getComputeId();
        String volumeId = order.getVolumeId();
        String endpoint = getPrefixEndpoint(projectId) 
                + OpenStackConstants.SERVERS_ENDPOINT
                + OpenStackConstants.ENDPOINT_SEPARATOR
                + serverId 
                + OpenStackConstants.OS_VOLUME_ATTACHMENTS
                + OpenStackConstants.ENDPOINT_SEPARATOR
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
        String jsonResponse = this.client.doGetRequest(endpoint, cloudUser);
        return doGetAttachmentResponseFrom(jsonResponse);
    }

    protected GetAttachmentResponse doGetAttachmentResponseFrom(String json) throws InternalServerErrorException {
        try {
            return GetAttachmentResponse.fromJson(json);
        } catch (JsonSyntaxException e) {
            LOGGER.error(Messages.Log.UNABLE_TO_GET_ATTACHMENT_INSTANCE, e);
            throw new InternalServerErrorException(Messages.Exception.UNABLE_TO_GET_ATTACHMENT_INSTANCE);
        }
    }

    protected void doDeleteInstance(String endpoint, OpenStackV3User cloudUser) throws FogbowException {
        this.client.doDeleteRequest(endpoint, cloudUser);
    }

    protected CreateAttachmentResponse doRequestInstance(String endpoint, String jsonRequest,
            OpenStackV3User cloudUser) throws FogbowException {
        
        String jsonResponse = this.client.doPostRequest(endpoint, jsonRequest, cloudUser);
        return doCreateAttachmentResponseFrom(jsonResponse);
    }

    protected CreateAttachmentResponse doCreateAttachmentResponseFrom(String json) throws InternalServerErrorException {
        try {
            return CreateAttachmentResponse.fromJson(json);
        } catch (JsonSyntaxException e) {
            LOGGER.error(Messages.Log.UNABLE_TO_CREATE_ATTACHMENT, e);
            throw new InternalServerErrorException(Messages.Exception.UNABLE_TO_CREATE_ATTACHMENT);
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
        return this.properties.getProperty(OpenStackPluginUtils.COMPUTE_NOVA_URL_KEY) +
                OpenStackConstants.NOVA_V2_API_ENDPOINT + OpenStackConstants.ENDPOINT_SEPARATOR + projectId;
    }

    private void initClient() {
        this.client = new OpenStackHttpClient();
    }

    protected void setClient(OpenStackHttpClient client) {
        this.client = client;
    }
    
}
