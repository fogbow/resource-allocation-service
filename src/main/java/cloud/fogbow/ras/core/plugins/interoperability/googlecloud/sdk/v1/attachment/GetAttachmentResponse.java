package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.attachment;

import cloud.fogbow.common.util.GsonHolder;
import com.google.gson.annotations.SerializedName;

import static cloud.fogbow.common.constants.GoogleCloudConstants.Attachment.*;


/**
 * Documentation:
 * <p>
 * Response example:
*   {
*     "attachDisk": {
*       "deviceName": "disk1",
*       "name": "operation-1603404163017-5b2499f9169f3-be0a4b37-5471f304",
*       "serverId": "instance-1",
*       "volumeSource": "/compute/v1/projects/diesel-talon-291703/zones/southamerica-east1-b/disks/testattach"
*     }
 *  }
 */
public class GetAttachmentResponse {
    @SerializedName(ATTACH_DISK_KEY_JSON)
    private GetAttachmentResponse.Attachment attachment;

    public String getId() {
        return attachment.id;
    }

    public String getVolumeId() {
        return attachment.volumeSource;
    }

    public String getServerId() {
        return attachment.serverId;
    }

    public String getDevice() {
        return attachment.device;
    }

    public Attachment getAttachment() {
        return attachment;
    }

    public static GetAttachmentResponse fromJson(String jsonStr) {
        return GsonHolder.getInstance().fromJson(jsonStr, GetAttachmentResponse.class);
    }

    public class Attachment {
        @SerializedName(ATTACH_ID_KEY_JSON)
        private String id;
        @SerializedName(VOLUME_SOURCE_KEY_JSON)
        private String volumeSource;
        @SerializedName(INSTANCE_NAME_KEY_JSON)
        private String serverId;
        @SerializedName(DEVICE_NAME_KEY_JSON)
        private String device;
    }
}
