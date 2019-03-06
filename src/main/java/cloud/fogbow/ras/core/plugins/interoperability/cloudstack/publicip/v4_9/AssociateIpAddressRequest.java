package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.util.cloud.cloudstack.CloudStackRequest;

import static cloud.fogbow.common.constants.CloudStackConstants.PublicIp.ASSOCIATE_IP_ADDRESS_COMMAND;
import static cloud.fogbow.common.constants.CloudStackConstants.PublicIp.NETWORK_ID_KEY_JSON;

/**
 * Documentation : https://cloudstack.apache.org/api/apidocs-4.9/apis/associateIpAddress.html
 *
 * Request Example:
 */
public class AssociateIpAddressRequest extends CloudStackRequest {

    protected AssociateIpAddressRequest(Builder builder) throws InvalidParameterException {
        super(builder.cloudStackUrl);
        addParameter(NETWORK_ID_KEY_JSON, builder.networkId);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public String getCommand() {
        return ASSOCIATE_IP_ADDRESS_COMMAND;
    }

    public static class Builder {
        private String cloudStackUrl;
        private String networkId;

        public Builder networkId(String networkId) {
            this.networkId = networkId;
            return this;
        }

        public AssociateIpAddressRequest build(String cloudStackUrl) throws InvalidParameterException {
            this.cloudStackUrl = cloudStackUrl;
            return new AssociateIpAddressRequest(this);
        }
    }

}
