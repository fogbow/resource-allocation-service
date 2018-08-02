package org.fogbowcloud.manager.core.plugins.serialization.openstack.compute.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.manager.core.plugins.serialization.GsonHolder;

import static org.fogbowcloud.manager.core.plugins.serialization.openstack.OpenstackRestApiConstants.Compute.*;

public class GetFlavorResponse {

    @SerializedName(FLAVOR_KEY_JSON)
    private Flavor flavor;

    public String getId() {
        return flavor.id;
    }

    public String getName() {
        return flavor.name;
    }

    public int getDisk() {
        return flavor.disk;
    }

    public int getMemory() {
        return flavor.memory;
    }

    public int getVcpusCount() {
        return flavor.vcpusCount;
    }

    public static GetFlavorResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, GetFlavorResponse.class);
    }

    public class Flavor {

        @SerializedName(ID_KEY_JSON)
        private String id;

        @SerializedName(NAME_KEY_JSON)
        private String name;

        @SerializedName(DISK_KEY_JSON)
        private int disk;

        @SerializedName(MEMORY_KEY_JSON)
        private int memory;

        @SerializedName(VCPUS_KEY_JSON)
        private int vcpusCount;

    }

}
