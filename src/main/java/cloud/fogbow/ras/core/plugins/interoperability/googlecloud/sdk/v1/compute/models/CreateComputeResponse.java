package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.compute.models;

import cloud.fogbow.common.util.GsonHolder;
import com.google.gson.annotations.SerializedName;

/* Documentation reference: https://cloud.google.com/compute/docs/reference/rest/v1/instances/insert
 * Response example:
 *
 * {
 * "targetId":"687689811763492787",
 * }
 */

public class CreateComputeResponse {

    @SerializedName(cloud.fogbow.common.constants.GoogleCloudConstants.Compute.TARGET_ID_KEY_JSON)
    private String id;

    public String getId () {
        return this.id;
    }

    public static CreateComputeResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, CreateComputeResponse.class);
    }

}
