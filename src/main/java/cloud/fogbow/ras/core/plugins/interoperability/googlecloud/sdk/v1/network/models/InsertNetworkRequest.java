package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.network.models;


import cloud.fogbow.common.constants.GoogleCloudConstants;
import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.network.models.enums.RoutingMode;
import com.google.gson.annotations.SerializedName;



/**
 * Documentation: https://cloud.google.com/compute/docs/reference/rest/v1/networks
 * <p>
 * Request Example:
 * {
 *  "name": "net1",
 *  "autoCreateSubnetworks": false,
 *  "routingConfig": {
 *     "routingMode": "GLOBAL" (or REGIONAL)
 *   }
 * }
 * </p>
 * We use the @SerializedName annotation to specify that the request parameter is not equal to the class field.
 */
public class InsertNetworkRequest {
    @SerializedName(cloud.fogbow.common.constants.GoogleCloudConstants.Network.NAME_KEY_JSON)
    private final String name;
    @SerializedName(cloud.fogbow.common.constants.GoogleCloudConstants.Network.AUTO_CREATE_SUBNETS_KEY_JSON)
    private final boolean autoCreateSubnetworks;
    @SerializedName(cloud.fogbow.common.constants.GoogleCloudConstants.Network.ROUTING_CONFIG_KEY_JSON)
    private final RoutingConfig routingConfig;

    public InsertNetworkRequest(RoutingConfig routingConfig, Builder builder){
        this.routingConfig = routingConfig;
        this.name = builder.name;
        this.autoCreateSubnetworks = builder.autoCreateSubnetworks;
    }

    public String toJson(){
        return GsonHolder.getInstance().toJson(this);
    }

    public static class RoutingConfig {

        @SerializedName(GoogleCloudConstants.Network.ROUTING_MODE_KEY_JSON)
        private final String routingMode;
        private RoutingConfig(Builder builder) {
            this.routingMode = builder.routingMode.getDescription();
        }
    }

    public static class Builder{
        private String name;
        private boolean autoCreateSubnetworks;
        private RoutingMode routingMode;

        public Builder name(String name){
            this.name = name;
            return this;
        }
        public Builder autoCreateSubnetworks(boolean option){
            this.autoCreateSubnetworks = option;
            return this;
        }
        public Builder routingMode(RoutingMode routingMode){
            this.routingMode = routingMode;
            return this;
        }
        public InsertNetworkRequest build(){
            RoutingConfig routingConfig = new RoutingConfig(this);
            return new InsertNetworkRequest(routingConfig, this);
        }
    }

}
