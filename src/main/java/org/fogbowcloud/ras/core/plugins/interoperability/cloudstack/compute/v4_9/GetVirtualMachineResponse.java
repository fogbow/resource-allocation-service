package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.compute.v4_9;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;

import java.util.List;

import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.Compute.*;

/**
 * Documentation: https://cloudstack.apache.org/api/apidocs-4.9/apis/listVirtualMachines.html
 * Response example:
 * {
 * "listvirtualmachinesresponse": {
 * "count": 1,
 * "virtualmachine": [{
 * "id": "97637962-2244-4159-b72c-120834757514",
 * "name": "PreprocessingProducao",
 * "state": "Running",
 * "cpunumber": 4,
 * "memory": 6144,
 * "nic": [{
 * "ipaddress": "10.1.1.146",
 * }]
 * }]
 * }
 * }
 */
public class GetVirtualMachineResponse {
    @SerializedName(VIRTUAL_MACHINES_KEY_JSON)
    private ListVirtualMachinesResponse virtualMachinesResponse;

    public class ListVirtualMachinesResponse {
        @SerializedName(VIRTUAL_MACHINE_KEY_JSON)
        private List<VirtualMachine> virtualMachines;
    }

    public List<VirtualMachine> getVirtualMachines() {
        return virtualMachinesResponse.virtualMachines;
    }

    public class VirtualMachine {
        @SerializedName(ID_KEY_JSON)
        private String id;
        @SerializedName(NAME_KEY_JSON)
        private String name;
        @SerializedName(STATE_KEY_JSON)
        private String state;
        @SerializedName(CPU_NUMBER_KEY_JSON)
        private int cpuNumber;
        @SerializedName(MEMORY_KEY_JSON)
        private int memory;
        @SerializedName(NIC_KEY_JSON)
        private Nic[] nic;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getState() {
            return state;
        }

        public int getCpuNumber() {
            return cpuNumber;
        }

        public int getMemory() {
            return memory;
        }

        public Nic[] getNic() {
            return nic;
        }
    }

    public class Nic {
        @SerializedName(IP_ADDRESS_KEY_JSON)
        private String ipAddress;

        public String getIpAddress() {
            return ipAddress;
        }
    }

    public static GetVirtualMachineResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, GetVirtualMachineResponse.class);
    }
}
