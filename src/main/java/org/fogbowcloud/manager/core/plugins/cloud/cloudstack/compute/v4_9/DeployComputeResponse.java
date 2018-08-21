package org.fogbowcloud.manager.core.plugins.cloud.cloudstack.compute.v4_9;

import org.fogbowcloud.manager.util.GsonHolder;

public class DeployComputeResponse {

    // FIXME add @SerializedName tag
    private String id;

    public static DeployComputeResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, DeployComputeResponse.class);
    }

    public String getId() {
        return id;
    }
}
