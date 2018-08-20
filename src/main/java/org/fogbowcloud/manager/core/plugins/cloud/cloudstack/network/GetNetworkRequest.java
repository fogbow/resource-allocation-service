package org.fogbowcloud.manager.core.plugins.cloud.cloudstack.network;

import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;

public class GetNetworkRequest extends CloudStackRequest {

    public static final String LIST_NETWORKS_COMMAND = "listNetworks";

    public static final String NETWORK_ID_KEY = "id";

    private GetNetworkRequest(Builder builder) throws InvalidParameterException {
        super();
        addParameter(NETWORK_ID_KEY, builder.id);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public String getCommand() {
        return LIST_NETWORKS_COMMAND;
    }

    public static class Builder {

        private String id;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public GetNetworkRequest build() throws InvalidParameterException {
            return new GetNetworkRequest(this);
        }

    }

}
