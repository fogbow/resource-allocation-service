package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackRequest;
import cloud.fogbow.common.constants.CloudStackConstants;

/**
 * Documentation : https://cloudstack.apache.org/api/apidocs-4.9/apis/listFirewallRules.html
 * 
 * Request Example: {url_cloudstack}?command=listFirewallRules&ipaddressid={id}&apikey={apiKey}&secret_key={secretKey}
 *
 */	
public class ListFirewallRulesRequest extends CloudStackRequest {

	protected ListFirewallRulesRequest(Builder builder) throws InvalidParameterException {
        super(builder.cloudStackUrl);
		addParameter(CloudStackConstants.SecurityGroupPlugin.IP_ADDRESS_ID_KEY_JSON, builder.ipAddressId);
	}

	@Override
	public String getCommand() {
		return CloudStackConstants.SecurityGroupPlugin.LIST_FIREWALL_RULES_COMMAND;
	}
	

    public static class Builder {
        private String cloudStackUrl;
        private String ipAddressId;

        public Builder ipAddressId(String ipAddressId) {
            this.ipAddressId = ipAddressId;
            return this;
        }

        public ListFirewallRulesRequest build(String cloudStackUrl) throws InvalidParameterException {
            this.cloudStackUrl = cloudStackUrl;
            return new ListFirewallRulesRequest(this);
        }
    }	
	
}
