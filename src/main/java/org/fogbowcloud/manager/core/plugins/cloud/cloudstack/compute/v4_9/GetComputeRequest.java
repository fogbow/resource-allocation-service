package org.fogbowcloud.manager.core.plugins.cloud.cloudstack.compute.v4_9;

import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackRequest;

public class GetComputeRequest extends CloudStackRequest {

    public static final String LIST_VMS_COMMAND = "listNetworks";
    public static final String VIRTUAL_MACHINE_ID_KEY = "id";

    private GetComputeRequest(Builder builder) throws InvalidParameterException {
        super(builder.endpoint, builder.command);
        addParameter(VIRTUAL_MACHINE_ID_KEY, builder.id);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public String getCommand() {
        return LIST_VMS_COMMAND;
    }

    public static class Builder {

        private String id;
        private String endpoint;
        private String command;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder command(String command) {
            this.command = command;
            return this;
        }

        public GetComputeRequest build() throws InvalidParameterException {
            return new GetComputeRequest(this);
        }

    }

}
