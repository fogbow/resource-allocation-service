package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.compute.v4_9;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;

import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.Compute.JOB_RESULT_KEY_JSON;
import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.Compute.VIRTUAL_MACHINE_KEY_JSON;
import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.Compute.ID_KEY_JSON;

public class DeployVirtualMachineResponse {

    @SerializedName(JOB_RESULT_KEY_JSON)
    private JobResult jobResult;

    public String getId() {
        return jobResult.virtualMachine.id;
    }

    public class JobResult {
        @SerializedName(VIRTUAL_MACHINE_KEY_JSON)
        private VirtualMachine virtualMachine;

    }

    public class VirtualMachine {
        @SerializedName(ID_KEY_JSON)
        private String id;

    }

    public static DeployVirtualMachineResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, DeployVirtualMachineResponse.class);
    }
}
