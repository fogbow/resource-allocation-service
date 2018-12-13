package org.fogbowcloud.ras.core.plugins.interoperability.openstack.compute.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;

import java.util.List;
import java.util.Map;

import static org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants.Compute.FLAVOR_EXTRA_SPECS_KEY_JSON;

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
    @SerializedName(FLAVOR_EXTRA_SPECS_KEY_JSON)
    private Map<String, String> flavorExtraSpecs;

    public Map<String, String> getFlavorExtraSpecs() {
        return flavorExtraSpecs;
    }

    public static GetFlavorExtraSpecsResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, GetFlavorExtraSpecsResponse.class);
    }
}
