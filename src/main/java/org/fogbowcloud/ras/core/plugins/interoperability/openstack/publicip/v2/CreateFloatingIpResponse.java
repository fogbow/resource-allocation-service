package org.fogbowcloud.ras.core.plugins.interoperability.openstack.publicip.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;

import static org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants.PublicIp.*;

/**
 * Documentation : https://developer.openstack.org/api-ref/network/v2/#create-floating-ip
 * <p>
 * Response Example:
 * {
 * "floatingip": {
 * "id": "2f245a7b-796b-4f26-9cf9-9e82d248fda7",
 * }
 * }
 */
public class CreateFloatingIpResponse {

    @SerializedName(FLOATING_IP_KEY_JSON)
    private FloatingIp floatingIp;

    public static class FloatingIp {
        @SerializedName(ID_KEY_JSON)
        private String id;
        @SerializedName(FLOATING_IP_ADDRESS_KEY_JSON)
        private String floatingIpAddress;

        public String getId() {
            return id;
        }

        public String getFloatingIpAddress() {
            return floatingIpAddress;
        }
    }

    public FloatingIp getFloatingIp() {
        return floatingIp;
    }

    public static CreateFloatingIpResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, CreateFloatingIpResponse.class);
    }

}
