package org.fogbowcloud.manager.core.plugins.serialization.openstack.compute;

import org.fogbowcloud.manager.core.plugins.serialization.GsonHolder;
import org.fogbowcloud.manager.core.plugins.serialization.JsonSerializable;

import java.util.List;

public class CreateRequest implements JsonSerializable {

    private String name;
    private String imageReference;
    private String flavorReference;
    private String userData;
    private String keyName;
    private List<String> networksIds;

    public static class Builder {

        private String name;
        private String imageReference;
        private String flavorReference;
        private String userData;
        private String keyName;
        private List<String> networksIds;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder imageReference(String imageReference) {
            this.imageReference = imageReference;
            return this;
        }

        public Builder flavorReference(String flavorReference) {
            this.flavorReference = flavorReference;
            return this;
        }

        public Builder userData(String userData) {
            this.userData = userData;
            return this;
        }

        public Builder keyName(String keyName) {
            this.keyName = keyName;
            return this;
        }

        public Builder networksIds(List<String> networksIds) {
            this.networksIds = networksIds;
            return this;
        }

        public CreateRequest build() {
            return new CreateRequest(this);
        }

    }

    private CreateRequest(Builder builder) {
        this.imageReference = builder.imageReference;
        this.flavorReference = builder.flavorReference;
        this.userData = builder.userData;
        this.keyName = builder.keyName;
        this.networksIds = builder.networksIds;
    }

    @Override
    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

}
