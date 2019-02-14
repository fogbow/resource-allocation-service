package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.attachment.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.HttpRequestClientUtil;
import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.instances.AttachmentInstance;
import cloud.fogbow.ras.core.models.instances.InstanceState;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.plugins.interoperability.AttachmentPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackHttpClient;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackHttpToFogbowExceptionMapper;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9.CloudStackVolumePlugin;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;

import java.util.Properties;

public class CloudStackAttachmentPlugin implements AttachmentPlugin {
    private static final Logger LOGGER = Logger.getLogger(CloudStackVolumePlugin.class);

    private static final String SEPARATOR_ID = " ";
    protected static final String ATTACHMENT_ID_FORMAT = "%s %s";
    protected static final int JOB_STATUS_COMPLETE = 1;
    protected static final int JOB_STATUS_PENDING = 0;    
    protected static final int JOB_STATUS_FAILURE = 2;
    private static final String PENDING_STATE = "pending";
    private static final String FAILURE_STATE = "failure";
    private static final String CLOUDSTACK_URL = "cloudstack_api_url";

    private CloudStackHttpClient client;
    private Properties properties;
    private String cloudStackUrl;
    
    public CloudStackAttachmentPlugin() {
        Integer timeout = new Integer(PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.HTTP_REQUEST_TIMEOUT_KEY,
                ConfigurationPropertyDefaults.XMPP_TIMEOUT));
        HttpRequestClientUtil client = new HttpRequestClientUtil(timeout);
        this.client = new CloudStackHttpClient(client);
    }

    public CloudStackAttachmentPlugin(String confFilePath) {
        this();
        this.properties = PropertiesUtil.readProperties(confFilePath);
        this.cloudStackUrl = this.properties.getProperty(CLOUDSTACK_URL);
    }

    @Override
    public String requestInstance(AttachmentOrder attachmentOrder, CloudToken localUserAttributes)
            throws FogbowException {
        
        String virtualMachineId = attachmentOrder.getComputeId();
        String volumeId = attachmentOrder.getVolumeId();
        
        AttachVolumeRequest request = new AttachVolumeRequest.Builder()
                .id(volumeId)
                .virtualMachineId(virtualMachineId)
                .build(this.cloudStackUrl);
        
        CloudStackUrlUtil.sign(request.getUriBuilder(), localUserAttributes.getTokenValue());
        
        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), localUserAttributes);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowExceptionMapper.map(e);
        }
        
        AttachVolumeResponse response = AttachVolumeResponse.fromJson(jsonResponse);
        
        if (response.getJobId() != null) {
            String jobId = response.getJobId();
            String attachmentId = String.format(ATTACHMENT_ID_FORMAT, volumeId, jobId);
            return attachmentId;
        } else {
            throw new UnexpectedException();
        }
    }

    @Override
    public void deleteInstance(String attachmentInstanceId, CloudToken localUserAttributes) throws FogbowException {
        
        String[] separatorInstanceId = attachmentInstanceId.split(SEPARATOR_ID);
        String volumeId = separatorInstanceId[0];
        
        DetachVolumeRequest request = new DetachVolumeRequest.Builder()
                .id(volumeId)
                .build(this.cloudStackUrl);
        
        CloudStackUrlUtil.sign(request.getUriBuilder(), localUserAttributes.getTokenValue());
        
        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), localUserAttributes);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowExceptionMapper.map(e);
        }
        
        DetachVolumeResponse response = DetachVolumeResponse.fromJson(jsonResponse);
        
        if (response.getJobId() == null) {
            throw new UnexpectedException();
        }
    }

    @Override
    public AttachmentInstance getInstance(String attachmentInstanceId, CloudToken localUserAttributes)
            throws FogbowException {
        
        String[] separatorInstanceId = attachmentInstanceId.split(SEPARATOR_ID);
        String jobId = separatorInstanceId[1];

        AttachmentJobStatusRequest request = new AttachmentJobStatusRequest.Builder()
                .jobId(jobId)
                .build(this.cloudStackUrl);
        
        CloudStackUrlUtil.sign(request.getUriBuilder(), localUserAttributes.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), localUserAttributes);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowExceptionMapper.map(e);
        }
        
        AttachmentJobStatusResponse response = AttachmentJobStatusResponse.fromJson(jsonResponse);
        
        return loadInstanceByJobStatus(attachmentInstanceId, response);
    }

    private AttachmentInstance loadInstanceByJobStatus(String attachmentInstanceId,
            AttachmentJobStatusResponse response) throws UnexpectedException {
        
        InstanceState instanceState;
        int status = response.getJobStatus();
        switch (status) {
            case JOB_STATUS_PENDING:
                instanceState = CloudStackStateMapper.map(ResourceType.ATTACHMENT, PENDING_STATE);
                return new AttachmentInstance(attachmentInstanceId, instanceState, null, null, null);

            case JOB_STATUS_COMPLETE:
                AttachmentJobStatusResponse.Volume volume = response.getVolume();
                return mountInstance(volume);
                
            case JOB_STATUS_FAILURE:
                instanceState = CloudStackStateMapper.map(ResourceType.ATTACHMENT, FAILURE_STATE);
                return new AttachmentInstance(attachmentInstanceId, instanceState, null, null, null);
                
            default:
                throw new UnexpectedException();
        }
    }

    private AttachmentInstance mountInstance(AttachmentJobStatusResponse.Volume volume) {
        String source = volume.getVirtualMachineId();
        String target = volume.getId();
        String jobId = volume.getJobId();
        String attachmentId = String.format(ATTACHMENT_ID_FORMAT, target, jobId);
        String device = String.valueOf(volume.getDeviceId());
        String state = volume.getState();
        
        InstanceState instanceState = CloudStackStateMapper.map(ResourceType.ATTACHMENT, state);
        
        AttachmentInstance attachmentInstance = new AttachmentInstance(attachmentId, instanceState, source, target, device);
        return attachmentInstance;
    }

    protected void setClient(CloudStackHttpClient client) {
        this.client = client;
    }

}
