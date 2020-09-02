package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.attachment;

import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudConstants;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.ResourceManager;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.attachment.models.EmulatedAttachment;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.compute.EmulatedCloudComputeManager;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.compute.models.EmulatedCompute;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.volume.EmulatedCloudVolumeManager;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.volume.models.EmulatedVolume;

import java.security.InvalidParameterException;
import java.util.*;

public class EmulatedCloudAttachmentManager implements ResourceManager<EmulatedAttachment> {
    private Map<String, EmulatedAttachment> attachments;
    private static EmulatedCloudAttachmentManager instance;

    private EmulatedCloudAttachmentManager() {
        this.attachments = new HashMap<>();
    }

    public static EmulatedCloudAttachmentManager getInstance() {
        if (instance == null) {
            instance = new EmulatedCloudAttachmentManager();
        }
        return instance;
    }

    @Override
    public Optional<EmulatedAttachment> find(String instanceId) {
        return Optional.ofNullable(this.attachments.get(instanceId));
    }

    @Override
    public List<EmulatedAttachment> list() {
        return new ArrayList<>(this.attachments.values());
    }

    @Override
    public String create(EmulatedAttachment attachment) {
        EmulatedCloudUtils.validateEmulatedResource(attachment);
        validateCompute(attachment.getComputeId());
        validateVolume(attachment.getVolumeId());

        this.attachments.put(attachment.getInstanceId(), attachment);
        return attachment.getInstanceId();
    }

    @Override
    public void delete(String instanceId) {
        if (this.attachments.containsKey(instanceId)) {
            this.attachments.remove(instanceId);
        } else {
            throw new InvalidParameterException(EmulatedCloudConstants.Exception.RESOURCE_NOT_FOUND);
        }
    }

    private void validateCompute(String instanceId) {
        Optional<EmulatedCompute> emulatedCompute = EmulatedCloudComputeManager.getInstance().find(instanceId);
        if (!emulatedCompute.isPresent()) {
            throw new InvalidParameterException(EmulatedCloudConstants.Exception.UNABLE_TO_ATTACH_COMPUTE_NOT_FOUND);
        }
    }

    private void validateVolume(String instanceId) {
        Optional<EmulatedVolume> emulatedVolume = EmulatedCloudVolumeManager.getInstance().find(instanceId);
        if (!emulatedVolume.isPresent()) {
            throw new InvalidParameterException(EmulatedCloudConstants.Exception.UNABLE_TO_ATTACH_VOLUME_NOT_FOUND);
        }
    }
}
