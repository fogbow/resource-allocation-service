package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.attachment;

import cloud.fogbow.common.util.GsonHolder;
import com.google.gson.annotations.SerializedName;

import static cloud.fogbow.common.constants.GoogleCloudConstants.Attachment.*;

/**
 * Documentation: https://cloud.google.com/compute/docs/reference/rest/v1/instances/attachDisk/
 * <p>
 * Response example:
 * {
 *   "attachDisk": {
 *      "id": "627807028365999980"
 *      "name": "operation-1603392146426-5b246d352c8b2-74578d56-bb18116d"
 *   }
 * }
 */
public class CreateAttachmentResponse {
    @SerializedName(ATTACH_DISK_KEY_JSON)
    private Attachment attachment;

    public String getAttachmentId() {
        return attachment.id;
    }
    public String getAttachmentName() { return attachment.name; }

    public Attachment getAttachment() {
        return attachment;
    }

    public static CreateAttachmentResponse fromJson(String jsonStr) {
        return GsonHolder.getInstance().fromJson(jsonStr, CreateAttachmentResponse.class);
    }

    public class Attachment {
        @SerializedName(ATTACH_ID_KEY_JSON)
        private String id;
        @SerializedName(ATTACH_NAME_KEY_JSON)
        private String name;
    }


}
