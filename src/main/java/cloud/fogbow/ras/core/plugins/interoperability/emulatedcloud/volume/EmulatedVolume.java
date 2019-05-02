package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.volume;


import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.JsonSerializable;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.volume.v2.GetVolumeResponse;
import com.google.gson.annotations.SerializedName;

import static cloud.fogbow.common.constants.OpenStackConstants.Volume.*;

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
public class EmulatedVolume implements JsonSerializable {

    @SerializedName(VOLUME_KEY_JSON)
    private Volume volume;


    private EmulatedVolume(Volume volume) {
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
        @SerializedName(ID_KEY_JSON)
        private final String id;
        @SerializedName(STATUS_KEY_JSON)
        private final String status;

        public Volume(Builder builder) {
            this.name = builder.name;
            this.size = builder.size;
            this.id = builder.id;
            this.status = builder.status;
        }
    }

    public static class Builder {
        private String name;
        private String size;
        private String id;
        private String status;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder size(String size) {
            this.size = size;
            return this;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public EmulatedVolume build() {
            Volume volume = new Volume(this);
            return new EmulatedVolume(volume);
        }
    }

    public Volume getVolume() {
        return this.volume;
    }

    public String getId() {
        return volume.id;
    }

    public static EmulatedVolume fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, EmulatedVolume.class);
    }

    public String getName() {
        return volume.name;
    }

    public String getSize() {
        return volume.size;
    }

    public String getStatus() {
        return volume.status;
    }
}

