package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.PublicIp.ID_KEY_JSON;

import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRequest;

/**
 * Documentation : https://cloudstack.apache.org/api/apidocs-4.9/apis/disassociateIpAddress.html
 * 
 * Request Example: 
 *
 */	
public class DisassociateIpAddressRequest extends CloudStackRequest {

	protected DisassociateIpAddressRequest(Builder builder) throws InvalidParameterException {
		addParameter(ID_KEY_JSON, builder.id);
	}

	@Override
	public String getCommand() {
		return null;
	}
	
    public static class Builder {
    	
	    private String id;
	
	    public Builder id(String id) {
	        this.id = id;
	        return this;
	    }
    	
        public DisassociateIpAddressRequest build() throws InvalidParameterException {
            return new DisassociateIpAddressRequest(this);
        }
    }

}
