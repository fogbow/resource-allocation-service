package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.quota.v4_9;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackRequest;
import static cloud.fogbow.common.constants.CloudStackConstants.Network.ID_KEY_JSON;

public class ListPublicIpAddressRequest extends CloudStackRequest {
    private static final String LIST_PUBLIC_IP_ADDRESSES_COMMAND = "listPublicIpAddresses";

    private ListPublicIpAddressRequest(Builder builder) throws InternalServerErrorException {
        super(builder.cloudStackUrl);
        addParameter(ID_KEY_JSON, builder.id);
    }

    @Override
    public String getCommand() {
        return LIST_PUBLIC_IP_ADDRESSES_COMMAND;
    }

    public static class Builder {
        private String cloudStackUrl;
        private String id;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public ListPublicIpAddressRequest build(String cloudStackUrl) throws InternalServerErrorException {
            this.cloudStackUrl = cloudStackUrl;
            return new ListPublicIpAddressRequest(this);
        }
    }
}
