package org.fogbowcloud.manager.core.plugins.serialization.openstack.network.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.manager.core.plugins.serialization.GsonHolder;

import static org.fogbowcloud.manager.core.plugins.serialization.openstack.OpenstackRestApiConstants.Network.*;

public class CreateRequest {

    @SerializedName(NETWORK_KEY_JSON)
    private Network network;

    public CreateRequest(Network network) {
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

        public CreateRequest build() {
            Network network = new Network(this);
            return new CreateRequest(network);
        }

    }

}
