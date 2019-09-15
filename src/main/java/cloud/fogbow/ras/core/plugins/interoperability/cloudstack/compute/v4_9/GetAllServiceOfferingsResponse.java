package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9;

import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackErrorResponse;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.annotations.SerializedName;
import org.apache.http.client.HttpResponseException;

import java.util.List;

import static cloud.fogbow.common.constants.CloudStackConstants.Compute.*;

/**
 * Documentation: https://cloudstack.apache.org/api/apidocs-4.9/apis/listServiceOfferings.html
 * <p>
 * Response example:
 * {
 * "listserviceofferingsresponse": {
 * "count": 1,
 * "serviceoffering": [{
 * "id": "97637962-2244-4159-b72c-120834757514",
 * "name": "offering-name",
 * "cpunumber": 4,
 * "memory": 6144,
 * "tags": "tag1:value1,tag2:value2",
 * }]
 * }
 * }
 */
public class GetAllServiceOfferingsResponse {

    @SerializedName(LIST_SERVICE_OFFERINGS_KEY_JSON)
    private ListServiceOfferingsResponse listServiceOfferingsResponse;

    public List<ServiceOffering> getServiceOfferings() {
        return listServiceOfferingsResponse.serviceOfferings;
    }

    public static GetAllServiceOfferingsResponse fromJson(String json) throws HttpResponseException {
        GetAllServiceOfferingsResponse getAllServiceOfferingsResponse =
                GsonHolder.getInstance().fromJson(json, GetAllServiceOfferingsResponse.class);
        getAllServiceOfferingsResponse.listServiceOfferingsResponse.checkErrorExistence();
        return getAllServiceOfferingsResponse;
    }

    public class ListServiceOfferingsResponse extends CloudStackErrorResponse {

        @SerializedName(SERVICE_OFFERING_KEY_JSON)
        private List<ServiceOffering> serviceOfferings;

    }

    public class ServiceOffering {

        @VisibleForTesting
        ServiceOffering(String id, int cpuNumber, int memory, String tags) {
            this.id = id;
            this.cpuNumber = cpuNumber;
            this.memory = memory;
            this.tags = tags;
        }

        @SerializedName(ID_KEY_JSON)
        private String id;
        @SerializedName(NAME_KEY_JSON)
        private String name;
        @SerializedName(CPU_NUMBER_KEY_JSON)
        private int cpuNumber;
        @SerializedName(MEMORY_KEY_JSON)
        private int memory;
        @SerializedName(TAGS_KEY_JSON)
        private String tags;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public int getCpuNumber() {
            return cpuNumber;
        }

        /**
         * @return the memory of this service offering in MB
         */
        public int getMemory() {
            return memory;
        }

        public String getTags() {
            return tags;
        }
    }
}
