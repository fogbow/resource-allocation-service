package org.fogbowcloud.ras.core.plugins.interoperability.openstack.compute.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;
import org.fogbowcloud.ras.util.JsonSerializable;

import java.util.List;

import static org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants.Compute.*;

/**
 * Documentation: https://developer.openstack.org/api-ref/compute/
 * <p>
 * Request Example:
 * {
 * "server":{
 * "name":"new-server-test",
 * "imageRef":"70a599e0-31e7-49b7-b260-868f441e862b",
 * "flavorRef":"1",
 * "user_data":"IyEvYmluL2Jhc2gKL2Jpbi9zdQplY2hvICJJIGFtIGluIHlvdSEiCg==",
 * "key_name":"keypair-d20a3d59-9433-4b79-8726-20b431d89c78",
 * "networks":[
 * {
 * "uuid":"ff608d40-75e9-48cb-b745-77bb55b5eaf2",
 * "tag":"nic1"
 * }
 * ],
 * "security_groups":[
 * {
 * "name":"default"
 * }
 * ]
 * }
 * }
 * <p>
 * We use the @SerializedName annotation to specify that the request parameter is not equal to the class field.
 */
public class CreateComputeRequest implements JsonSerializable {
    @SerializedName(SERVER_KEY_JSON)
    private Server server;

    private CreateComputeRequest(Server server) {
        this.server = server;
    }

    @Override
    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

    public static class Server {
        @SerializedName(NAME_KEY_JSON)
        private final String name;
        @SerializedName(IMAGE_REFERENCE_KEY_JSON)
        private final String imageReference;
        @SerializedName(FLAVOR_REFERENCE_KEY_JSON)
        private final String flavorReference;
        @SerializedName(USER_DATA_KEY_JSON)
        private final String userData;
        @SerializedName(KEY_NAME_KEY_JSON)
        private final String keyName;
        @SerializedName(NETWORKS_KEY_JSON)
        private final List<Network> networks;
        @SerializedName(SECURITY_GROUPS_KEY_JSON)
        private final List<SecurityGroup> securityGroups;

        private Server(Builder builder) {
            this.name = builder.name;
            this.imageReference = builder.imageReference;
            this.flavorReference = builder.flavorReference;
            this.userData = builder.userData;
            this.keyName = builder.keyName;
            this.networks = builder.networks;
            this.securityGroups = builder.securityGroups;
        }
    }

    public static class Network {
        @SerializedName(UUID_KEY_JSON)
        private String uuid;

        public Network(String uuid) {
            this.uuid = uuid;
        }
    }

    public static class SecurityGroup {
        @SerializedName(NAME_KEY_JSON)
        private String name;

        public SecurityGroup(String name) {
            this.name = name;
        }
    }

    public static class Builder {
        private String name;
        private String imageReference;
        private String flavorReference;
        private String userData;
        private String keyName;
        private List<Network> networks;
        private List<SecurityGroup> securityGroups;

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

        public Builder networks(List<Network> networks) {
            this.networks = networks;
            return this;
        }

        public Builder securityGroups(List<SecurityGroup> securityGroups) {
            this.securityGroups = securityGroups;
            return this;
        }

        public CreateComputeRequest build() {
            Server server = new Server(this);
            return new CreateComputeRequest(server);
        }
    }
}
