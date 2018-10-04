package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.network;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants;
import org.fogbowcloud.ras.util.GsonHolder;

public class CreateNetworkResponse {
    @SerializedName(CloudStackRestApiConstants.Network.CREATE_NETWORK_RESPONSE_KEY_JSON)
    private Response response;

    public static CreateNetworkResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, CreateNetworkResponse.class);
    }

    public String getId() {
        return response.network.id;
    }

    private class Response {
        @SerializedName(CloudStackRestApiConstants.Network.NETWORK_KEY_JSON)
        private Network network;
    }

    private class Network {
        @SerializedName(CloudStackRestApiConstants.Network.ID_KEY)
        private String id;
    }
}
