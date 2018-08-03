package org.fogbowcloud.manager.core.plugins.serialization.openstack.network.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.manager.core.plugins.serialization.GsonHolder;
import org.fogbowcloud.manager.core.plugins.serialization.openstack.OpenstackRestApiConstants;

public class CreateResponse {

    public Network network;

    public CreateResponse(Network network) {
        this.network = network;
    }

    public String getId() {
        return network.id;
    }

    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

    public static CreateResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, CreateResponse.class);
    }

    public static class Network {

        @SerializedName(OpenstackRestApiConstants.Network.ID_KEY_JSON)
        private String id;

        public Network(String id) {
            this.id = id;
        }

    }

}
