package cloud.fogbow.ras.core.plugins.interoperability.openstack.volume.v2;

import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.JsonSerializable;
import com.google.gson.annotations.SerializedName;

import static cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants.Volume.*;

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
        @SerializedName(VOLUME_TYPES_KEY_JSON)
        private final String volume_type;

        public Volume(Builder builder) {
            this.name = builder.name;
            this.size = builder.size;
            this.volume_type = builder.volume_type;
        }
    }

    public static class Builder {
        private String name;
        private String size;
        private String volume_type;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder size(String size) {
            this.size = size;
            return this;
        }

        public Builder volume_type(String volume_type){
            this.volume_type = volume_type;
            return this;
        }

        public CreateVolumeRequest build() {
            Volume volume = new Volume(this);
            return new CreateVolumeRequest(volume);
        }
    }
}
