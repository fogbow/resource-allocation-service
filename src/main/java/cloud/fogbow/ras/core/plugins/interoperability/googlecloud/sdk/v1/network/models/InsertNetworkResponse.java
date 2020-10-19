package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.network.models;

import cloud.fogbow.common.util.GsonHolder;
import com.google.gson.annotations.SerializedName;
import static cloud.fogbow.ras.core.plugins.interoperability.googlecloud.util.GoogleCloudConstants.Network.NETWORK_KEY_JSON;

/**
 * Documentation: https://cloud.google.com/compute/docs/reference/rest/v1/networks
 * <p>
 * Response Example:
 * {
 *  "network": https://www.googleapis.com/compute/v1/projects/chatflutter-5c6ad/global/networks/net1
 * }
 * <p>
 * We use the @SerializedName annotation to specify that the request parameter is not equal to the class field.
 */
public class InsertNetworkResponse {
    @SerializedName(NETWORK_KEY_JSON)
    private String network;

    public InsertNetworkResponse(String network){
        this.network = network;
    }

    public String getNetwork(){
        return this.network;
    }

    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

    public static InsertNetworkResponse fromJson(String json){
        return GsonHolder.getInstance().fromJson(json, InsertNetworkResponse.class);
    }

}
