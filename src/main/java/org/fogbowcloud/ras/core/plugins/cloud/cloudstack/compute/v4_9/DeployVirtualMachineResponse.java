package org.fogbowcloud.ras.core.plugins.cloud.cloudstack.compute.v4_9;

import org.fogbowcloud.ras.util.GsonHolder;

public class DeployVirtualMachineResponse {
    // FIXME add @SerializedName tag
    private String id;

    public static DeployVirtualMachineResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, DeployVirtualMachineResponse.class);
    }

    public String getId() {
        return id;
    }
}
