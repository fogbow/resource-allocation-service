package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.attachment.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.api.http.response.AttachmentInstance;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.plugins.interoperability.AttachmentPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackErrorResponse;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.attachment.model.*;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.volume.model.GetVolumeRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.volume.model.GetVolumeResponse;

import com.google.common.annotations.VisibleForTesting;

import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;

import java.util.Properties;

public class CloudStackAttachmentPlugin implements AttachmentPlugin<CloudStackUser> {
    private static final Logger LOGGER = Logger.getLogger(CloudStackAttachmentPlugin.class);

    @VisibleForTesting
    static final String FAILED_ATTACH_ERROR_MESSAGE = "code: %s, description: %s.";

    private CloudStackHttpClient client;
    private String cloudStackUrl;

    public CloudStackAttachmentPlugin(String confFilePath) {
        this.client = new CloudStackHttpClient();
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.cloudStackUrl = properties.getProperty(CloudStackCloudUtils.CLOUDSTACK_URL_CONFIG);
    }

    @Override
    public boolean isReady(String cloudState) {
        return CloudStackStateMapper.map(ResourceType.ATTACHMENT, cloudState).equals(InstanceState.READY);
    }

    @Override
    public boolean hasFailed(String cloudState) {
        return CloudStackStateMapper.map(ResourceType.ATTACHMENT, cloudState).equals(InstanceState.FAILED);
    }

    @Override
    public String requestInstance(AttachmentOrder attachmentOrder,
                                  CloudStackUser cloudStackUser)
            throws FogbowException {

        LOGGER.info(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER);

        String virtualMachineId = attachmentOrder.getComputeId();
        String volumeId = attachmentOrder.getVolumeId();
        AttachVolumeRequest request = new AttachVolumeRequest.Builder()
                .id(volumeId)
                .virtualMachineId(virtualMachineId)
                .build(this.cloudStackUrl);

        return doRequestInstance(request, cloudStackUser);
    }

    @Override
    public void deleteInstance(AttachmentOrder attachmentOrder,
                               CloudStackUser cloudStackUser) throws FogbowException {

        LOGGER.info(String.format(Messages.Log.DELETING_INSTANCE_S, attachmentOrder.getInstanceId()));

        String volumeId = attachmentOrder.getVolumeId();
        DetachVolumeRequest request = new DetachVolumeRequest.Builder()
                .id(volumeId)
                .build(this.cloudStackUrl);

        doDeleteInstance(request, cloudStackUser);
    }

    @Override
    public AttachmentInstance getInstance(AttachmentOrder attachmentOrder,
                                          CloudStackUser cloudStackUser)
            throws FogbowException {

        LOGGER.info(String.format(Messages.Log.GETTING_INSTANCE_S, attachmentOrder.getInstanceId()));

        String jobId = attachmentOrder.getInstanceId();
        AttachmentJobStatusRequest request = new AttachmentJobStatusRequest.Builder()
                .jobId(jobId)
                .build(this.cloudStackUrl);

        return doGetInstance(attachmentOrder, request, cloudStackUser);
    }

    @VisibleForTesting
    AttachmentInstance doGetInstance(AttachmentOrder attachmentOrder,
                                     AttachmentJobStatusRequest request,
                                     CloudStackUser cloudStackUser) throws FogbowException {

        URIBuilder uriRequest = request.getUriBuilder();
        CloudStackUrlUtil.sign(uriRequest, cloudStackUser.getToken());

        String jsonResponse = CloudStackCloudUtils.doRequest(this.client, uriRequest.toString(), cloudStackUser);
        AttachmentJobStatusResponse response = AttachmentJobStatusResponse.fromJson(jsonResponse);
        return createInstanceByJobStatus(attachmentOrder, response, cloudStackUser);
    }

    @VisibleForTesting
    void doDeleteInstance(DetachVolumeRequest request ,
                          CloudStackUser cloudStackUser)
            throws FogbowException {

        URIBuilder uriRequest = request.getUriBuilder();
        CloudStackUrlUtil.sign(uriRequest, cloudStackUser.getToken());

        String jsonResponse = CloudStackCloudUtils.doRequest(this.client, uriRequest.toString(), cloudStackUser);
        DetachVolumeResponse.fromJson(jsonResponse);
    }

