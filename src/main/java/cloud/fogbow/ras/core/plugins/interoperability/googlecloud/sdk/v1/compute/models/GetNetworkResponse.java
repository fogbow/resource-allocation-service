package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.compute.models;

import cloud.fogbow.common.constants.GoogleCloudConstants;
import cloud.fogbow.common.util.GsonHolder;
import com.google.gson.annotations.SerializedName;

public class GetNetworkResponse {
    @SerializedName(GoogleCloudConstants.Network.NAME_KEY_JSON)
    private String name;

    public String getName() {
        return this.name;
    }

    public static GetNetworkResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, GetNetworkResponse.class);
    }
}
