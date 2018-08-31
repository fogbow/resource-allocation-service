package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.quota;

import java.util.List;

/**
 * Documentation: https://cloudstack.apache.org/api/apidocs-4.9/apis/listResourceLimits.html
 *
 * Response example:
 * {
 *     "listresourcelimitsresponse": {
 *         "count": 1,
 *         "resourcelimit": [{
 *             "account": "fogbow-rec",
 *             "domainid": "0dbcf598-4d56-4b8a-8a6f-343fd0956392",
 *             "domain": "FOGBOW",
 *             "resourcetype": "0",
 *             "max": 100
 *         }]
 *     }
 * }
 */
public class ListResourceLimitsResponse {

    public static ListResourceLimitsResponse fromJson(String jsonResponse) {
        return null;
    }

    public List<ResourceLimit> getResourceLimits() {
        return null;
    }

    public static class ResourceLimit {

        private String max;
        private String resourceType;

        public String getResourceType() {
            return resourceType;
        }

        public String getMax() {
            return max;
        }
    }

}