    @VisibleForTesting
    String doRequestInstance(AttachVolumeRequest request,
                             CloudStackUser cloudStackUser)
            throws FogbowException {

        URIBuilder uriRequest = request.getUriBuilder();
        CloudStackUrlUtil.sign(uriRequest, cloudStackUser.getToken());

        String jsonResponse = CloudStackCloudUtils.doRequest(this.client, uriRequest.toString(), cloudStackUser);
        AttachVolumeResponse response = AttachVolumeResponse.fromJson(jsonResponse);
        return response.getJobId();
    }

    @VisibleForTesting
    AttachmentInstance createInstanceByJobStatus(
            AttachmentOrder attachmentOrder,
            AttachmentJobStatusResponse response,
            CloudStackUser cloudStackUser) throws FogbowException {
        
        int status = response.getJobStatus();
        switch (status) {
            case CloudStackCloudUtils.JOB_STATUS_PENDING:
                return createInstance(response.getJobId(), CloudStackCloudUtils.PENDING_STATE);
            case CloudStackCloudUtils.JOB_STATUS_COMPLETE:
                /*
                 * The jobId used as an identifier in the verification of the status
                 * of completion of the processing of the association of the
                 * resources does not include in the response content, data that
                 * confirm its dissociation, being necessary to check together with
                 * the cloud if the resources are still associated.
                 */
                checkVolumeAttached(attachmentOrder, cloudStackUser);
                AttachmentJobStatusResponse.Volume volume = response.getVolume();
                return buildAttachmentInstance(volume);
            case CloudStackCloudUtils.JOB_STATUS_FAILURE:
                logFailure(response);
                return createInstance(response.getJobId(), CloudStackCloudUtils.FAILURE_STATE);
            default:
                throw new InternalServerErrorException(Messages.Exception.UNEXPECTED_JOB_STATUS);
        }
    }

    @VisibleForTesting
    void checkVolumeAttached(
            AttachmentOrder attachmentOrder,
            CloudStackUser cloudStackUser) throws FogbowException {

        GetVolumeRequest request = buildGetVolumeRequest(attachmentOrder);
        URIBuilder uriRequest = request.getUriBuilder();
        CloudStackUrlUtil.sign(uriRequest, cloudStackUser.getToken());

        String jsonResponse = CloudStackCloudUtils.doRequest(this.client, uriRequest.toString(), cloudStackUser);
        GetVolumeResponse response = GetVolumeResponse.fromJson(jsonResponse);
        if (response.getVolumes() == null) {
            throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
        }
    }

    @VisibleForTesting
    GetVolumeRequest buildGetVolumeRequest(AttachmentOrder attachmentOrder) throws FogbowException {
        GetVolumeRequest request = new GetVolumeRequest.Builder()
                .id(attachmentOrder.getVolumeId())
                .virtualMachineId(attachmentOrder.getComputeId())
                .build(this.cloudStackUrl);

        return request;
    }

    @VisibleForTesting
    void logFailure(AttachmentJobStatusResponse response) throws InternalServerErrorException {
        CloudStackErrorResponse errorResponse = response.getErrorResponse();
        String errorText = String.format(FAILED_ATTACH_ERROR_MESSAGE,
                errorResponse.getErrorCode(), errorResponse.getErrorText());
        LOGGER.error(String.format(Messages.Log.ERROR_WHILE_ATTACHING_VOLUME_GENERAL_S, errorText));
    }

    private AttachmentInstance buildAttachmentInstance(AttachmentJobStatusResponse.Volume volume) {
        String source = volume.getVirtualMachineId();
        String target = volume.getId();
        String jobId = volume.getJobId();
        String device = String.valueOf(volume.getDeviceId());
        String state = volume.getState();

        return new AttachmentInstance(jobId, state, source, target, device);
    }

    private AttachmentInstance createInstance(String attachmentInstanceId, String state) {
        return new AttachmentInstance(attachmentInstanceId, state,null, null, null);
    }

    @VisibleForTesting
    void setClient(CloudStackHttpClient client) {
        this.client = client;
    }
}
