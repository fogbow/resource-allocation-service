package org.fogbowcloud.manager.core.plugins.serialization.openstack.computev2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.manager.core.plugins.serialization.GsonHolder;
import org.fogbowcloud.manager.core.plugins.serialization.JsonSerializable;

public class CreateOsKeypairRequest implements JsonSerializable {

    @SerializedName("keypair")
    private KeyPair keyPair;

    public CreateOsKeypairRequest(KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    public static class KeyPair {

        @SerializedName("name")
        private String name;

        @SerializedName("public_key")
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
