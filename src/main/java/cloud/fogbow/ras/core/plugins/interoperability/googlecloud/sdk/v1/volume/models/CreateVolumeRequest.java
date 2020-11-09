package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.volume.models;

import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.JsonSerializable;
import com.google.gson.annotations.SerializedName;

import static cloud.fogbow.common.constants.GoogleCloudConstants.Volume.*;
/**
 * Documentation: https://cloud.google.com/compute/docs/reference/rest/v1/disks/
 * <p>
 * Request example:
 * "volume": {
 *   "name": "volume-name"
 *   "sizeGb": 10,
 *   "type": "https://cloud.google.com/compute/v1/projects/diesel-talon-291703/zones/southamerica-east1-b/diskTypes/pd-balanced"
 * }
 * <p>
 */
public class CreateVolumeRequest implements JsonSerializable {
    @SerializedName(NAME_KEY_JSON)
    private String name;
    @SerializedName(SIZE_KEY_JSON)
    private String size;
    @SerializedName(TYPE_KEY_JSON)
    private String volumeType;

    private CreateVolumeRequest(String name, String size, String volumeType) {
        this.name = name;
        this.size = size;
        this.volumeType = volumeType;
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
        @SerializedName(TYPE_KEY_JSON)
        private final String volumeType;

        public Volume(Builder builder) {
            this.name = builder.name;
            this.size = builder.size;
            this.volumeType = builder.volumeType;
        }
    }

    public static class Builder {
        private String name;
        private String size;
        private String volumeType;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder size(String size) {
            this.size = size;
            return this;
        }

        public Builder volumeType(String volumeType){
            this.volumeType = volumeType;
            return this;
        }

        public CreateVolumeRequest build() {
            Volume volume = new CreateVolumeRequest.Volume(this);
            return new CreateVolumeRequest(volume.name, volume.size, volume.volumeType);
        }
    }
}