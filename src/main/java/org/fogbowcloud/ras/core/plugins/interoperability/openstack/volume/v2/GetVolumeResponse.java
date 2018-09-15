package org.fogbowcloud.ras.core.plugins.interoperability.openstack.volume.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;

import static org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants.Volume.*;

/**
 * Documentation: https://developer.openstack.org/api-ref/block-storage/v2/
 * <p>
 * Response example:
 * {
 * "volume": {
 * "status": "creating",
 * "name": "test-volume-attachments",
 * "id": "6edbc2f4-1507-44f8-ac0d-eed1d2608d38",
 * "size": 2
 * }
 * }
 */
public class GetVolumeResponse {
    @SerializedName(VOLUME_KEY_JSON)
    private Volume volume;

    public Volume getVolume() {
        return this.volume;
    }

    public String getId() {
        return volume.id;
    }

    public static GetVolumeResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, GetVolumeResponse.class);
    }

    public String getName() {
        return volume.name;
    }

    public Integer getSize() {
        return volume.size;
    }

    public String getStatus() {
        return volume.status;
    }

    public class Volume {
        @SerializedName(ID_KEY_JSON)
        private String id;
        @SerializedName(NAME_KEY_JSON)
        private String name;
        @SerializedName(SIZE_KEY_JSON)
        private Integer size;
        @SerializedName(STATUS_KEY_JSON)
        private String status;
    }
}
