package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.network;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;

import java.util.List;

import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.Network.*;

/*
 * Documentation: https://cloudstack.apache.org/api/apidocs-4.9/apis/listNetworks.html
 *
 * Example response:
 * {
 *   "listzonesresponse": {
 *     "count": 2,
 *     "zone": [
 *       {
 *         "id": "df33046e-ab2a-429c-8653-09d869a9d7aa",
 *         "name": "CDC-MAO01",
 *         "networktype": "Advanced",
 *         "securitygroupsenabled": false,
 *         "allocationstate": "Enabled",
 *         "zonetoken": "c1b1f76f-355f-36e7-9657-b160209b4fd1",
 *         "dhcpprovider": "VirtualRouter",
 *         "localstorageenabled": false,
 *         "tags": [],
 *         "resourcedetails": {
 *           "outOfBandManagementEnabled": "false"
 *         }
 *       },
 *       {
 *         "id": "0d89768b-bdf5-455e-b4fd-91881fa07375",
 *         "name": "CDC-REC01",
 *         "networktype": "Advanced",
 *         "securitygroupsenabled": false,
 *         "allocationstate": "Enabled",
 *         "zonetoken": "8bf4bcf8-217c-3243-ac36-fd570896ddfa",
 *         "dhcpprovider": "VirtualRouter",
 *         "localstorageenabled": false,
 *         "tags": []
 *       }
 *     ]
 *   }
 * }
 */
public class GetZonesResponse {
    @SerializedName(LIST_ZONES_RESPONSE_KEY_JSON)
    private ListZonesResponse listZonesResponse;

    public List<Zone> getZones() {
        return listZonesResponse.zones;
    }

    public static GetZonesResponse fromJson(String jsonResponse) {
        return GsonHolder.getInstance().fromJson(jsonResponse, GetZonesResponse.class);
    }

    public class ListZonesResponse {
        @SerializedName(SECURITY_GROUPS_ENABLED_KEY_JSON)
        private List<Zone> zones;
    }

    public class Zone {
        @SerializedName(ID_KEY_JSON)
        private String id;

        @SerializedName(SECURITY_GROUPS_ENABLED_KEY_JSON)
        private boolean securityGroupsEnabled;

        @SerializedName(NETWORK_TYPE_KEY_JSON)
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
