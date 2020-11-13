package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.attachment.v1;

import cloud.fogbow.common.constants.GoogleCloudConstants;
import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.models.GoogleCloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.googlecloud.GoogleCloudHttpClient;
import cloud.fogbow.ras.api.http.response.AttachmentInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.plugins.interoperability.AttachmentPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.attachment.CreateAttachmentRequest;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.attachment.CreateAttachmentResponse;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.attachment.GetAttachmentResponse;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.util.GoogleCloudPluginUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonSyntaxException;
import org.apache.log4j.Logger;

import java.util.Properties;

public class GoogleCloudAttachmentPlugin implements AttachmentPlugin<GoogleCloudUser> {
    private static final Logger LOGGER = Logger.getLogger(GoogleCloudAttachmentPlugin.class);

    @VisibleForTesting
    static final String EMPTY_STRING = "";

    private Properties properties;
    private GoogleCloudHttpClient client;

    public GoogleCloudAttachmentPlugin(String confFilePath) throws FatalErrorException {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        initClient();
    }

    @Override
    public boolean isReady(String cloudState) {
        return false; //TODO
    }

    @Override
    public boolean hasFailed(String cloudState) {
        return false; //TODO
    }

    @Override
    public String requestInstance(AttachmentOrder attachmentOrder, GoogleCloudUser cloudUser) throws FogbowException {
        LOGGER.info(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER);
        String projectId = GoogleCloudPluginUtils.getProjectIdFrom(cloudUser);
        String serverId = attachmentOrder.getComputeId();
        String endpoint = getPrefixEndpoint(projectId)
                + GoogleCloudConstants.ZONES_ENDPOINT
                + cloud.fogbow.common.constants.GoogleCloudConstants.ENDPOINT_SEPARATOR
                + cloud.fogbow.common.constants.GoogleCloudConstants.DEFAULT_ZONE
                + GoogleCloudConstants.INSTANCES_ENDPOINT
                + cloud.fogbow.common.constants.GoogleCloudConstants.ENDPOINT_SEPARATOR
                + serverId
                + cloud.fogbow.common.constants.GoogleCloudConstants.ATTACH_DISK_KEY_ENDPOINT;

        String volumeId = attachmentOrder.getVolumeId();
        String volumeSource = createSourcePath(volumeId, projectId);
        String device = attachmentOrder.getDevice();
        String jsonRequest = generateJsonRequest(volumeSource, device);
        //  TODO - Google Cloud compatible response
        CreateAttachmentResponse response = doRequestInstance(endpoint, jsonRequest, cloudUser);
        return response.getAttachmentName();
    }



    @Override
    public void deleteInstance(AttachmentOrder attachmentOrder, GoogleCloudUser cloudUser) throws FogbowException {
        String instanceId = attachmentOrder.getInstanceId();
        LOGGER.info(String.format(Messages.Log.DELETING_INSTANCE_S, instanceId));

        String projectId = GoogleCloudPluginUtils.getProjectIdFrom(cloudUser);
        String serverId = attachmentOrder.getComputeId();
        String volumeId = attachmentOrder.getVolumeId();
        String endpoint = getPrefixEndpoint(projectId)
                + getZoneEndpoint(cloud.fogbow.common.constants.GoogleCloudConstants.DEFAULT_ZONE)
                + getInstanceEndpoint(serverId)
                + cloud.fogbow.common.constants.GoogleCloudConstants.DETACH_DISK_KEY_ENDPOINT
                + cloud.fogbow.common.constants.GoogleCloudConstants.DEVICE_NAME_QUERY_PARAM
                + volumeId;

        doDeleteInstance(endpoint, cloudUser);
    }

    @Override
    public AttachmentInstance getInstance(AttachmentOrder order, GoogleCloudUser cloudUser) throws FogbowException {
        String instanceId = order.getInstanceId();
        LOGGER.info(String.format(Messages.Log.GETTING_INSTANCE_S, instanceId));

        String projectId = GoogleCloudPluginUtils.getProjectIdFrom(cloudUser);
        String serverId = order.getComputeId();
        String volumeId = order.getVolumeId();
        String endpoint = getPrefixEndpoint(projectId)
                + getZoneEndpoint(cloud.fogbow.common.constants.GoogleCloudConstants.DEFAULT_ZONE)
                + getInstanceEndpoint(serverId)
                + cloud.fogbow.common.constants.GoogleCloudConstants.DETACH_DISK_KEY_ENDPOINT
                + cloud.fogbow.common.constants.GoogleCloudConstants.DEVICE_NAME_QUERY_PARAM
                + volumeId;

//        TODO - Google Cloud compatible response
        GetAttachmentResponse response = doGetInstance(endpoint, cloudUser);
        AttachmentInstance attachmentInstance = buildAttachmentInstanceFrom(response);
        return attachmentInstance;
    }

