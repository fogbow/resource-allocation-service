package org.fogbowcloud.ras.core.plugins.interoperability.openstack.network.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;

import static org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants.Network.*;

/**
 * Documentation: https://developer.openstack.org/api-ref/network/v2/
 * <p>
 * Request Example:
 * {
 * "network":{
 * "name":"net1",
 * "tenant_id":"fake-tenant"
 * }
 * }
 * <p>
 * We use the @SerializedName annotation to specify that the request parameter is not equal to the class field.
 */
public class CreateNetworkRequest {
    @SerializedName(NETWORK_KEY_JSON)
    private Network network;

    public CreateNetworkRequest(Network network) {
        this.network = network;
    }

    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

    public static class Network {
        @SerializedName(NAME_KEY_JSON)
        private String name;
        @SerializedName(PROJECT_ID_KEY_JSON)
        private String projectId;

        private Network(Builder builder) {
            this.name = builder.name;
            this.projectId = builder.projectId;
        }
    }

    public static class Builder {
        private String name;
        private String projectId;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public CreateNetworkRequest build() {
            Network network = new Network(this);
            return new CreateNetworkRequest(network);
        }
    }
}
