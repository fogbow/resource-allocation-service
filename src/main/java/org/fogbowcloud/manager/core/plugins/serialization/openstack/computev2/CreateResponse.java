package org.fogbowcloud.manager.core.plugins.serialization.openstack.computev2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.manager.core.plugins.serialization.GsonHolder;

public class CreateResponse {

    @SerializedName("server")
    private ServerParameters serverParameters;

    public class ServerParameters {

        @SerializedName("id")
        private String id;

    }

    public String getId() {
        return serverParameters.id;
    }

    public static CreateResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, CreateResponse.class);
    }
}
