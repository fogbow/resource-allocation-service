package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.network;

import cloud.fogbow.common.util.GsonHolder;
import com.google.gson.annotations.SerializedName;

import java.util.List;

import static cloud.fogbow.common.constants.CloudStackConstants.Network.NETWORKS_KEY_JSON;
import static cloud.fogbow.common.constants.CloudStackConstants.Network.NETWORK_KEY_JSON;

/*
 * Documentation: https://cloudstack.apache.org/api/apidocs-4.9/apis/listNetworks.html
 *
 * Example response:
 * {
 *     "listnetworksresponse": {
 *         "count": 8,
 *         "network": [{
 *             "id": "dcd137e5-1ed8-4917-b3ef-e56e0e5f2290",
 *             "name": "F",
 *             "gateway": "10.1.1.1",
 *             "cidr": "10.1.1.0/24",
 *             "state": "Allocated"
 *         ]
 *     }
 * }
 */
public class GetNetworkResponse {
    @SerializedName(NETWORKS_KEY_JSON)
    private ListNetworksResponse listNetworksResponse;

    public List<Network> getNetworks() {
        return listNetworksResponse.networks;
    }

    public static GetNetworkResponse fromJson(String jsonResponse) {
        return GsonHolder.getInstance().fromJson(jsonResponse, GetNetworkResponse.class);
    }

    public class ListNetworksResponse {
        @SerializedName(NETWORK_KEY_JSON)
        private List<Network> networks;
    }

    public class Network {
        private String id;
        private String name;
        private String gateway;
        private String cidr;
        private String state;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getGateway() {
            return gateway;
        }

        public String getCidr() {
            return cidr;
        }

        public String getState() {
            return state;
        }
    }
}
