package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.network.models;


import cloud.fogbow.common.constants.GoogleCloudConstants;
import cloud.fogbow.common.util.GsonHolder;
import com.google.gson.annotations.SerializedName;

/**
 * Documentation: https://cloud.google.com/compute/docs/reference/rest/v1/subnetworks/get
 * <p>
 * Response Example:
 * {
 * "ipCidrRange": "10.158.0.0/20",
 * "gatewayAddress": "10.158.0.1"
 * }
 * <p>
 * We use the @SerializedName annotation to specify that the request parameter is not equal to the class field.
 */
public class GetSubnetworkResponse {
    @SerializedName(GoogleCloudConstants.Network.CIDR_KEY_JSON)
    private String ipCidrRange;
    @SerializedName(GoogleCloudConstants.Network.GATEWAY_ADDRESS_KEY_JSON)
    private String gatewayAddress;

    public GetSubnetworkResponse(String ipCidrRange, String gatewayAddress){
        this.ipCidrRange = ipCidrRange;
        this.gatewayAddress = gatewayAddress;
    }

    public String getIpCidrRange() {
        return this.ipCidrRange;
    }

    public String getGatewayAddress() {
        return gatewayAddress;
    }
    public static GetSubnetworkResponse fromJson(String json){
        return GsonHolder.getInstance().fromJson(json, GetSubnetworkResponse.class);
    }
}
