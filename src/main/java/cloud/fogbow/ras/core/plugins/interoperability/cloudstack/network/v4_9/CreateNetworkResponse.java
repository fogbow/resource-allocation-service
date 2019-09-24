package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.network.v4_9;

import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackErrorResponse;
import com.google.gson.annotations.SerializedName;
import org.apache.http.client.HttpResponseException;

import static cloud.fogbow.common.constants.CloudStackConstants.Network.*;

/**
 * Documentation: https://cloudstack.apache.org/api/apidocs-4.9/apis/createnetwork.html
 * <p>
 * Response example:
 * {
 * "createnetworkresponse": {
 * "network": {
 * "id": "dad76621-edcd-4968-a152-74d877d1961b",
 * }
 * }
 * }
 * <p>
 * We use the @SerializedName annotation to specify that the request parameter is not equal to the class field.
 */
public class CreateNetworkResponse {
    @SerializedName(CREATE_NETWORK_RESPONSE_KEY_JSON)
    private Response response;

    public static CreateNetworkResponse fromJson(String json) throws HttpResponseException {
        CreateNetworkResponse createNetworkResponse =
                GsonHolder.getInstance().fromJson(json, CreateNetworkResponse.class);
        createNetworkResponse.response.checkErrorExistence();
        return createNetworkResponse;
    }

    public String getId() {
        return response != null && response.network != null ?
                response.network.id :
                null;
    }

    private class Response extends CloudStackErrorResponse {
        @SerializedName(NETWORK_KEY_JSON)
        private Network network;
    }

    private class Network {
        @SerializedName(ID_KEY_JSON)
        private String id;
    }
}
