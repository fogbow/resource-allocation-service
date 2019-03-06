package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.network;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.util.cloud.cloudstack.CloudStackRequest;

public class DeleteNetworkRequest extends CloudStackRequest {
    public static final String DELETE_NETWORK_COMMAND = "deleteNetwork";
    public static final String NETWORK_ID_KEY = "id";

    protected DeleteNetworkRequest(Builder builder) throws InvalidParameterException {
        super(builder.cloudStackUrl);
        addParameter(NETWORK_ID_KEY, builder.id);
    }

    @Override
    public String getCommand() {
        return DELETE_NETWORK_COMMAND;
    }

    public static class Builder {
        private String cloudStackUrl;
        private String id;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public DeleteNetworkRequest build(String cloudStackUrl) throws InvalidParameterException {
            this.cloudStackUrl = cloudStackUrl;
            return new DeleteNetworkRequest(this);
        }
    }
}
