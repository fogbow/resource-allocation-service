package org.fogbowcloud.manager.core.plugins.serialization.openstack.computev2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.manager.core.plugins.serialization.GsonHolder;

public class GetFlavorResponse {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("disk")
    private int disk;

    @SerializedName("ram")
    private int memory;

    @SerializedName("vcpus")
    private int vcpusCount;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getDisk() {
        return disk;
    }

    public int getMemory() {
        return memory;
    }

    public int getVcpusCount() {
        return vcpusCount;
    }

    public static GetFlavorResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, GetFlavorResponse.class);
    }

}
