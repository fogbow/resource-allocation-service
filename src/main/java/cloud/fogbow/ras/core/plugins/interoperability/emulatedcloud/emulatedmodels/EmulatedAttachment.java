package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.emulatedmodels;

import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.JsonSerializable;
import com.google.gson.annotations.SerializedName;

import static cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudConstants.Json.*;

/**
 * Documentation: https://developer.openstack.org/api-ref/device/
 * <p>
 * Request example:
 * {
 *   "volumeAttachment": {
 *      "volumeId": "a26887c6-c47b-4654-abb5-dfadf7d3f803",
 *      "device": "/dev/vdd"
 *      }
 * }
 */
public class EmulatedAttachment implements JsonSerializable {
    @SerializedName(VOLUME_ATTACHMENT_KEY_JSON)
    private Attachment attachment;

    private EmulatedAttachment(Attachment attachment) {
        this.attachment = attachment;
    }

    @Override
    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

    public static class Attachment {
        @SerializedName(VOLUME_ID_KEY_JSON)
        private final String volumeId;
        @SerializedName(DEVICE_KEY_JSON)
        private final String device;
        @SerializedName(INSTANCE_ID_KEY_JSON)
        private final String instanceId;
        @SerializedName(CLOUD_STATE_KEY_JSON)
        private final String cloudState;
        @SerializedName(COMPUTE_ID_KEY_JSON)
        private final String computeId;

        public Attachment(Builder builder) {
            this.volumeId = builder.volumeId;
            this.device = builder.device;
            this.instanceId = builder.instanceId;
            this.cloudState = builder.cloudState;
            this.computeId = builder.computeId;
        }

    }

    public static EmulatedAttachment fromJson(String jsonStr) {
        return GsonHolder.getInstance().fromJson(jsonStr, EmulatedAttachment.class);
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
            Attachment attachment = new Attachment(this);
            return new EmulatedAttachment(attachment);
        }
    }

    public String getVolumeId(){
        return attachment.volumeId;
    }

    public String getDevice(){
        return attachment.device;
    }

    public String getInstanceId(){
        return attachment.instanceId;
    }

    public String getCloudState(){
        return attachment.cloudState;
    }

    public String getComputeId(){
        return attachment.computeId;
    }
}
