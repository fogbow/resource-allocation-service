package cloud.fogbow.ras.core.plugins.interoperability.openstack.sdk.v2.compute.models;

import cloud.fogbow.common.util.JsonSerializable;
import cloud.fogbow.common.util.SerializeNullsGsonHolder;
import com.google.gson.annotations.SerializedName;

import static cloud.fogbow.common.constants.OpenStackConstants.Compute.*;

/**
 * Documentation: https://developer.openstack.org/api-ref/compute/
 * <p>
 * Pause request body:
 * {
 *      "pause": null
 * }
 * <p>
 * We use the @SerializedName annotation to specify that the request parameter is not equal to the class field.
 */
public class PauseComputeRequest implements JsonSerializable {

    @SerializedName(PAUSE_KEY_JSON)
    private String pause;

    public PauseComputeRequest() {
        this.pause = null;
    }

    @Override
    public String toJson() {
        return SerializeNullsGsonHolder.getInstance().toJson(this);
    }

    public static class Builder {
        public PauseComputeRequest build() {
            return new PauseComputeRequest();
        }
    }
}
