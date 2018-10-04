package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.network;


import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRequest;

public class DeleteNetworkRequest extends CloudStackRequest {
    public static final String DELETE_NETWORK_COMMAND = "deleteNetwork";
    public static final String NETWORK_ID_KEY = "id";

    protected DeleteNetworkRequest(Builder builder) throws InvalidParameterException {
        addParameter(NETWORK_ID_KEY, builder.id);
    }

    @Override
    public String getCommand() {
        return DELETE_NETWORK_COMMAND;
    }

    public static class Builder {
        private String id;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public DeleteNetworkRequest build() throws InvalidParameterException {
            return new DeleteNetworkRequest(this);
        }
    }
}
