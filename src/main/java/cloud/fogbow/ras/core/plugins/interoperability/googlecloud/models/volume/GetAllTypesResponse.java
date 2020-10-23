package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.models.volume;

import cloud.fogbow.common.util.GsonHolder;
import com.google.gson.annotations.SerializedName;

import static cloud.fogbow.common.constants.GoogleCloudConstants.Volume.*;

import java.util.List;

/**
 * Documentation: https://cloud.google.com/compute/docs/reference/rest/v1/diskTypes
 * <p>
 * Response example:
 *   {
 *   "items": [
 *         {
 *             "name": "pd-standard",
 *             "description": "Standard Persistent Disk"
 *             validDiskSize: "10GB-65536GB"
 *         },
 *         {
 *             "name": "pd-ssd",
 *             "description": "SSD Persistent Disk"
 *             validDiskSize: "10GB-65536GB"
 *         }
 *     ]
 * }
 */

//TODO
public class GetAllTypesResponse {
    @SerializedName(VOLUME_KEY_JSON)
    private List<Type> types;

    public static GetAllTypesResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, GetAllTypesResponse.class);
    }

    public class Type {

        @SerializedName(NAME_KEY_JSON)
        private String name;

        @SerializedName(DESCRIPTION_KEY_JSON)
        private String description;

        @SerializedName(VALID_DISK_SIZE_KEY_JSON)
        private String validDiskSize;

        public String getValidDiskSize() {
            return validDiskSize;
        }

        public String getName() {
            return name;
        }
    }

    public List<Type> getTypes() {
        return types;
    }
}