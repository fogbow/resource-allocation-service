package cloud.fogbow.ras.core.plugins.interoperability.openstack.sdk.v2.compute.models;

import static cloud.fogbow.common.constants.OpenStackConstants.Compute.SHELVE_KEY_JSON;

import com.google.gson.annotations.SerializedName;

import cloud.fogbow.common.util.JsonSerializable;
import cloud.fogbow.common.util.SerializeNullsGsonHolder;

/**
 * Documentation: https://developer.openstack.org/api-ref/compute/
 * <p>
 * Shelve request body:
 * {
 *      "shelve": null
 * }
 * <p>
 * We use the @SerializedName annotation to specify that the request parameter is not equal to the class field.
 */
public class ShelveComputeRequest implements JsonSerializable {
    
    @SerializedName(SHELVE_KEY_JSON)
    private String shelve;

    public ShelveComputeRequest() {
        this.shelve = null;
    }
    
    @Override
    public String toJson() {
        return SerializeNullsGsonHolder.getInstance().toJson(this);
    }
    
    public static class Builder {

        public ShelveComputeRequest build() {
            return new ShelveComputeRequest();
        }

    }
}
