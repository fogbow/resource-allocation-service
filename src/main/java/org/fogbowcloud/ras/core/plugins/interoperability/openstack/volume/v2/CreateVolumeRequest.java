package org.fogbowcloud.ras.core.plugins.interoperability.openstack.volume.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;
import org.fogbowcloud.ras.util.JsonSerializable;

import static org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants.Volume.*;

/**
 * Documentation: https://developer.openstack.org/api-ref/block-storage/v2/
 * <p>
 * Request example:
 * {
 * "volume": {
 * "size": 10,
 * "name": "volume-name"
 * }
 * }
 */
public class CreateVolumeRequest implements JsonSerializable {
    @SerializedName(VOLUME_KEY_JSON)
    private Volume volume;

    private CreateVolumeRequest(Volume volume) {
        this.volume = volume;
    }

    @Override
    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

    public static class Volume {
        @SerializedName(NAME_KEY_JSON)
        private final String name;
        @SerializedName(SIZE_KEY_JSON)
        private final String size;

        public Volume(Builder builder) {
            this.name = builder.name;
            this.size = builder.size;
        }
    }

    public static class Builder {
        private String name;
        private String size;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder size(String size) {
            this.size = size;
            return this;
        }

        public CreateVolumeRequest build() {
            Volume volume = new Volume(this);
            return new CreateVolumeRequest(volume);
        }
    }
}
