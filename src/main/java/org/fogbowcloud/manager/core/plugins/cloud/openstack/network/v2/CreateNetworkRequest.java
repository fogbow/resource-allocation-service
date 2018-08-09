package org.fogbowcloud.manager.core.plugins.cloud.openstack.network.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.manager.util.GsonHolder;

import static org.fogbowcloud.manager.core.plugins.cloud.openstack.OpenstackRestApiConstants.Network.*;

/**
 * Documentation: https://developer.openstack.org/api-ref/network/v2/
 *
 * Request Example:
 * {
 *   "network":{
 *     "name":"net1",
 *     "tenant_id":"fake-tenant"
 *   }
 * }
 *
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

        @SerializedName(TENANT_ID_KEY_JSON)
        private String tenantId;

        private Network(Builder builder) {
            this.name = builder.name;
            this.tenantId = builder.tenantId;
        }
    }

    public static class Builder {

        private String name;
        private String tenantId;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public CreateNetworkRequest build() {
            Network network = new Network(this);
            return new CreateNetworkRequest(network);
        }

    }

}
