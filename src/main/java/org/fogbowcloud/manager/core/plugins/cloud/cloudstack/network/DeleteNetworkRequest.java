package org.fogbowcloud.manager.core.plugins.cloud.cloudstack.network;


import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackRequest;

public class DeleteNetworkRequest extends CloudStackRequest {

    private static final String DELETE_NETWORK_COMMAND = "deleteNetwork";
    private static final String NETWORK_ID_KEY = "id";

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
