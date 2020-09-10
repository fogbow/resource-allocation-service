package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.attachment;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.AttachmentInstance;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.plugins.interoperability.AttachmentPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.attachment.models.EmulatedAttachment;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.attachment.EmulatedCloudAttachmentManager;

import java.util.Optional;
import java.util.Properties;

public class EmulatedCloudAttachmentPlugin implements AttachmentPlugin<CloudUser> {

    private Properties properties;

    public EmulatedCloudAttachmentPlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
    }

    @Override
    public String requestInstance(AttachmentOrder attachmentOrder, CloudUser cloudUser) throws FogbowException {
        EmulatedCloudAttachmentManager attachmentManager = EmulatedCloudAttachmentManager.getInstance();
        EmulatedAttachment attachment = createEmulatedAttachment(attachmentOrder);
        String instanceId = attachmentManager.create(attachment);
        return instanceId;
    }

    @Override
    public void deleteInstance(AttachmentOrder attachmentOrder, CloudUser cloudUser) throws FogbowException {
        String attachmentId = attachmentOrder.getInstanceId();
        EmulatedCloudAttachmentManager attachmentManager = EmulatedCloudAttachmentManager.getInstance();
        attachmentManager.delete(attachmentId);
    }

    @Override
    public AttachmentInstance getInstance(AttachmentOrder attachmentOrder, CloudUser cloudUser) throws FogbowException {
        String instanceId = attachmentOrder.getInstanceId();
        EmulatedCloudAttachmentManager attachmentManager = EmulatedCloudAttachmentManager.getInstance();
        Optional<EmulatedAttachment> emulatedAttachment = attachmentManager.find(instanceId);

        if (emulatedAttachment.isPresent()) {
            return buildAttachmentInstance(emulatedAttachment.get());
        } else {
            throw new InstanceNotFoundException();
        }
    }

    @Override
    public boolean isReady(String instanceState) {
        return EmulatedCloudStateMapper.map(ResourceType.ATTACHMENT, instanceState).equals(InstanceState.READY);
    }

    @Override
    public boolean hasFailed(String instanceState) {
        return EmulatedCloudStateMapper.map(ResourceType.ATTACHMENT, instanceState).equals(InstanceState.FAILED);
    }

    private AttachmentInstance buildAttachmentInstance(EmulatedAttachment attachment) {
        String instanceId = attachment.getInstanceId();
        String cloudState = attachment.getCloudState();
        String computeId = attachment.getComputeId();
        String volumeId = attachment.getVolumeId();
        String device = attachment.getDevice();

        return new AttachmentInstance(instanceId, cloudState, computeId, volumeId, device);
    }

    private EmulatedAttachment createEmulatedAttachment(AttachmentOrder attachmentOrder){
        String instanceId = EmulatedCloudUtils.getRandomUUID();
        String computeId = attachmentOrder.getComputeId();
        String volumeId = attachmentOrder.getVolumeId();
        String cloudState = EmulatedCloudStateMapper.ACTIVE_STATUS;

        return new EmulatedAttachment.Builder()
                .instanceId(instanceId)
                .computeId(computeId)
                .volumeId(volumeId)
                .cloudState(cloudState)
                .build();
    }
}
