package cloud.fogbow.ras.core.plugins.interoperability.openstack.sdk.v2.compute.models;

import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.JsonSerializable;
import com.google.gson.annotations.SerializedName;

import static cloud.fogbow.common.constants.CloudStackConstants.Compute.NAME_KEY_JSON;
import static cloud.fogbow.common.constants.OpenStackConstants.Compute.CREATE_IMAGE_KEY_JSON;


public class CreateImageRequest implements JsonSerializable{
    @SerializedName(CREATE_IMAGE_KEY_JSON)
    private CreateImage snapshot;

    public CreateImageRequest(CreateImage snapshot) {
        this.snapshot = snapshot;
    }

    @Override
    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

    public static class CreateImage {
        @SerializedName(NAME_KEY_JSON)
        private String name;

        private CreateImage(Builder build) {
            this.name = build.name;
        }
    }

    public static class Builder {
        private String name;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public CreateImageRequest build() {
            CreateImage snapshot = new CreateImage(this);
            return new CreateImageRequest(snapshot);
        }
    }
}
