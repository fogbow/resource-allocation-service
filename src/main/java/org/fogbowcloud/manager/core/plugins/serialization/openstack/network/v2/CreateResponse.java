package org.fogbowcloud.manager.core.plugins.serialization.openstack.network.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.manager.core.plugins.serialization.GsonHolder;
import org.fogbowcloud.manager.core.plugins.serialization.openstack.OpenstackRestApiConstants;

public class CreateResponse {

    public Network network;

    public static CreateResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, CreateResponse.class);
    }

    public class Network {

        @SerializedName(OpenstackRestApiConstants.Network.ID_KEY_JSON)
        private String id;

    }

    public String getId() {
        return network.id;
    }

}
