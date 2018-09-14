package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.compute.v4_9;

import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRequest;

public class DeleteSSHKeypairRequest extends CloudStackRequest {
    public static final String DELETE_KEYPAIR_COMMAND = "deleteSSHKeyPair";
    public static final String NAME = "name";

    private DeleteSSHKeypairRequest(Builder builder) throws InvalidParameterException {
        super();
        addParameter(NAME, builder.name);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public String getCommand() {
        return DELETE_KEYPAIR_COMMAND;
    }

    public static class Builder {

        private String name;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public DeleteSSHKeypairRequest build() throws InvalidParameterException {
            return new DeleteSSHKeypairRequest(this);
        }
    }
}
