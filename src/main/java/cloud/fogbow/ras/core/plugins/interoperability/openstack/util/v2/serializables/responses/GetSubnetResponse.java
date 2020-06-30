package cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.responses;

import cloud.fogbow.common.util.GsonHolder;
import com.google.gson.annotations.SerializedName;

import static cloud.fogbow.common.constants.OpenStackConstants.Network.*;

/**
 * Documentation: https://developer.openstack.org/api-ref/network/v2/
 * <p>
 * Response Example:
 * {
 * "subnet": {
 * "gateway_ip": "192.0.0.1",
 * "enable_dhcp": true,
 * "cidr": "192.0.0.0/8"
 * }
 * }
 * <p>
 * We use the @SerializedName annotation to specify that the request parameter is not equal to the class field.
 */
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
        private String subnetCidr;
    }

    public String getGatewayIp() {
        return subnet.gatewayIp;
    }

    public boolean isDhcpEnabled() {
        return subnet.dhcpEnabled;
    }

    public String getSubnetCidr() {
        return subnet.subnetCidr;
    }
}
