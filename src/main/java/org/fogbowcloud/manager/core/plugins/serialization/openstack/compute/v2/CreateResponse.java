package org.fogbowcloud.manager.core.plugins.serialization.openstack.compute.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.manager.core.plugins.serialization.GsonHolder;

import static org.fogbowcloud.manager.core.plugins.serialization.openstack.OpenstackRestApiConstants.Compute.*;

public class CreateResponse {

    @SerializedName(SERVER_KEY_JSON)
    private Server server;

    public String getId() {
        return server.id;
    }

    public static CreateResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, CreateResponse.class);
    }

    public class Server {

        @SerializedName(ID_KEY_JSON)
        private String id;

    }

}
