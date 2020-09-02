package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.attachment.models;

import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.EmulatedResource;

public class EmulatedAttachment extends EmulatedResource {
    private final String volumeId;
    private final String device;
    private final String cloudState;
    private final String computeId;

    public EmulatedAttachment(String instanceId, String volumeId, String computeId, String device, String cloudState) {
        super(instanceId);
        this.volumeId = volumeId;
        this.device = device;
        this.cloudState = cloudState;
        this.computeId = computeId;
    }

    public String getVolumeId() {
        return volumeId;
    }

    public String getDevice() {
        return device;
    }

    public String getCloudState() {
        return cloudState;
    }

    public String getComputeId() {
        return computeId;
    }

    public static class Builder {
        private String volumeId;
        private String device;
        private String instanceId;
        private String cloudState;
        private String computeId;

        public Builder volumeId(String volumeId) {
            this.volumeId = volumeId;
            return this;
        }

        public Builder device(String device) {
            this.device = device;
            return this;
        }

        public Builder instanceId(String instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        public Builder cloudState(String cloudState) {
            this.cloudState = cloudState;
            return this;
        }

        public Builder computeId(String computeId) {
            this.computeId = computeId;
            return this;
        }

        public EmulatedAttachment build() {
            return new EmulatedAttachment(instanceId, volumeId, computeId, device, cloudState);
        }
    }
}
