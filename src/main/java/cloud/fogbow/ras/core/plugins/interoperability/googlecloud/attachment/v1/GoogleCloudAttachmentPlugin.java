package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.attachment.v1;

import cloud.fogbow.common.exceptions.UnacceptableOperationException;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.attachment.models.GetInstanceResponse;
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
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.attachment.models.CreateAttachmentRequest;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.attachment.models.CreateAttachmentResponse;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.attachment.models.GetAttachmentResponse;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.util.GoogleCloudPluginUtils;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.util.GoogleCloudStateMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonSyntaxException;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Properties;

public class GoogleCloudAttachmentPlugin implements AttachmentPlugin<GoogleCloudUser> {
    private static final Logger LOGGER = Logger.getLogger(GoogleCloudAttachmentPlugin.class);

    static final String SPECIAL_EMPTY_BODY = "{null:null}";

    private Properties properties;
    private GoogleCloudHttpClient client;
    private final String zone;

    public GoogleCloudAttachmentPlugin(String confFilePath) throws FatalErrorException {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        this.zone = properties.getProperty(GoogleCloudConstants.ZONE_KEY_CONFIG);
        initClient();
    }

    @Override
    public boolean isReady(String cloudState) {
        return GoogleCloudStateMapper.map(ResourceType.ATTACHMENT, cloudState).equals(InstanceState.READY);
    }

    @Override
    public boolean hasFailed(String cloudState) {
        return GoogleCloudStateMapper.map(ResourceType.ATTACHMENT, cloudState).equals(InstanceState.FAILED);
    }

    @Override
    public String requestInstance(AttachmentOrder attachmentOrder, GoogleCloudUser cloudUser) throws FogbowException {
        LOGGER.info(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER);
        String projectId = GoogleCloudPluginUtils.getProjectIdFrom(cloudUser);
        String serverId = attachmentOrder.getComputeId();
        String endpoint = getPrefixEndpoint(projectId)
                + getZoneEndpoint()
                + getInstanceEndpoint(serverId)
                + GoogleCloudConstants.ATTACH_DISK_KEY_ENDPOINT;

        String volumeId = attachmentOrder.getVolumeId();
        String volumeSource = createSourcePath(volumeId, projectId);
        String device = attachmentOrder.getDevice();
        String jsonRequest = generateJsonRequest(volumeSource, device);

        doRequestInstance(endpoint, jsonRequest, cloudUser);

        return volumeId;
    }

    @Override
    public void deleteInstance(AttachmentOrder attachmentOrder, GoogleCloudUser cloudUser) throws FogbowException {
        String instanceId = attachmentOrder.getInstanceId();
        LOGGER.info(String.format(Messages.Log.DELETING_INSTANCE_S, instanceId));

        String projectId = GoogleCloudPluginUtils.getProjectIdFrom(cloudUser);
        String serverId = attachmentOrder.getComputeId();
        String device = attachmentOrder.getDevice();
        String endpoint = getPrefixEndpoint(projectId)
                + getZoneEndpoint()
                + getInstanceEndpoint(serverId)
                + GoogleCloudConstants.DETACH_DISK_KEY_ENDPOINT
                + GoogleCloudConstants.DEVICE_NAME_QUERY_PARAM
                + device;

        doDeleteInstance(endpoint, cloudUser);
    }

    @Override
    public AttachmentInstance getInstance(AttachmentOrder order, GoogleCloudUser cloudUser) throws FogbowException {
        String instanceId = order.getInstanceId();
        LOGGER.info(String.format(Messages.Log.GETTING_INSTANCE_S, instanceId));

        String projectId = GoogleCloudPluginUtils.getProjectIdFrom(cloudUser);
        String serverId = order.getComputeId();
        String volumeId = order.getVolumeId();
        String endpoint = getPrefixEndpoint(projectId) + getZoneEndpoint();

        GetAttachmentResponse response = doGetInstance(endpoint, volumeId, serverId, cloudUser);
        AttachmentInstance attachmentInstance = buildAttachmentInstanceFrom(response);

        return attachmentInstance;
    }

    @VisibleForTesting
    AttachmentInstance buildAttachmentInstanceFrom(GetAttachmentResponse response) {
        String computeId = response.getServerId();
        String id = response.getId();
        String volumeId = response.getVolumeName();
        String device = response.getDevice();

        /*
         * There is no GoogleCloudState for attachments; we set it to empty string to
         * allow its mapping by the GoogleCloudStateMapper.map() function.
         */
        String googleCloudState = GoogleCloudConstants.EMPTY_STRING;
        AttachmentInstance attachmentInstance = new AttachmentInstance(id, googleCloudState, computeId, volumeId, device);
        return attachmentInstance;
    }


