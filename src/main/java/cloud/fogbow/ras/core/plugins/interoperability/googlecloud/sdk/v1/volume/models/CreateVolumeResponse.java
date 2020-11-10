package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.volume.models;

import cloud.fogbow.common.util.GsonHolder;
import com.google.gson.annotations.SerializedName;

import static cloud.fogbow.common.constants.GoogleCloudConstants.Volume.*;
import static cloud.fogbow.common.constants.GoogleCloudConstants.GOOGLE_RESPONSE_BASE_URL;
import static cloud.fogbow.common.constants.GoogleCloudConstants.EMPTY_STRING;


/**
 * Documentation: https://cloud.google.com/compute/docs/reference/rest/v1/disks/
 * <p>
 * Response example:
 *  {
 *    "name": "operation=131231231312-d12d21d12d-21d21d21d-ad113g2g234",
 *    "status": "RUNNING",
 *    "targetLink": "https://www.googleapis.com/compute/v1/projects/diesel-talon-291703/zones/southamerica-east1-b/disks/disco1"
 *  }
 */
public class CreateVolumeResponse {

    @SerializedName(TARGET_LINK_KEY_JSON)
    private String targetLink;
    @SerializedName(STATUS_KEY_JSON)
    private String status;
    @SerializedName(NAME_KEY_JSON)
    private String name;

    public CreateVolumeResponse getOperation() {
        return this;
    }

    public static CreateVolumeResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, CreateVolumeResponse.class);
    }

    public String getTargetEndpoint() {
        String targetEndpoint = targetLink.replace(GOOGLE_RESPONSE_BASE_URL, EMPTY_STRING);
        return targetEndpoint;
    }

    public String getTargetLink(){
        return targetLink;
    }

    public String getStatus() {
        return status;
    }

    public String getName() { return name; }

}