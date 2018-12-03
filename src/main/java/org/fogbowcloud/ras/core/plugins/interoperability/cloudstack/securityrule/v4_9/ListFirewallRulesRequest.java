package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9;

import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.SecurityGroupPlugin.IP_ADDRESS_ID_KEY_JSON;
import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.SecurityGroupPlugin.LIST_FIREWALL_RULES_COMMAND;

import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRequest;

/**
 * Documentation : https://cloudstack.apache.org/api/apidocs-4.9/apis/listFirewallRules.html
 * 
 * Request Example: {url_cloudstack}?command=listFirewallRules&ipaddressid={id}&apikey={apiKey}&secret_key={secretKey}
 *
 */	
public class ListFirewallRulesRequest extends CloudStackRequest {

	protected ListFirewallRulesRequest(Builder builder) throws InvalidParameterException {
		addParameter(IP_ADDRESS_ID_KEY_JSON, builder.ipAddressId);
	}

	@Override
	public String getCommand() {
		return LIST_FIREWALL_RULES_COMMAND;
	}
	

    public static class Builder {
        private String ipAddressId;

        public Builder ipAddressId(String ipAddressId) {
            this.ipAddressId = ipAddressId;
            return this;
        }

        public ListFirewallRulesRequest build() throws InvalidParameterException {
            return new ListFirewallRulesRequest(this);
        }
    }	
	
}
