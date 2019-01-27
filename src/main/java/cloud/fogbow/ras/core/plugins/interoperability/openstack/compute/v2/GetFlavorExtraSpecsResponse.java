package cloud.fogbow.ras.core.plugins.interoperability.openstack.compute.v2;

import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.util.GsonHolder;
import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * Documentation: https://developer.openstack.org/api-ref/compute/?expanded=list-extra-specs-for-a-flavor-detail#list-extra-specs-for-a-flavor
 * <p>
 * Response Example:
 * {
 *     "extra_specs": {
 *         "key1": "value1",
 *         "key2": "value2"
 *     }
 * }
 * <p>
 * We use the @SerializedName annotation to specify that the request parameter is not equal to the class field.
 */
public class GetFlavorExtraSpecsResponse {
    @SerializedName(OpenStackConstants.Compute.FLAVOR_EXTRA_SPECS_KEY_JSON)
    private Map<String, String> flavorExtraSpecs;

    public Map<String, String> getFlavorExtraSpecs() {
        return flavorExtraSpecs;
    }

    public static GetFlavorExtraSpecsResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, GetFlavorExtraSpecsResponse.class);
    }
}
