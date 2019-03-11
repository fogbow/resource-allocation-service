package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.network.v4_9;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackRequest;

public class GetNetworkRequest extends CloudStackRequest {
    public static final String LIST_NETWORKS_COMMAND = "listNetworks";
    public static final String NETWORK_ID_KEY = "id";

    private GetNetworkRequest(Builder builder) throws InvalidParameterException {
        super(builder.cloudStackUrl);
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
        private String cloudStackUrl;
        private String id;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public GetNetworkRequest build(String cloudStackUrl) throws InvalidParameterException {
            this.cloudStackUrl = cloudStackUrl;
            return new GetNetworkRequest(this);
        }
    }
}
