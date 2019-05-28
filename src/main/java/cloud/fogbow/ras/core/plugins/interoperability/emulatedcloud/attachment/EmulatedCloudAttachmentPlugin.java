package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.attachment;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.NoAvailableResourcesException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.AttachmentInstance;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.plugins.interoperability.AttachmentPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudConstants;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.emulatedmodels.EmulatedAttachment;

import java.io.IOException;
import java.util.Properties;

public class EmulatedCloudAttachmentPlugin implements AttachmentPlugin<CloudUser> {

    private Properties properties;

    public EmulatedCloudAttachmentPlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
    }

    @Override
    public String requestInstance(AttachmentOrder attachmentOrder, CloudUser cloudUser) throws FogbowException {
        String compute = attachmentOrder.getComputeId();
        String volume = attachmentOrder.getVolumeId();
        String instanceId = EmulatedCloudUtils.getRandomUUID();

        EmulatedAttachment attachment = generateJsonEntityToCreateAttachment(compute, volume, instanceId);


        String newAttachmentPath = EmulatedCloudUtils.getResourcePath(this.properties, instanceId);

        try {
            EmulatedCloudUtils.saveFileContent(newAttachmentPath, attachment.toJson());
        } catch (IOException e) {
            throw new NoAvailableResourcesException(e.getMessage());
        }

        return instanceId;
    }

    @Override
    public void deleteInstance(AttachmentOrder attachmentOrder, CloudUser cloudUser) throws FogbowException {
        String attachmentId = attachmentOrder.getId();
        String attachmentPath = EmulatedCloudUtils.getResourcePath(this.properties, attachmentId);

        EmulatedCloudUtils.deleteFile(attachmentPath);
    }

    @Override
    public AttachmentInstance getInstance(AttachmentOrder attachmentOrder, CloudUser cloudUser) throws FogbowException {
        String instanceId = attachmentOrder.getInstanceId();

        String attachmentJson = null;
        try {
            attachmentJson = EmulatedCloudUtils.getFileContentById(this.properties, instanceId);
        } catch (IOException e) {
            throw new InstanceNotFoundException(e.getMessage());
        }

        EmulatedAttachment attachment = EmulatedAttachment.fromJson(attachmentJson);

        String cloudState = attachment.getCloudState();
        String computeId = attachment.getComputeId();
        String volumeId = attachment.getVolumeId();
        String device = attachment.getDevice();

        return new AttachmentInstance(instanceId, cloudState, computeId, volumeId, device);
    }

    @Override
    public boolean isReady(String instanceState) {
        return true;
    }

    @Override
    public boolean hasFailed(String instanceState) {
        return false;
    }

    private EmulatedAttachment generateJsonEntityToCreateAttachment(String compute, String volume, String instanceId){
        EmulatedAttachment emulatedAttachment = new EmulatedAttachment.Builder()
                .computeId(compute)
                .volumeId(volume)
                .instanceId(instanceId)
                .cloudState(EmulatedCloudConstants.Plugins.STATE_ACTIVE)
                .build();

        return  emulatedAttachment;
    }
}
