package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.network;

import cloud.fogbow.common.util.GsonHolder;
import com.google.gson.annotations.SerializedName;
import static cloud.fogbow.common.constants.CloudStackConstants.Network.*;

public class CreateNetworkResponse {
    @SerializedName(CREATE_NETWORK_RESPONSE_KEY_JSON)
    private Response response;

    public static CreateNetworkResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, CreateNetworkResponse.class);
    }

    public String getId() {
        return response.network.id;
    }

    private class Response {
        @SerializedName(NETWORK_KEY_JSON)
        private Network network;
    }

    private class Network {
        @SerializedName(ID_KEY_JSON)
        private String id;
    }
}
