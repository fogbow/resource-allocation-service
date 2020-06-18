package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.quota.v4_9;

import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackErrorResponse;
import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Objects;

import static cloud.fogbow.common.constants.CloudStackConstants.Quota.*;

/**
 * Documentation: https://cloudstack.apache.org/api/apidocs-4.9/apis/listResourceLimits.html
 * <p>
 * Response example:
 * {
 * "listresourcelimitsresponse": {
 * "count": 1,
 * "resourcelimit": [{
 * "account": "fogbow-rec",
 * "domainid": "0dbcf598-4d56-4b8a-8a6f-343fd0956392",
 * "domain": "FOGBOW",
 * "resourcetype": "0",
 * "max": 100
 * }]
 * }
 * }
 */
public class ListResourceLimitsResponse {

    @SerializedName(LIST_RESOURCE_LIMITS_KEY_JSON)
    private ResourceLimitsResponse response;

    public List<ResourceLimit> getResourceLimits() {
        return response.resourceLimits;
    }

    public static ListResourceLimitsResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, ListResourceLimitsResponse.class);
    }

    public class ResourceLimitsResponse extends CloudStackErrorResponse {

        @SerializedName(RESOURCE_LIMIT_KEY_JSON)
        private List<ResourceLimit> resourceLimits;
    }

    public static class ResourceLimit {

        @SerializedName(RESOURCE_TYPE_KEY_JSON)
        private String resourceType;

        @SerializedName(DOMAIN_ID_KEY_JSON)
        private String domainId;

        @SerializedName(MAX_KEY_JSON)
        private int max;

        public ResourceLimit(String resourceType, String domainId, int max) {
            this.resourceType = resourceType;
            this.domainId = domainId;
            this.max = max;
        }

        public ResourceLimit() {
        }

        public String getResourceType() {
            return resourceType;
        }

        public String getDomainId() {
            return domainId;
        }

        public int getMax() {
            return max;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ResourceLimit that = (ResourceLimit) o;
            return max == that.max &&
                    resourceType.equals(that.resourceType) &&
                    domainId.equals(that.domainId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(resourceType, domainId, max);
        }
    }
}
