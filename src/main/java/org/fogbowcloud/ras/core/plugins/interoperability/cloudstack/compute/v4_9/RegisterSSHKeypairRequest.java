package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.compute.v4_9;

import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRequest;

public class RegisterSSHKeypairRequest extends CloudStackRequest {
    public static final String REGISTER_KEYPAIR_COMMAND = "registerSSHKeyPair";
    public static final String NAME = "name";
    public static final String PUBLIC_KEY = "publickey";

    private RegisterSSHKeypairRequest(Builder builder) throws InvalidParameterException {
        super();
        addParameter(NAME, builder.name);
        addParameter(PUBLIC_KEY, builder.publicKey);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public String getCommand() {
        return REGISTER_KEYPAIR_COMMAND;
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

        public RegisterSSHKeypairRequest build() throws InvalidParameterException {
            return new RegisterSSHKeypairRequest(this);
        }
    }
}
