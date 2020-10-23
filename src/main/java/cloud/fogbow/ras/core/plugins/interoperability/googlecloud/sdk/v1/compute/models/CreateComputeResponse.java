package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.compute.models;

import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.util.GoogleCloudConstants;
import com.google.gson.annotations.SerializedName;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.util.GoogleCloudConstants.*;

/* Documentation reference: https://cloud.google.com/compute/docs/reference/rest/v1/instances/insert
 * Response example:
 *
 * {
 * "targetId":"687689811763492787",
 * }
 */

public class CreateComputeResponse {

    private Instance instance;

    public String getId () {
        return this.instance.id;
    }

    public static CreateComputeResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, CreateComputeResponse.class);
    }

    public class Instance {
        @SerializedName(GoogleCloudConstants.Compute.TARGET_ID_KEY_JSON)
        private String id;
    }
}
