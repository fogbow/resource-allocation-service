package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackErrorResponse;
import com.google.gson.annotations.SerializedName;

import javax.validation.constraints.NotNull;
import java.util.List;

import static cloud.fogbow.common.constants.CloudStackConstants.Volume.*;

/**
 * Documentation: https://cloudstack.apache.org/api/apidocs-4.9/apis/listDiskOfferings.html
 * <p>
 * Response example:
 * {
 *   "listdiskofferingsresponse": {
 *     "diskoffering": [{
 *       "id": "e9c2a08d-6ca4-4b81-8e21-5ff2a103b7cb",
 *       "disksize": 10,
 *       "iscustomized": false,
 *       "tags": "tag1:value1,tag2:value2"
 *     }]
 *   }
 * }
 * <p>
 * We use the @SerializedName annotation to specify that the request parameter is not equal to the class field.
 */
public class GetAllDiskOfferingsResponse {

    @SerializedName(DISK_OFFERINGS_KEY_JSON)
    private ListDiskOfferingsResponse listDiskOfferingsResponse;

    @NotNull
    public List<DiskOffering> getDiskOfferings() {
        return this.listDiskOfferingsResponse.diskOfferings;
    }

    public static GetAllDiskOfferingsResponse fromJson(String json) throws FogbowException {
        GetAllDiskOfferingsResponse getAllDiskOfferingsResponse = GsonHolder.getInstance().
                fromJson(json, GetAllDiskOfferingsResponse.class);
        getAllDiskOfferingsResponse.listDiskOfferingsResponse.checkErrorExistence();
        return getAllDiskOfferingsResponse;
    }

    public class ListDiskOfferingsResponse extends CloudStackErrorResponse {
        @SerializedName(DISK_OFFERING_KEY_JSON)
        private List<DiskOffering> diskOfferings;
    }

    public class DiskOffering {
        @SerializedName(ID_KEY_JSON)
        private String id;
        @SerializedName(DISK_KEY_JSON)
        private int diskSize;
        @SerializedName(CUSTOMIZED_KEY_JSON)
        private boolean customized;
        @SerializedName(TAGS_KEY_JSON)
        private String tags;

        public String getId() {
            return id;
        }

        public int getDiskSize() {
            return diskSize;
        }

        public boolean isCustomized() {
            return customized;
        }

        public String getTags() {
            return tags;
        }
    }
}
