package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.models.volume;

import cloud.fogbow.common.util.GsonHolder;
import com.google.gson.annotations.SerializedName;

import static cloud.fogbow.ras.core.plugins.interoperability.googlecloud.util.GoogleCloudConstants.Volume.*;

/**
 * Documentation: https://cloud.google.com/compute/docs/reference/rest/v1/disks/
 * <p>
 * Response example:
 * {
 *  "volume": {
 *      "status": "READY",
 *      "name": "test-volume-attachments",
 *      "id": "2131393199439060075",
 *      "sizeGb": 20,
 *      "type": "https://www.googleapis.com/compute/v1/projects/diesel-talon-291703/zones/southamerica-east1-b/diskTypes/pd-standard"
 *  }
 * }
 */
public class GetVolumeResponse {
    @SerializedName(VOLUME_KEY_JSON)
    private Volume volume;

    public Volume getVolume() {
        return this.volume;
    }

    public static GetVolumeResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, GetVolumeResponse.class);
    }

    public String getId() {
        return volume.id;
    }

    public String getName() {
        return volume.name;
    }

    public Integer getSize() {
        return volume.size;
    }

    public String getType() {
        return volume.type;
    }

    public String getStatus() {
        return volume.status;
    }

    public class Volume {
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

    }
}