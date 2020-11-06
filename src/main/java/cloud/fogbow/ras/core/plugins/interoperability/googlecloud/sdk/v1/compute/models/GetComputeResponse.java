package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.compute.models;

import cloud.fogbow.common.constants.GoogleCloudConstants;
import cloud.fogbow.common.util.GsonHolder;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/*
 * Documentation reference: https://cloud.google.com/compute/docs/reference/rest/v1/instances/get
 * Successful get example:
 *
 * {
 *   "name":"machine-test-2",
 *   "machineType":"https://www.googleapis.com/compute/v1/projects/ninth-cubist-291121/zones/us-central1-f/machineTypes/custom-2-2048",
 *   "networkInterfaces":[
 *       {
 *           "networkIP":"10.128.0.15"
 *        }
 *   ],
 *   "status": "STAGING"
 * }
 *
 * Failed get exemple:
 * {
 *   "error":{
 *      "message": "The resource 'projects/ninth-cubist-291121/zones/us-central1-f/instances/8480966875822193550' was not found"
 *   }
 * }
 */

public class GetComputeResponse {

    @SerializedName(GoogleCloudConstants.Compute.ID_KEY_JSON)
    private String id;
    @SerializedName(GoogleCloudConstants.Compute.NAME_KEY_JSON)
    private String name;
    @SerializedName(GoogleCloudConstants.Compute.FLAVOR_KEY_JSON)
    private String flavorId;
    @SerializedName(GoogleCloudConstants.Compute.NETWORKS_KEY_JSON)
    private List<Network> addresses;
    @SerializedName(GoogleCloudConstants.Compute.STATUS_KEY_JSON)
    private String status;
    @SerializedName(GoogleCloudConstants.Compute.FAULT_MSG_KEY_JSON)
    private String faultMessage;

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public List<Network> getAddresses() {
        return this.addresses;
    }

    public String getFlavorId() {
        return this.flavorId;
    }

    public String getStatus() {
        return this.status;
    }

    public String getFaultMessage() {
        return this.faultMessage;
    }

    public static GetComputeResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, GetComputeResponse.class);
    }

    public static class Network {
        @SerializedName(GoogleCloudConstants.Compute.ADDRESS_KEY_JSON)
        private String address;

        public String getAddress() {
            return address;
        }
    }
}
