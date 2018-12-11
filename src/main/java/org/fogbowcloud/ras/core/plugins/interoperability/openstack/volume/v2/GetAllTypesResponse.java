package org.fogbowcloud.ras.core.plugins.interoperability.openstack.volume.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;

import java.util.List;
import java.util.Map;

import static org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants.Volume.*;

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
    @SerializedName(VOLUME_TYPES_KEY_JSON)
    private List<Type> types;

    public static GetAllTypesResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, GetAllTypesResponse.class);
    }

    public class Type {
        @SerializedName(EXTRA_SPECS_KEY_JSON)
        private Map<String, String> extraSpecs;

        @SerializedName(ID_KEY_JSON)
        private String id;

        @SerializedName(NAME_KEY_JSON)
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
