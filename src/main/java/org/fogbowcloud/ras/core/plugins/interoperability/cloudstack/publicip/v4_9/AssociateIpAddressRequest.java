package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRequest;

public class AssociateIpAddressRequest extends CloudStackRequest {
	
	public static final String ASSOCIATE_IP_ADDRESS_COMMAND = "associateIpAddress";
	public static final String NETWORK_ID_KEY = "networkid";
	
	protected AssociateIpAddressRequest(Builder builder) throws InvalidParameterException {
		addParameter(NETWORK_ID_KEY, builder.networkId);
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
