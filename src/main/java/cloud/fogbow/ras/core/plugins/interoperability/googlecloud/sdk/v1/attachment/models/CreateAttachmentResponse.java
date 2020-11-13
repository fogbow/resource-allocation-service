package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.attachment.models;

import cloud.fogbow.common.util.GsonHolder;
import com.google.gson.annotations.SerializedName;

import static cloud.fogbow.common.constants.GoogleCloudConstants.Attachment.*;
/**
 * Documentation: https://cloud.google.com/compute/docs/reference/rest/v1/instances/attachDisk/
 * <p>
 * Response example:
 *  {
 *    "name": "operation-1603392146426-5b246d352c8b2-74578d56-bb18116d"
 *    "status": "RUNNING",
 *    "targetLink": "https://www.googleapis.com/compute/v1/projects/diesel-talon-291703/zones/southamerica-east1-b/instances/instance-1",
 *  }
 * </p>
 */
public class CreateAttachmentResponse {

    @SerializedName(ATTACH_ID_KEY_JSON)
    private String id;
    @SerializedName(TARGET_LINK_KEY_JSON)
    private String targetLink;
    @SerializedName(STATUS_KEY_JSON)
    private String status;
    @SerializedName(NAME_KEY_JSON)
    private String name;

    public CreateAttachmentResponse getOperation() { return this; }

    public static CreateAttachmentResponse fromJson(String jsonStr) {
        return GsonHolder.getInstance().fromJson(jsonStr, CreateAttachmentResponse.class);
    }

    public String getTargetLink() { return targetLink; }

    public String getStatus() { return status; }

    public String getName() { return name; }

    public String getId() { return id; }

}