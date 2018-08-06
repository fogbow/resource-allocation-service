package org.fogbowcloud.manager.core.plugins.serialization.openstack.network.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.manager.core.plugins.serialization.GsonHolder;

import java.util.List;

import static org.fogbowcloud.manager.core.plugins.serialization.openstack.OpenstackRestApiConstants.Network.*;

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
