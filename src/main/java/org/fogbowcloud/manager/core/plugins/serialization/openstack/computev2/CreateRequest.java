package org.fogbowcloud.manager.core.plugins.serialization.openstack.computev2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.manager.core.plugins.serialization.GsonHolder;
import org.fogbowcloud.manager.core.plugins.serialization.JsonSerializable;

import java.util.List;

public class CreateRequest implements JsonSerializable {

    @SerializedName("server")
    private ServerParameters serverParameters;

    private CreateRequest(ServerParameters serverParameters) {
        this.serverParameters = serverParameters;
    }

    public static class ServerParameters {

        @SerializedName("name")
        private final String name;

        @SerializedName("imageRef")
        private final String imageReference;

        @SerializedName("flavorRef")
        private final String flavorReference;

        @SerializedName("user_data")
        private final String userData;

        @SerializedName("key_name")
        private final String keyName;

        @SerializedName("networks")
        private final List<Network> networks;

        @SerializedName("security_groups")
        private final List<SecurityGroup> securityGroups;

        public static class Network {

            private String uuid;

            public Network(String uuid) {
                this.uuid = uuid;
            }

        }

        public static class SecurityGroup {

            private String name;

            public SecurityGroup(String name) {
                this.name = name;
            }

        }

        private ServerParameters(Builder builder) {
            this.name = builder.name;
            this.imageReference = builder.imageReference;
            this.flavorReference = builder.flavorReference;
            this.userData = builder.userData;
            this.keyName = builder.keyName;
            this.networks = builder.networks;
            this.securityGroups = builder.securityGroups;
        }

    }

    public static class Builder {

        private String name;
        private String imageReference;
        private String flavorReference;
        private String userData;
        private String keyName;
        private List<ServerParameters.Network> networks;
        private List<ServerParameters.SecurityGroup> securityGroups;

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

        public Builder networks(List<ServerParameters.Network> networks) {
            this.networks = networks;
            return this;
        }

        public Builder securityGroups(List<ServerParameters.SecurityGroup> securityGroups) {
            this.securityGroups = securityGroups;
            return this;
        }

        public CreateRequest build() {
            ServerParameters serverParameters = new ServerParameters(this);
            return new CreateRequest(serverParameters);
        }

    }

    @Override
    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

}
