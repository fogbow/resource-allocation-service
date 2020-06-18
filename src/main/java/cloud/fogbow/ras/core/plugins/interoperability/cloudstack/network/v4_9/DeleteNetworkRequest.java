package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.network.v4_9;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackRequest;

import static cloud.fogbow.common.constants.CloudStackConstants.Network.DELETE_NETWORK_COMMAND;
import static cloud.fogbow.common.constants.CloudStackConstants.Network.ID_KEY_JSON;

public class DeleteNetworkRequest extends CloudStackRequest {

    protected DeleteNetworkRequest(Builder builder) throws InternalServerErrorException {
        super(builder.cloudStackUrl);
        addParameter(ID_KEY_JSON, builder.id);
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

        public DeleteNetworkRequest build(String cloudStackUrl) throws InternalServerErrorException {
            this.cloudStackUrl = cloudStackUrl;
            return new DeleteNetworkRequest(this);
        }
    }
}
