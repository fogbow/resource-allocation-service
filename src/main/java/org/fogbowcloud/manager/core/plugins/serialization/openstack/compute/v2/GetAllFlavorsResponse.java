package org.fogbowcloud.manager.core.plugins.serialization.openstack.compute.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.manager.core.plugins.serialization.GsonHolder;

import java.util.List;

public class GetAllFlavorsResponse {

    @SerializedName("flavors")
    private List<Flavor> flavors;

    public class Flavor {

        @SerializedName("id")
        private String id;

        public String getId() {
            return id;
        }

    }

    public List<Flavor> getFlavors() {
        return flavors;
    }

    public static GetAllFlavorsResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, GetAllFlavorsResponse.class);
    }

}
