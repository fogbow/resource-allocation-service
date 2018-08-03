package org.fogbowcloud.manager.core.plugins.serialization.openstack.network.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.manager.core.plugins.serialization.GsonHolder;

import static org.fogbowcloud.manager.core.plugins.serialization.openstack.OpenstackRestApiConstants.Network.*;

public class GetSubnetResponse {

    @SerializedName(SUBNET_KEY_JSON)
    private Subnet subnet;

    public static GetSubnetResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, GetSubnetResponse.class);
    }

    public static class Subnet {

        @SerializedName(GATEWAY_IP_KEY_JSON)
        private String gatewayIp;

        @SerializedName(ENABLE_DHCP_KEY_JSON)
        private boolean dhcpEnabled;

        @SerializedName(CIDR_KEY_JSON)
        private String subnetAddress;

    }

    public String getGatewayIp() {
        return subnet.gatewayIp;
    }

    public boolean isDhcpEnabled() {
        return subnet.dhcpEnabled;
    }

    public String getSubnetAddress() {
        return subnet.subnetAddress;
    }

}
