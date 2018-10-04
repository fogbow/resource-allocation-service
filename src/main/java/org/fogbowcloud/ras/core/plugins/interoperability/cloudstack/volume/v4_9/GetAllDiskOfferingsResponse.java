package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.volume.v4_9;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;

import java.util.List;

import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.Volume.*;

/**
 * Documentation: https://cloudstack.apache.org/api/apidocs-4.9/apis/listDiskOfferings.html
 * <p>
 * Response example:
 * {
 * "listdiskofferingsresponse": {
 * "diskoffering": [{
 * "id": "e9c2a08d-6ca4-4b81-8e21-5ff2a103b7cb",
 * "disksize": 10,
 * "iscustomized": false,
 * }]
 * }
 * }
 * <p>
 * We use the @SerializedName annotation to specify that the request parameter is not equal to the class field.
 */
public class GetAllDiskOfferingsResponse {
    @SerializedName(DISK_OFFERINGS_KEY_JSON)
    private ListDiskOfferingsResponse response;

    public class ListDiskOfferingsResponse {
        @SerializedName(DISK_OFFERING_KEY_JSON)
        private List<DiskOffering> diskOfferings;
    }

    public List<DiskOffering> getDiskOfferings() {
        return this.response.diskOfferings;
    }

    public static GetAllDiskOfferingsResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, GetAllDiskOfferingsResponse.class);
    }

    public class DiskOffering {
        @SerializedName(ID_KEY_JSON)
        private String id;
        @SerializedName(DISK_KEY_JSON)
        private int diskSize;
        @SerializedName(CUSTOMIZED_KEY_JSON)
        private boolean customized;

        public String getId() {
            return id;
        }

        public int getDiskSize() {
            return diskSize;
        }

        public boolean isCustomized() {
            return customized;
        }
    }
}
