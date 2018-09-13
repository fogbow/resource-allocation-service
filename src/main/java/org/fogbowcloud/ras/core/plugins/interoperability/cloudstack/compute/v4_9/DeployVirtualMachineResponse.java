package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.compute.v4_9;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;

import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.Compute.*;

public class DeployVirtualMachineResponse {

    @SerializedName(DEPLOY_VIRTUAL_MACHINE)
    private DeployVirtualMachineResponseInner response;

    public String getId() {
        return response.id;
    }

    public static DeployVirtualMachineResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, DeployVirtualMachineResponse.class);
    }

    public class DeployVirtualMachineResponseInner {

        @SerializedName(ID_KEY_JSON)
        private String id;

    }

}
