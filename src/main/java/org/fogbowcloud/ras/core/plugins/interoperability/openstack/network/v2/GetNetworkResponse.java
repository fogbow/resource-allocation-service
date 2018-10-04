package org.fogbowcloud.ras.core.plugins.interoperability.openstack.network.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;

import java.util.List;

import static org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants.Network.*;

/**
 * Documentation: https://developer.openstack.org/api-ref/network/v2/
 * <p>
 * Response Example:
 * {
 * "network":{
 * "id":"d32019d3-bc6e-4319-9c1d-6722fc136a22",
 * "name":"private-network",
 * "provider:segmentation_id":95612,
 * "subnets":[
 * "54d6f61d-db07-451c-9ab3-b9609b6b6f0b"
 * ],
 * "status":"ACTIVE"
 * }
 * }
 * <p>
 * We use the @SerializedName annotation to specify that the request parameter is not equal to the class field.
 */
public class GetNetworkResponse {
    @SerializedName(NETWORK_KEY_JSON)
    private Network network;

    public static GetNetworkResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, GetNetworkResponse.class);
    }

    public class Network {
        @SerializedName(ID_KEY_JSON)
        private String id;
        @SerializedName(NAME_KEY_JSON)
        private String name;
        @SerializedName(PROVIDER_SEGMENTATION_ID_KEY_JSON)
        private String segmentationId;
        @SerializedName(SUBNETS_KEY_JSON)
        private List<String> subnets;
        @SerializedName(STATUS_KEY_JSON)
        private String status;
    }

    public String getId() {
        return network.id;
    }

    public String getName() {
        return network.name;
    }

    public String getSegmentationId() {
        return network.segmentationId;
    }

    public List<String> getSubnets() {
        return network.subnets;
    }

    public String getStatus() {
        return network.status;
    }
}
