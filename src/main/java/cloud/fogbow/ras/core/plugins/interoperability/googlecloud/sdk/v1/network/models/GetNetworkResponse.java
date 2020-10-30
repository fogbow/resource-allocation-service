package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.network.models;


import cloud.fogbow.common.constants.GoogleCloudConstants;
import cloud.fogbow.common.util.GsonHolder;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Documentation: https://cloud.google.com/compute/docs/reference/rest/v1/networks/get
 * <p>
 * Response Example:
 * {
 *
 * "id":"922330021232618281",
 * "name":"private-network",
 * "selfLink": "https://www.googleapis.com/compute/v1/projects/my-project5c6ad/global/networks/net1",
 * "subnetworks":[
 *  "https://www.googleapis.com/compute/v1/projects/my-project5c6ad/regions/southamerica-east1/subnetworks/sub1"
 * ]
 * }
 * <p>
 * We use the @SerializedName annotation to specify that the request parameter is not equal to the class field.
 */
public class GetNetworkResponse {
    @SerializedName(GoogleCloudConstants.Network.ID_KEY_JSON)
    private String id;
    @SerializedName(GoogleCloudConstants.Network.NAME_KEY_JSON)
    private String name;
    @SerializedName(GoogleCloudConstants.Network.SELF_LINK_KEY_JSON)
    private String selfLink;
    @SerializedName(GoogleCloudConstants.Network.SUBNETWORKS_KEY_JSON)
    private List<String> subnetworks;

    public GetNetworkResponse(String id, String name, String selfLink, List<String> subnetworks){
        this.id = id;
        this.name = name;
        this.selfLink = selfLink;
        this.subnetworks = subnetworks;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getSelfLink() {
        return this.selfLink;
    }

    public List<String> getSubnetworks() {
        return this.subnetworks;
    }

    public static GetNetworkResponse fromJson(String json){
        return GsonHolder.getInstance().fromJson(json, GetNetworkResponse.class);
    }



}
