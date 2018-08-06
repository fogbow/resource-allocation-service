package org.fogbowcloud.manager.core.plugins.serialization.openstack.compute.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.manager.core.plugins.serialization.GsonHolder;

import static org.fogbowcloud.manager.core.plugins.serialization.openstack.OpenstackRestApiConstants.Compute.*;

public class CreateComputeResponse {

    @SerializedName(SERVER_KEY_JSON)
    private Server server;

    public String getId() {
        return server.id;
    }

    public static CreateComputeResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, CreateComputeResponse.class);
    }

    public class Server {

        @SerializedName(ID_KEY_JSON)
        private String id;

    }

}