    @VisibleForTesting
    GetAttachmentResponse doGetInstance(String endpointBase, String volumeId, String serverId, GoogleCloudUser cloudUser)
            throws FogbowException {

        String jsonResponse = this.client.doGetRequest(endpointBase + getVolumeEndpoint(volumeId), cloudUser);
        GetAttachmentResponse response = doGetAttachmentResponseFrom(jsonResponse);
        String deviceName = findDeviceNameFromVolume(endpointBase + getInstanceEndpoint(serverId),
                volumeId, cloudUser);
        response.setDevice(deviceName);
        response.setServerId(serverId);

        return response;
    }


    @VisibleForTesting
    String findDeviceNameFromVolume(String endpoint, String volumeName, GoogleCloudUser cloudUser)
            throws FogbowException {
        String json = doGetResponseFromCloud(endpoint, cloudUser);
        GetInstanceResponse instanceResponse = doGetInstanceResponseFrom(json);
        List<GetInstanceResponse.Disk> disks = instanceResponse.getDisks();

        String deviceName = null;
        for (GetInstanceResponse.Disk disk : disks){
            String diskVolumeName = disk.getVolumeName();
            if (diskVolumeName.equals(volumeName)) {
                deviceName = disk.getDeviceName();
                break;
            }
        }

        return deviceName;
    }


    @VisibleForTesting
    String doGetResponseFromCloud(String endpoint, GoogleCloudUser cloudUser) throws FogbowException {
        String jsonResponse = this.client.doGetRequest(endpoint, cloudUser);
        return jsonResponse;
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
    GetInstanceResponse doGetInstanceResponseFrom(String json) throws InternalServerErrorException {
        try {
            return GetInstanceResponse.fromJson(json);
        } catch (JsonSyntaxException e) {
            LOGGER.error(Messages.Log.UNABLE_TO_GET_ATTACHMENT_INSTANCE, e);
            throw new InternalServerErrorException(Messages.Exception.UNABLE_TO_GET_ATTACHMENT_INSTANCE);
        }
    }

    @VisibleForTesting
    void doDeleteInstance(String endpoint, GoogleCloudUser cloudUser) throws FogbowException {
        this.client.doPostRequest(endpoint, SPECIAL_EMPTY_BODY, cloudUser);
    }

    @VisibleForTesting
    CreateAttachmentResponse doRequestInstance(String endpoint, String jsonRequest, GoogleCloudUser cloudUser)
            throws FogbowException {
        String jsonResponse = this.client.doPostRequest(endpoint, jsonRequest, cloudUser);
        return doCreateAttachmentResponseFrom(jsonResponse);
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
    String generateJsonRequest(String volumeSource, String device) {
        CreateAttachmentRequest request = new CreateAttachmentRequest.Builder()
                .volumeSource(volumeSource)
                .device(device)
                .build();

        return request.toJson();
    }

    @VisibleForTesting
    String getPrefixEndpoint(String projectId) {
        return GoogleCloudConstants.BASE_COMPUTE_API_URL + GoogleCloudConstants.COMPUTE_ENGINE_V1_ENDPOINT
                + GoogleCloudConstants.PROJECT_ENDPOINT + GoogleCloudConstants.ENDPOINT_SEPARATOR + projectId;
    }

    @VisibleForTesting
    String getZoneEndpoint(){
        return GoogleCloudConstants.ZONES_ENDPOINT
                + GoogleCloudConstants.ENDPOINT_SEPARATOR
                + this.zone;
    }

    @VisibleForTesting
    String getVolumeEndpoint(String volumeId){
        return GoogleCloudConstants.VOLUME_ENDPOINT
                + GoogleCloudConstants.ENDPOINT_SEPARATOR
                + volumeId;
    }

    @VisibleForTesting
    String getInstanceEndpoint(String instance){
        return GoogleCloudConstants.INSTANCES_ENDPOINT
                + GoogleCloudConstants.ENDPOINT_SEPARATOR
                + instance;
    }

    @VisibleForTesting
    String createSourcePath(String resource, String projectId){
        return getPrefixEndpoint(projectId)
                + getZoneEndpoint()
                + getVolumeEndpoint(resource);
    }

    private void initClient() {
        this.client = new GoogleCloudHttpClient();
    }

}