package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.volume.models;

import cloud.fogbow.common.util.GsonHolder;
import com.google.gson.annotations.SerializedName;

import static cloud.fogbow.common.constants.GoogleCloudConstants.Volume.*;

/**
 * Documentation: https://cloud.google.com/compute/docs/reference/rest/v1/disks/
 * <p>
 * Response example:
 *  {
 *    "status": "READY",
 *    "name": "test-volume-attachments",
 *    "id": "2131393199439060075",
 *    "sizeGb": 20,
 *    "type": "https://www.googleapis.com/compute/v1/projects/diesel-talon-291703/zones/southamerica-east1-b/diskTypes/pd-standard"
 *  }
 */
public class GetVolumeResponse {
    @SerializedName(ID_KEY_JSON)
    private String id;
    @SerializedName(NAME_KEY_JSON)
    private String name;
    @SerializedName(SIZE_KEY_JSON)
    private Integer size;
    @SerializedName(STATUS_KEY_JSON)
    private String status;
    @SerializedName(TYPE_KEY_JSON)
    private String type;

    public GetVolumeResponse getVolume() {
        return this;
    }

    public static GetVolumeResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, GetVolumeResponse.class);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getSize() {
        return size;
    }

    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }

}