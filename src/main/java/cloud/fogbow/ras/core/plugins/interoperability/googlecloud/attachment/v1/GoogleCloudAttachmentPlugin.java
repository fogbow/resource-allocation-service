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

// TODO test
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
        String endpointBase = getPrefixEndpoint(projectId)
                + getZoneEndpoint(this.zone);

        String volumeId = attachmentOrder.getVolumeId();
        String volumeSource = createSourcePath(volumeId, projectId);
        // note(jadsonluan): Google Cloud Platform only accepts the device name.
        // This device name will reflect into the "/dev/disk/by-id/google-{deviceName}"
        String device = this.getDeviceName(attachmentOrder.getDevice());
        String jsonRequest = generateJsonRequest(volumeSource, device);

        CreateAttachmentResponse operation = doRequestInstance(endpointBase
                        + getInstanceEndpoint(serverId)
                        + GoogleCloudConstants.ATTACH_DISK_KEY_ENDPOINT,
                jsonRequest, cloudUser);

        return operation.getId();
    }

    @VisibleForTesting
    String getDeviceName(String device) {
        return device.replace(GoogleCloudConstants.Attachment.DEVICE_DEV_PREFIX, GoogleCloudConstants.EMPTY_STRING);
    }

    //TODO - 404 error when detaching. Delete method and query params may be the source of the problem.
    @Override
    public void deleteInstance(AttachmentOrder attachmentOrder, GoogleCloudUser cloudUser) throws FogbowException {
        String instanceId = attachmentOrder.getInstanceId();
        LOGGER.info(String.format(Messages.Log.DELETING_INSTANCE_S, instanceId));

        String projectId = GoogleCloudPluginUtils.getProjectIdFrom(cloudUser);
        String serverId = attachmentOrder.getComputeId();
        String device = this.getDeviceName(attachmentOrder.getDevice());
        String endpoint = getPrefixEndpoint(projectId)
                + getZoneEndpoint(this.zone)
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
        String endpoint = getPrefixEndpoint(projectId)
                + getZoneEndpoint(this.zone);

        GetAttachmentResponse response = doGetInstance(endpoint, volumeId, serverId, cloudUser);
        AttachmentInstance attachmentInstance = buildAttachmentInstanceFrom(response);

        return attachmentInstance;
    }

    @VisibleForTesting
    AttachmentInstance buildAttachmentInstanceFrom(GetAttachmentResponse response) {
        String computeId = response.getServerId();
        String id = response.getId();
        String volumeId = response.getVolumeId();
        String deviceName = response.getDevice();
        String device = this.formatDevice(deviceName);

        /*
         * There is no GoogleCloudState for attachments; we set it to empty string to
         * allow its mapping by the OpenStackStateMapper.map() function.
         */
        String googleCloudState = GoogleCloudConstants.EMPTY_STRING;
        AttachmentInstance attachmentInstance = new AttachmentInstance(id, googleCloudState, computeId, volumeId, device);
        return attachmentInstance;
    }

    private String formatDevice(String deviceName) {
        return String.format(GoogleCloudConstants.Attachment.DEVICE_FORMAT, deviceName);
    }

    @VisibleForTesting
    GetAttachmentResponse doGetInstance(String endpointBase, String volumeId, String serverId, GoogleCloudUser cloudUser)
            throws FogbowException {
        String jsonResponse = this.client.doGetRequest(endpointBase + getDiskEndpoint(volumeId), cloudUser);
        GetAttachmentResponse response = doGetAttachmentResponseFrom(jsonResponse);
        String deviceName = findDeviceNameFromVolume(endpointBase + getInstanceEndpoint(serverId),
                volumeId, cloudUser);
        response.setDevice(deviceName);

        return response;
    }


    @VisibleForTesting
    String findDeviceNameFromVolume(String endpoint, String volumeName, GoogleCloudUser cloudUser)
            throws FogbowException {
        String json = doGetResponseFromCloud(endpoint, cloudUser);
        GetInstanceResponse instanceResponse = doGetInstanceResponseFrom(json);
        List<GetInstanceResponse.Disk> disks = instanceResponse.getDisks();

        for (GetInstanceResponse.Disk disk : disks){
            boolean match = false;
            String diskVolumeName = disk.getVolumeName();
            if (diskVolumeName.equals(volumeName)) {
                match = true;
            }

            if (match) return disk.getDeviceName();
        }

        String message = Messages.Exception.UNABLE_TO_MATCH_REQUIREMENTS;
        throw new UnacceptableOperationException(message);
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
    String getZoneEndpoint(String zone){
        return GoogleCloudConstants.ZONES_ENDPOINT
                + GoogleCloudConstants.ENDPOINT_SEPARATOR
                + zone;
    }

    @VisibleForTesting
    String getDiskEndpoint(String volumeId){
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
                + getZoneEndpoint(this.zone)
                + GoogleCloudConstants.VOLUME_ENDPOINT
                + GoogleCloudConstants.ENDPOINT_SEPARATOR
                + resource;
    }

    private void initClient() {
        this.client = new GoogleCloudHttpClient();
    }

}