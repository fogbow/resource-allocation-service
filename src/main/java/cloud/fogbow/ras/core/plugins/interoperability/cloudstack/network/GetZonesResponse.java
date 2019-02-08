package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.network;

import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.constants.CloudStackConstants;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/*
 * Documentation: https://cloudstack.apache.org/api/apidocs-4.9/apis/listNetworks.html
 *
 * Example response:
 * {
 *   "listzonesresponse": {
 *     "count": 2,
 *     "zone": [
 *       {
 *         "id": "fake-id-1",
 *         "name": "fake-name-1",
 *         "networktype": "Advanced",
 *         "securitygroupsenabled": false,
 *         "allocationstate": "Enabled",
 *         "zonetoken": "fake-token-1",
 *         "dhcpprovider": "VirtualRouter",
 *         "localstorageenabled": false,
 *         "tags": [],
 *         "resourcedetails": {
 *           "outOfBandManagementEnabled": "false"
 *         }
 *       },
 *       {
 *         "id": "fake-id-2",
 *         "name": "fake-name-2",
 *         "networktype": "Advanced",
 *         "securitygroupsenabled": false,
 *         "allocationstate": "Enabled",
 *         "zonetoken": "fake-token-2",
 *         "dhcpprovider": "VirtualRouter",
 *         "localstorageenabled": false,
 *         "tags": []
 *       }
 *     ]
 *   }
 * }
 */
public class GetZonesResponse {
    @SerializedName(CloudStackConstants.Network.LIST_ZONES_RESPONSE_KEY_JSON)
    private ListZonesResponse listZonesResponse;

    public List<Zone> getZones() {
        return listZonesResponse.zones;
    }

    public static GetZonesResponse fromJson(String jsonResponse) {
        return GsonHolder.getInstance().fromJson(jsonResponse, GetZonesResponse.class);
    }

    public class ListZonesResponse {
        @SerializedName(CloudStackConstants.Network.SECURITY_GROUPS_ENABLED_KEY_JSON)
        private List<Zone> zones;
    }

    public class Zone {
        @SerializedName(CloudStackConstants.Network.ID_KEY_JSON)
        private String id;

        @SerializedName(CloudStackConstants.Network.SECURITY_GROUPS_ENABLED_KEY_JSON)
        private boolean securityGroupsEnabled;

        @SerializedName(CloudStackConstants.Network.NETWORK_TYPE_KEY_JSON)
        private String networkType;

        public String getId() {
            return id;
        }

        public boolean areSecurityGroupsEnabled() {
            return securityGroupsEnabled;
        }

        public String getNetworkType() {
            return networkType;
        }
    }
}
