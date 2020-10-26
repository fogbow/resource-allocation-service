package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.network.models;


import cloud.fogbow.common.constants.GoogleCloudConstants;
import cloud.fogbow.common.util.GsonHolder;
import com.google.gson.annotations.SerializedName;


/**
 * Documentation: https://cloud.google.com/compute/docs/reference/rest/v1/subnetworks/insert
 * <p>
 * Request Example:
 * {
 *
 *  "name":"subnet1",
 *  "network":"https://www.googleapis.com/compute/v1/projects/{project}/global/networks/net1",
 *  "ipCidrRange":"10.158.0.0/20",
 *
 * }
 * <p>
 * We use the @SerializedName annotation to specify that the request parameter is not equal to the class field.
 */
public class InsertSubnetworkRequest {

    @SerializedName(cloud.fogbow.common.constants.GoogleCloudConstants.Network.NAME_KEY_JSON)
    private String name;
    @SerializedName(cloud.fogbow.common.constants.GoogleCloudConstants.Network.NETWORK_KEY_JSON)
    private String network;
    @SerializedName(GoogleCloudConstants.Network.CIDR_KEY_JSON)
    private String ipCidrRange;

    public InsertSubnetworkRequest(Builder builder){
        this.name = builder.name;
        this.network = builder.network;
        this.ipCidrRange = builder.ipCidrRange;
    }
    public String toJson(){
        return GsonHolder.getInstance().toJson(this);
    }

    public static class Builder{
        private String name;
        private String network;
        private String ipCidrRange;

        public Builder name(String name){
            this.name = name;
            return this;
        }
        public Builder network(String network){
            this.network = network;
            return this;
        }
        public Builder ipCidrRange(String cidr){
            this.ipCidrRange = cidr;
            return this;
        }
        public InsertSubnetworkRequest build(){
            return new InsertSubnetworkRequest(this);
        }
    }

}
