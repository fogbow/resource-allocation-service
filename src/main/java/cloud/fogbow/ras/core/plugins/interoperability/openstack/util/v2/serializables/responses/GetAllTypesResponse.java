package cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.responses;

import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.util.GsonHolder;
import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/**
 * Documentation: https://developer.openstack.org/api-ref/block-storage/v2/?expanded=list-all-volume-types-for-v2-detail
 * <p>
 * Response example:
 *   {
 *   "volume_types": [
 *         {
 *             "extra_specs": {
 *                 "capabilities": "gpu"
 *             },
 *             "id": "6685584b-1eac-4da6-b5c3-555430cf68ff",
 *             "name": "SSD"
 *         },
 *         {
 *             "extra_specs": {},
 *             "id": "8eb69a46-df97-4e41-9586-9a40a7533803",
 *             "name": "SATA"
 *         }
 *     ]
 * }
 */

public class GetAllTypesResponse {
    @SerializedName(OpenStackConstants.Volume.VOLUME_TYPES_KEY_JSON)
    private List<Type> types;

    public static GetAllTypesResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, GetAllTypesResponse.class);
    }

    public class Type {
        @SerializedName(OpenStackConstants.Volume.EXTRA_SPECS_KEY_JSON)
        private Map<String, String> extraSpecs;

        @SerializedName(OpenStackConstants.Volume.ID_KEY_JSON)
        private String id;

        @SerializedName(OpenStackConstants.Volume.NAME_KEY_JSON)
        private String name;

        public Map<String, String> getExtraSpecs() {
            return extraSpecs;
        }

        public String getId() {
            return id;
        }
    }

    public List<Type> getTypes() {
        return types;
    }
}
