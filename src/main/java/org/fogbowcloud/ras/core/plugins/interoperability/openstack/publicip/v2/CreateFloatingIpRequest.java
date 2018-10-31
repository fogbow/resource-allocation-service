package org.fogbowcloud.ras.core.plugins.interoperability.openstack.publicip.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;
import org.fogbowcloud.ras.util.JsonSerializable;

import static org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants.PublicIp.*;

/**
 * Documentation : https://developer.openstack.org/api-ref/network/v2/#create-floating-ip
 * <p>
 * Request Example:
 * {
 * "floatingip": {
 * "floating_network_id": "376da547-b977-4cfe-9cba-275c80debf57",
 * "port_id": "ce705c24-c1ef-408a-bda3-7bbd946164ab",
 * "project_id": "10705c24-c1ef-408a-bda3-7bbd946164xx"
 * }
 * }
 */
public class CreateFloatingIpRequest implements JsonSerializable {

    @SerializedName(FLOATING_IP_KEY_JSON)
    private FloatingIp floatingIp;

    public CreateFloatingIpRequest(FloatingIp floatingIp) {
        this.floatingIp = floatingIp;
    }

    @Override
    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

    private static class FloatingIp {

        @SerializedName(FLOATING_NETWORK_ID_KEY_JSON)
        private String floatingNetworkId;
        @SerializedName(PORT_ID_KEY_JSON)
        private String portId;
        @SerializedName(PROJECT_ID_KEY_JSON)
        private String projectId;

        public FloatingIp(Builder builder) {
            this.floatingNetworkId = builder.floatingNetworkId;
            this.portId = builder.portId;
            this.projectId = builder.projectId;
        }

    }

    public static class Builder {
        private String floatingNetworkId;
        private String portId;
        private String projectId;

        public Builder floatingNetworkId(String floatingNetworkId) {
            this.floatingNetworkId = floatingNetworkId;
            return this;
        }

        public Builder portId(String portId) {
            this.portId = portId;
            return this;
        }

        public Builder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public CreateFloatingIpRequest build() {
            FloatingIp server = new FloatingIp(this);
            return new CreateFloatingIpRequest(server);
        }
    }

}
