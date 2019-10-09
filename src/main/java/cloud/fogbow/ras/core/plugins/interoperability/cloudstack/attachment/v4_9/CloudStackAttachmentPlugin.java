package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.attachment.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpToFogbowExceptionMapper;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.api.http.response.AttachmentInstance;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.plugins.interoperability.AttachmentPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackStateMapper;
import com.google.common.annotations.VisibleForTesting;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;

import javax.validation.constraints.NotNull;
import java.util.Properties;

public class CloudStackAttachmentPlugin implements AttachmentPlugin<CloudStackUser> {
    private static final Logger LOGGER = Logger.getLogger(CloudStackAttachmentPlugin.class);

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
    public String requestInstance(@NotNull AttachmentOrder attachmentOrder,
                                  @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE_FROM_PROVIDER));
        String virtualMachineId = attachmentOrder.getComputeId();
        String volumeId = attachmentOrder.getVolumeId();

        AttachVolumeRequest request = new AttachVolumeRequest.Builder()
                .id(volumeId)
                .virtualMachineId(virtualMachineId)
                .build(this.cloudStackUrl);

        return doRequestInstance(request, cloudStackUser);
    }

    @Override
    public void deleteInstance(@NotNull AttachmentOrder attachmentOrder,
                               @NotNull CloudStackUser cloudStackUser) throws FogbowException {

        LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE_S, attachmentOrder.getInstanceId()));
        String volumeId = attachmentOrder.getVolumeId();
        DetachVolumeRequest request = new DetachVolumeRequest.Builder()
                .id(volumeId)
                .build(this.cloudStackUrl);

        doDeleteInstance(request, cloudStackUser);
    }

    @Override
    public AttachmentInstance getInstance(@NotNull AttachmentOrder attachmentOrder,
                                          @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE_S, attachmentOrder.getInstanceId()));
        String jobId = attachmentOrder.getInstanceId();
        AttachmentJobStatusRequest request = new AttachmentJobStatusRequest.Builder()
                .jobId(jobId)
                .build(this.cloudStackUrl);

        return doGetInstance(attachmentOrder, request, cloudStackUser);
    }

    @NotNull
    @VisibleForTesting
    AttachmentInstance doGetInstance(@NotNull AttachmentOrder attachmentOrder,
                                     @NotNull AttachmentJobStatusRequest request,
                                     @NotNull CloudStackUser cloudStackUser) throws FogbowException {

        URIBuilder uriRequest = request.getUriBuilder();
        CloudStackUrlUtil.sign(uriRequest, cloudStackUser.getToken());

        try {
            String jsonResponse = this.client.doGetRequest(uriRequest.toString(), cloudStackUser);
            AttachmentJobStatusResponse response = AttachmentJobStatusResponse.fromJson(jsonResponse);
            return loadInstanceByJobStatus(attachmentOrder.getInstanceId(), response);
        } catch (HttpResponseException e) {
            throw CloudStackHttpToFogbowExceptionMapper.get(e);
        }
    }

    @VisibleForTesting
    void doDeleteInstance(@NotNull DetachVolumeRequest request ,
                          @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        URIBuilder uriRequest = request.getUriBuilder();
        CloudStackUrlUtil.sign(uriRequest, cloudStackUser.getToken());

        try {
            String jsonResponse = CloudStackCloudUtils.doGet(
                    this.client, uriRequest.toString(), cloudStackUser);
            DetachVolumeResponse response = DetachVolumeResponse.fromJson(jsonResponse);

            if (response.getJobId() == null) {
                throw new UnexpectedException();
            }
        } catch (HttpResponseException e) {
            throw CloudStackHttpToFogbowExceptionMapper.get(e);
        }
    }

    @NotNull
    @VisibleForTesting
    String doRequestInstance(@NotNull AttachVolumeRequest request,
                             @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        URIBuilder uriRequest = request.getUriBuilder();
        CloudStackUrlUtil.sign(uriRequest, cloudStackUser.getToken());

        try {
            String jsonResponse = CloudStackCloudUtils.doGet(
                    this.client, uriRequest.toString(), cloudStackUser);
            AttachVolumeResponse response = AttachVolumeResponse.fromJson(jsonResponse);
            return response.getJobId();
        } catch (HttpResponseException e) {
            throw CloudStackHttpToFogbowExceptionMapper.get(e);
        }
    }

    private AttachmentInstance loadInstanceByJobStatus(String attachmentInstanceId,
                                       AttachmentJobStatusResponse response) throws UnexpectedException {
        
        int status = response.getJobStatus();
        switch (status) {
            case CloudStackCloudUtils.JOB_STATUS_PENDING:
                return new AttachmentInstance(attachmentInstanceId, CloudStackCloudUtils.PENDING_STATE,
                        null, null, null);
            case CloudStackCloudUtils.JOB_STATUS_COMPLETE:
                AttachmentJobStatusResponse.Volume volume = response.getVolume();
                return mountInstance(volume);
            case CloudStackCloudUtils.JOB_STATUS_FAILURE:
                return new AttachmentInstance(attachmentInstanceId, CloudStackCloudUtils.FAILURE_STATE,
                        null, null, null);
            default:
                throw new UnexpectedException();
        }
    }

    private AttachmentInstance mountInstance(AttachmentJobStatusResponse.Volume volume) {
        String source = volume.getVirtualMachineId();
        String target = volume.getId();
        String jobId = volume.getJobId();
        String device = String.valueOf(volume.getDeviceId());
        String state = volume.getState();

        AttachmentInstance attachmentInstance = new AttachmentInstance(jobId, state, source, target, device);
        return attachmentInstance;
    }

    @VisibleForTesting
    void setClient(CloudStackHttpClient client) {
        this.client = client;
    }
}
