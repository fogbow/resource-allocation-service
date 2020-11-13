package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.attachment;

import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.JsonSerializable;
import com.google.gson.annotations.SerializedName;

import static cloud.fogbow.common.constants.GoogleCloudConstants.Attachment.*;

/**
 * Documentation: https://cloud.google.com/compute/docs/reference/rest/v1/instances/attachDisk/
 * <p>
 * Request example:
 * {
 *   "attachDisk": {
 *      "source": "/compute/v1/projects/talon-291703/zones/southamerica-east1-b/disks/testdisk",
 *      "deviceName": "testAttach"
 *      }
 * }
 */
public class CreateAttachmentRequest implements JsonSerializable {
    @SerializedName(ATTACH_DISK_KEY_JSON)
    private Attachment attachment;

    private CreateAttachmentRequest(Attachment attachment) {
        this.attachment = attachment;
    }

    @Override
    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

    public static class Attachment {
        @SerializedName(VOLUME_SOURCE_KEY_JSON)
        private final String volumeSource;
        @SerializedName(DEVICE_NAME_KEY_JSON)
        private final String device;

        public Attachment(Builder builder) {
            this.volumeSource = builder.volumeSource;
            this.device = builder.device;
        }
    }

    public static class Builder {
        private String volumeSource;
        private String device;

        public Builder volumeSource(String volumeSource) {
            this.volumeSource = volumeSource;
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

