package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9;

import cloud.fogbow.common.util.GsonHolder;
import com.google.gson.annotations.SerializedName;

import static cloud.fogbow.common.constants.CloudStackConstants.Compute.DEPLOY_VIRTUAL_MACHINE;
import static cloud.fogbow.common.constants.CloudStackConstants.Compute.ID_KEY_JSON;

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
