package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRequest;

import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.PublicIp.*;

/**
 * Documentation : https://cloudstack.apache.org/api/apidocs-4.9/apis/createFirewallRule.html
 * 
 * Request Example: 
 *
 */	
public class CreateFirewallRuleRequest extends CloudStackRequest {

    public static final String CREATE_FIREWALL_RULE_COMMAND = "createFirewallRule";

    protected CreateFirewallRuleRequest(Builder builder) throws InvalidParameterException {
		addParameter(PROTOCOL_KEY_JSON, builder.protocol);
		addParameter(STARTPORT_KEY_JSON, builder.startPort);
		addParameter(ENDPORT_KEY_JSON, builder.endPort);
		addParameter(IP_ADDRESS_ID_KEY_JSON, builder.ipAddressId);
	}

	@Override
    public String toString() {
        return super.toString();
    }

	@Override
	public String getCommand() {
		return CREATE_FIREWALL_RULE_COMMAND;
	}

    public static class Builder {
    	private String protocol;
    	private String startPort;
    	private String endPort;
    	private String ipAddressId;
    	
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
        
        public Builder ipAddressId(String ipAddressId) {
            this.ipAddressId = ipAddressId;
            return this;
        }
    	
        public CreateFirewallRuleRequest build() throws InvalidParameterException {
            return new CreateFirewallRuleRequest(this);
        }
    }
	
}
