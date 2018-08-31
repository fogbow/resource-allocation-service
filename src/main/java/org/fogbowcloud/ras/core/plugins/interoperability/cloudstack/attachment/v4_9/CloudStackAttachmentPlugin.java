package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.attachment.v4_9;

import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.instances.AttachmentInstance;
import org.fogbowcloud.ras.core.models.instances.InstanceState;
import org.fogbowcloud.ras.core.models.orders.AttachmentOrder;
import org.fogbowcloud.ras.core.models.tokens.CloudStackToken;
import org.fogbowcloud.ras.core.plugins.interoperability.AttachmentPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackHttpToFogbowRasExceptionMapper;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackStateMapper;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.attachment.v4_9.AttachmentJobStatusResponse.Volume;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.volume.v4_9.CloudStackVolumePlugin;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;

public class CloudStackAttachmentPlugin implements AttachmentPlugin<CloudStackToken>{
    private static final Logger LOGGER = Logger.getLogger(CloudStackVolumePlugin.class);

    private static final String SEPARATOR_ID = " ";
    protected static final String ATTACHMENT_ID_FORMAT = "%s %s";
    protected static final int JOB_STATUS_COMPLETE = 1;
    
    private HttpRequestClientUtil client;
    
    public CloudStackAttachmentPlugin() {
        this.client = new HttpRequestClientUtil();
    }

    @Override
    public String requestInstance(AttachmentOrder attachmentOrder,
            CloudStackToken localUserAttributes) throws FogbowRasException, UnexpectedException {
        
        String virtualMachineId = attachmentOrder.getSource();
        String volumeId = attachmentOrder.getTarget();
        
        AttachVolumeRequest request = new AttachVolumeRequest.Builder()
                .id(volumeId)
                .virtualMachineId(virtualMachineId)
                .build();
        
        CloudStackUrlUtil.sign(request.getUriBuilder(), localUserAttributes.getTokenValue());
        
        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), localUserAttributes);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowRasExceptionMapper.map(e);
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
    public void deleteInstance(String attachmentInstanceId, CloudStackToken localUserAttributes)
            throws FogbowRasException, UnexpectedException {
        
        String[] separatorInstanceId = attachmentInstanceId.split(SEPARATOR_ID);
        String volumeId = separatorInstanceId[0];
        
        DetachVolumeRequest request = new DetachVolumeRequest.Builder()
                .id(volumeId)
                .build();
        
        CloudStackUrlUtil.sign(request.getUriBuilder(), localUserAttributes.getTokenValue());
        
        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), localUserAttributes);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowRasExceptionMapper.map(e);
        }
        
        DetachVolumeResponse response = DetachVolumeResponse.fromJson(jsonResponse);
        
        if (response.getJobId() == null) {
            throw new UnexpectedException();
        }
    }

    @Override
    public AttachmentInstance getInstance(String attachmentInstanceId,
            CloudStackToken localUserAttributes) throws FogbowRasException, UnexpectedException {
        
        String[] separatorInstanceId = attachmentInstanceId.split(SEPARATOR_ID);
        String jobId = separatorInstanceId[1];

        AttachmentJobStatusRequest request = new AttachmentJobStatusRequest.Builder()
                .jobId(jobId)
                .build();
        
        CloudStackUrlUtil.sign(request.getUriBuilder(), localUserAttributes.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), localUserAttributes);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowRasExceptionMapper.map(e);
        }
        
        AttachmentJobStatusResponse response = AttachmentJobStatusResponse.fromJson(jsonResponse);
        
        
        int status = response.getJobStatus();
        
        if (status == JOB_STATUS_COMPLETE) {
            Volume volume = response.getVolume();
            return loadInstance(volume);
        } else {
            throw new UnexpectedException();
        }
    }

    private AttachmentInstance loadInstance(Volume volume) {
        String source = volume.getVirtualMachineId();
        String target = volume.getId();
        String jobId = volume.getJobId();
        String attachmentId = String.format(ATTACHMENT_ID_FORMAT, target, jobId);
        String device = String.valueOf(volume.getDeviceId());
        String state = volume.getState();
        
        InstanceState instanceState = CloudStackStateMapper.map(ResourceType.VOLUME, state);
        
        AttachmentInstance attachmentInstance = new AttachmentInstance(attachmentId, instanceState, source, target, device);
        return attachmentInstance;
    }

    protected void setClient(HttpRequestClientUtil client) {
        this.client = client;
    }

}
