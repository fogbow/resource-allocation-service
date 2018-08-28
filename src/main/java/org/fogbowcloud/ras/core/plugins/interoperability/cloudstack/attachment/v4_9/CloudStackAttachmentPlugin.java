package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.attachment.v4_9;

import java.util.List;
import org.apache.http.client.HttpResponseException;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InstanceNotFoundException;
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
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetVolumeRequest;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetVolumeResponse;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetVolumeResponse.Volume;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;

public class CloudStackAttachmentPlugin implements AttachmentPlugin<CloudStackToken>{

    private static final String ATTACHMENT_ID_FORMAT = "%s %s";
    private static final String SEPARATOR_ID = " ";
    private static final int FIRST_ELEMENT_POSITION = 0;
    
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
//            String attachmentId = response.getJobId();
            String attachmentId = String.format(ATTACHMENT_ID_FORMAT, volumeId, virtualMachineId);
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
        String volumeId = separatorInstanceId[0];
        String virtualMachineId = separatorInstanceId[1];
        
        GetVolumeRequest request = new GetVolumeRequest.Builder()
                .id(volumeId)
                .build();

        CloudStackUrlUtil.sign(request.getUriBuilder(), localUserAttributes.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), localUserAttributes);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowRasExceptionMapper.map(e);
        }

        GetVolumeResponse response = GetVolumeResponse.fromJson(jsonResponse);
        List<GetVolumeResponse.Volume> volumes = response.getVolumes();
        
        if (volumes != null && volumes.size() > 0) {
            // since an id were specified, there should be no more than one volume in the response
            Volume volume = volumes.get(FIRST_ELEMENT_POSITION);
            if (volume.getAttached() != null) {
                return loadInstance(volume, virtualMachineId); 
            } else {
                throw new InstanceNotFoundException();
            }
        } else {
            throw new InstanceNotFoundException();
        }
    }

    private AttachmentInstance loadInstance(Volume volume, String source) {
        String target = volume.getId();
        String attachmentId = String.format(ATTACHMENT_ID_FORMAT, target, source);
        String device = String.valueOf(volume.getDevice());
        String state = volume.getState();
        
        InstanceState instanceState = CloudStackStateMapper.map(ResourceType.VOLUME, state);

        AttachmentInstance attachmentInstance = new AttachmentInstance(attachmentId, instanceState, source, target, device);
        return attachmentInstance;
    }

}