    @VisibleForTesting
    AttachmentInstance buildAttachmentInstanceFrom(GetAttachmentResponse response) {
        String id = response.getId();
        String computeId = response.getServerId();
        String volumeId = response.getVolumeId();
        String device = response.getDevice();

        // TODO - Can I use 'status' from Cloud response?
        String googleCloudState = EMPTY_STRING;
        AttachmentInstance attachmentInstance = new AttachmentInstance(id, googleCloudState, computeId, volumeId, device);
        return attachmentInstance;
    }


    @VisibleForTesting
    GetAttachmentResponse doGetInstance(String endpoint, GoogleCloudUser cloudUser) throws FogbowException {
        return null;    //TODO
    }


    @VisibleForTesting
    GetAttachmentResponse doGetAttachmentResponseFrom(String json) throws InternalServerErrorException {
        try {
            return GetAttachmentResponse.fromJson(json);
        } catch (JsonSyntaxException e) {
            LOGGER.error(Messages.Log.UNABLE_TO_GET_ATTACHMENT_INSTANCE, e);
            throw new InternalServerErrorException(Messages.Exception.UNABLE_TO_GET_ATTACHMENT_INSTANCE);
        }
    }

    @VisibleForTesting
    void doDeleteInstance(String endpoint, GoogleCloudUser cloudUser) throws FogbowException {
        this.client.doDeleteRequest(endpoint, cloudUser);
    }

    @VisibleForTesting
    String generateJsonRequest(String volumeSource, String device) {
        CreateAttachmentRequest request = new CreateAttachmentRequest.Builder()
                .volumeSource(volumeSource)
                .device(device)
                .build();

        return request.toJson();
    }

    @VisibleForTesting
    CreateAttachmentResponse doRequestInstance(String endpoint, String jsonRequest, GoogleCloudUser cloudUser)
            throws FogbowException {
        return null;    // TODO
    }

    @VisibleForTesting
    CreateAttachmentResponse doCreateAttachmentResponseFrom(String json) throws InternalServerErrorException {
        try {
            return CreateAttachmentResponse.fromJson(json);
        } catch (JsonSyntaxException e) {
            LOGGER.error(Messages.Log.UNABLE_TO_CREATE_ATTACHMENT, e);
            throw new InternalServerErrorException(Messages.Exception.UNABLE_TO_CREATE_ATTACHMENT);
        }
    }

    @VisibleForTesting
    String getPrefixEndpoint(String projectId) {
        return this.properties.getProperty(GoogleCloudPluginUtils.VOLUME_COMPUTE_URL_KEY)
                + GoogleCloudConstants.COMPUTE_ENGINE_V1_ENDPOINT
                + GoogleCloudConstants.PROJECT_ENDPOINT
                + cloud.fogbow.common.constants.GoogleCloudConstants.ENDPOINT_SEPARATOR
                + projectId;
    }

    @VisibleForTesting
    String getZoneEndpoint(String zone){
        return GoogleCloudConstants.ZONES_ENDPOINT
                + cloud.fogbow.common.constants.GoogleCloudConstants.ENDPOINT_SEPARATOR
                + zone;
    }

    @VisibleForTesting
    String getInstanceEndpoint(String instance){
        return GoogleCloudConstants.INSTANCES_ENDPOINT
                + cloud.fogbow.common.constants.GoogleCloudConstants.ENDPOINT_SEPARATOR
                + instance;
    }

    @VisibleForTesting
    String createSourcePath(String resource, String projectId){
        return GoogleCloudConstants.INSTANCES_ENDPOINT
                + GoogleCloudConstants.COMPUTE_ENGINE_V1_ENDPOINT
                + GoogleCloudConstants.PROJECT_ENDPOINT
                + projectId
                + getZoneEndpoint(cloud.fogbow.common.constants.GoogleCloudConstants.DEFAULT_ZONE)
                + GoogleCloudConstants.VOLUME_ENDPOINT
                + cloud.fogbow.common.constants.GoogleCloudConstants.ENDPOINT_SEPARATOR
                + resource;
    }

    private void initClient() {
        this.client = new GoogleCloudHttpClient();
    }

    @VisibleForTesting
    void setClient(GoogleCloudHttpClient client) {
        this.client = client;
    }

}
