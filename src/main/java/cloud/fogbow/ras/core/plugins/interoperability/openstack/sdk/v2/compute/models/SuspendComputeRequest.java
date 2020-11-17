package cloud.fogbow.ras.core.plugins.interoperability.openstack.sdk.v2.compute.models;

import cloud.fogbow.common.util.JsonSerializable;
import cloud.fogbow.common.util.SerializeNullsGsonHolder;
import com.google.gson.annotations.SerializedName;

import static cloud.fogbow.common.constants.OpenStackConstants.Compute.*;

/**
 * Documentation: https://developer.openstack.org/api-ref/compute/
 * <p>
 * Suspend request body:
 * {
 *      "suspend": null
 * }
 * <p>
 * We use the @SerializedName annotation to specify that the request parameter is not equal to the class field.
 */
public class SuspendComputeRequest implements JsonSerializable {

    @SerializedName(SUSPEND_KEY_JSON)
    private String suspend;

    public SuspendComputeRequest() {
        this.suspend = null;
    }

    @Override
    public String toJson() {
        return SerializeNullsGsonHolder.getInstance().toJson(this);
    }

    public static class Builder {
        public SuspendComputeRequest build() {
            return new SuspendComputeRequest();
        }
    }
}
