package cloud.fogbow.ras.core.plugins.interoperability.openstack.sdk.v2.compute.models;

import static cloud.fogbow.common.constants.OpenStackConstants.Compute.UNSHELVE_KEY_JSON;

import com.google.gson.annotations.SerializedName;

import cloud.fogbow.common.util.JsonSerializable;
import cloud.fogbow.common.util.SerializeNullsGsonHolder;

/**
 * Documentation: https://developer.openstack.org/api-ref/compute/
 * <p>
 * Unshelve request body:
 * {
 *      "unshelve": null
 * }
 * <p>
 * We use the @SerializedName annotation to specify that the request parameter is not equal to the class field.
 */
public class UnshelveComputeRequest implements JsonSerializable {
    @SerializedName(UNSHELVE_KEY_JSON)
    private String unshelve;

    public UnshelveComputeRequest() {
        this.unshelve = null;
    }
    
    @Override
    public String toJson() {
        return SerializeNullsGsonHolder.getInstance().toJson(this);
    }
    
    public static class Builder {

        public UnshelveComputeRequest build() {
            return new UnshelveComputeRequest();
        }

    }
}
