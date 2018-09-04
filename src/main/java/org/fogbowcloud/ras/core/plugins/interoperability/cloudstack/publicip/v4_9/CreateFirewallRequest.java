package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.PublicIp.ENDPORT_KEY_JSON;
import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.PublicIp.IP_ADDRESS_KEY_JSON;
import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.PublicIp.PROTOCOL_KEY_JSON;
import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.PublicIp.STARTPORT_KEY_JSON;

import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRequest;

/**
 * Documentation : https://cloudstack.apache.org/api/apidocs-4.9/apis/createFirewallRule.html
 * 
 * Request Example: 
 *
 */	
public class CreateFirewallRequest extends CloudStackRequest {

	protected CreateFirewallRequest(Builder builder) throws InvalidParameterException {
		addParameter(PROTOCOL_KEY_JSON, builder.protocol);
		addParameter(STARTPORT_KEY_JSON, builder.startPort);
		addParameter(ENDPORT_KEY_JSON, builder.endPort);
		addParameter(IP_ADDRESS_KEY_JSON, builder.ipAddress);
	}

	@Override
    public String toString() {
        return super.toString();
    }
	
	@Override
	public String getCommand() {
		return null;
	}

    public static class Builder {
    	private String protocol;
    	private String startPort;
    	private String endPort;
    	private String ipAddress;
    	
        public Builder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }
        
        public Builder startPort(String startPort) {
            this.startPort = startPort;
            return this;
        }
        
        public Builder endPort(String endPort) {
            this.endPort = endPort;
            return this;
        }        
        
        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }
    	
        public CreateFirewallRequest build() throws InvalidParameterException {
            return new CreateFirewallRequest(this);
        }
    }
	
}
