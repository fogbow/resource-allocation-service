package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.compute.model;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackErrorResponse;
import com.google.gson.annotations.SerializedName;

import static cloud.fogbow.common.constants.CloudStackConstants.Compute.DEPLOY_VIRTUAL_MACHINE;
import static cloud.fogbow.common.constants.CloudStackConstants.Compute.ID_KEY_JSON;

public class DeployVirtualMachineResponse {

    @SerializedName(DEPLOY_VIRTUAL_MACHINE)
    private DeployVirtualMachineResponseInner deployVirtualMachineResponseInner;

    public String getId() {
        return deployVirtualMachineResponseInner.id;
    }

    public static DeployVirtualMachineResponse fromJson(String json) throws FogbowException {
        DeployVirtualMachineResponse deployVirtualMachineResponse =
                GsonHolder.getInstance().fromJson(json, DeployVirtualMachineResponse.class);
        deployVirtualMachineResponse.deployVirtualMachineResponseInner.checkErrorExistence();
        return deployVirtualMachineResponse;
    }

    public class DeployVirtualMachineResponseInner extends CloudStackErrorResponse {

        @SerializedName(ID_KEY_JSON)
        private String id;
    }

}
