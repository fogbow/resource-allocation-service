package org.fogbowcloud.ras.core.plugins.interoperability.openstack.compute.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;

import static org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants.Compute.ID_KEY_JSON;
import static org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants.Compute.SERVER_KEY_JSON;

/**
 * Documentation: https://developer.openstack.org/api-ref/compute/
 * <p>
 * Response Example:
 * {
 * "server":{
 * "id":"f5dc173b-6804-445a-a6d8-c705dad5b5eb"
 * }
 * }
 * <p>
 * We use the @SerializedName annotation to specify that the request parameter is not equal to the class field.
 */
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
