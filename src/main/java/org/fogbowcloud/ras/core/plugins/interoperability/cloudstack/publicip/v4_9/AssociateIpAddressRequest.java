package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.PublicIp.ASSOCIATE_IP_ADDRESS_COMMAND;
import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.PublicIp.NETWORK_ID_KEY_JSON;

import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRequest;

/**
 * Documentation : https://cloudstack.apache.org/api/apidocs-4.9/apis/associateIpAddress.html
 *
 * Request Example:
 */
public class AssociateIpAddressRequest extends CloudStackRequest {

    protected AssociateIpAddressRequest(Builder builder) throws InvalidParameterException {
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

        private String networkId;

        public Builder networkId(String networkId) {
            this.networkId = networkId;
            return this;
        }

        public AssociateIpAddressRequest build() throws InvalidParameterException {
            return new AssociateIpAddressRequest(this);
        }
    }

}
