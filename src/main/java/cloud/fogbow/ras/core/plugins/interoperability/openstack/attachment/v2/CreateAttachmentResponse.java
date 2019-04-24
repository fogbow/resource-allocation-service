package cloud.fogbow.ras.core.plugins.interoperability.openstack.attachment.v2;

import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.JsonSerializable;
import com.google.gson.annotations.SerializedName;

import static cloud.fogbow.common.constants.OpenStackConstants.Attachment.*;

/**
 * Documentation: https://developer.openstack.org/api-ref/compute/
 * <p>
 * Response example:
 * {
 *   "volumeAttachment": {
 *      "volumeId": "a26887c6-c47b-4654-abb5-dfadf7d3f803",
 *      }
 * }
 */
public class CreateAttachmentResponse {
    @SerializedName(VOLUME_ATTACHMENT_KEY_JSON)
    private Attachment attachment;

    public String getVolumeId() {
        return attachment.volumeId;
    }

    public Attachment getAttachment() {
        return attachment;
    }

    public static CreateAttachmentResponse fromJson(String jsonStr) {
        return GsonHolder.getInstance().fromJson(jsonStr, CreateAttachmentResponse.class);
    }

    public class Attachment {
        @SerializedName(VOLUME_ID_KEY_JSON)
        private String volumeId;
    }
}
