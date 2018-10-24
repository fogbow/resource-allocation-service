package org.fogbowcloud.ras.core.plugins.interoperability.openstack.compute.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;
import org.fogbowcloud.ras.util.JsonSerializable;

import static org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants.Compute.*;

/**
 * Documentation: https://developer.openstack.org/api-ref/compute/
 * <p>
 * Request Example:
 * {
 * "keypair":{
 * "name":"keypair-d20a3d59-9433-4b79-8726-20b431d89c78",
 * "public_key":"ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAAAgQDx8nkQv/zgGgB4rMYmIf+6A4l6Rr+o/6lHBQdW5aYd44bd8JttDCE/F/pNRr0lRE+PiqSPO8nDPHw0010JeMH9gYgnnFlyY3/OcJ02RhIPyyxYpv9FhY+2YiUkpwFOcLImyrxEsYXpD/0d3ac30bNH6Sw9JD9UZHYcpSxsIbECHw== Generated-by-Nova",
 * "user_id":"fake"
 * }
 * }
 * <p>
 * We use the @SerializedName annotation to specify that the request parameter is not equal to the class field.
 */
public class CreateOsKeypairRequest implements JsonSerializable {
    @SerializedName(KEY_PAIR_KEY_JSON)
    private KeyPair keyPair;

    public CreateOsKeypairRequest(KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    public static class KeyPair {
        @SerializedName(NAME_KEY_JSON)
        private String name;
        @SerializedName(PUBLIC_KEY_KEY_JSON)
        private String publicKey;

        public KeyPair(Builder builder) {
            this.name = builder.name;
            this.publicKey = builder.publicKey;
        }

    }

    public static class Builder {
        private String name;
        private String publicKey;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder publicKey(String publicKey) {
            this.publicKey = publicKey;
            return this;
        }

        public CreateOsKeypairRequest build() {
            KeyPair keyPair = new KeyPair(this);
            return new CreateOsKeypairRequest(keyPair);
        }
    }

    @Override
    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }
}
