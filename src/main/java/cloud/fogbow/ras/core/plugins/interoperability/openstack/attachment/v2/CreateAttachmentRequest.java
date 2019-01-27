package cloud.fogbow.ras.core.plugins.interoperability.openstack.attachment.v2;

import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.JsonSerializable;
import com.google.gson.annotations.SerializedName;

import static cloud.fogbow.common.constants.OpenStackConstants.Attachment.DEVICE_KEY_JSON;
import static cloud.fogbow.common.constants.OpenStackConstants.Attachment.VOLUME_ATTACHMENT_KEY_JSON;
import static cloud.fogbow.common.constants.OpenStackConstants.Attachment.VOLUME_ID_KEY_JSON;

/**
 * Documentation: https://developer.openstack.org/api-ref/compute/
 * <p>
 * Request example:
 * {
 *   "volumeAttachment": {
 *      "volumeId": "a26887c6-c47b-4654-abb5-dfadf7d3f803",
 *      "device": "/dev/vdd"
 *      }
 * }
 */
public class CreateAttachmentRequest implements JsonSerializable {
    @SerializedName(VOLUME_ATTACHMENT_KEY_JSON)
    private Attachment attachment;

    private CreateAttachmentRequest(Attachment attachment) {
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

        public Attachment(Builder builder) {
            this.volumeId = builder.volumeId;
            this.device = builder.device;
        }
    }

    public static class Builder {
        private String volumeId;
        private String device;

        public Builder volumeId(String volumeId) {
            this.volumeId = volumeId;
            return this;
        }

        public Builder device(String device) {
            this.device = device;
            return this;
        }

        public CreateAttachmentRequest build() {
            Attachment attachment = new Attachment(this);
            return new CreateAttachmentRequest(attachment);
        }
    }
}
