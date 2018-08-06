package org.fogbowcloud.manager.core.plugins.serialization.openstack.network.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.manager.core.plugins.serialization.GsonHolder;
import org.fogbowcloud.manager.core.plugins.serialization.openstack.OpenstackRestApiConstants;

public class CreateNetworkResponse {

    public Network network;

    public CreateNetworkResponse(Network network) {
        this.network = network;
    }

    public String getId() {
        return network.id;
    }

    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

    public static CreateNetworkResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, CreateNetworkResponse.class);
    }

    public static class Network {

        @SerializedName(OpenstackRestApiConstants.Network.ID_KEY_JSON)
        private String id;

        public Network(String id) {
            this.id = id;
        }

    }

}
