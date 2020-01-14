package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackRequest;

import static cloud.fogbow.common.constants.CloudStackConstants.PublicIp.DISASSOCIATE_IP_ADDRESS_COMMAND;
import static cloud.fogbow.common.constants.CloudStackConstants.PublicIp.ID_KEY_JSON;

/**
 * Documentation : https://cloudstack.apache.org/api/apidocs-4.9/apis/disassociateIpAddress.html
 * 
 * Request Example: {url_cloudstack}?command=disassociateIpAddress&id={id}&apikey={apiKey}&secret_key={secretKey}
 */	
public class DisassociateIpAddressRequest extends CloudStackRequest {

	protected DisassociateIpAddressRequest(Builder builder) throws InvalidParameterException {
		super(builder.cloudStackUrl);
		addParameter(ID_KEY_JSON, builder.id);
	}

	@Override
	public String getCommand() {
		return DISASSOCIATE_IP_ADDRESS_COMMAND;
	}
	
    public static class Builder {
		private String cloudStackUrl;
	    private String id;
	
	    public Builder id(String id) {
	        this.id = id;
	        return this;
	    }
    	
        public DisassociateIpAddressRequest build(String cloudStackUrl) throws InvalidParameterException {
			this.cloudStackUrl = cloudStackUrl;
            return new DisassociateIpAddressRequest(this);
        }
    }

}
