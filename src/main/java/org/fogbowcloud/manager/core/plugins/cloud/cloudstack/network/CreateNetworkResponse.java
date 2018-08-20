package org.fogbowcloud.manager.core.plugins.cloud.cloudstack.network;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.manager.util.GsonHolder;

public class CreateNetworkResponse {

    // FIXME add @SerializedName tag
    private String id;

    public static CreateNetworkResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, CreateNetworkResponse.class);
    }

    public String getId() {
        return id;
    }
}
